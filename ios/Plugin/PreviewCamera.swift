import Foundation
import UIKit
import Capacitor
import AVFoundation

typealias CapacitorNotifyListeners = (_ eventName: String, _ data: [String : Any?]?) -> Void

@objc public class PreviewCamera: NSObject, AVCaptureFileOutputRecordingDelegate, AVCapturePhotoCaptureDelegate {
    
    let notifyListeners: CapacitorNotifyListeners
    
    var webView: UIView
    var parentView: UIView
    var settings: CameraSettings
    // Camera Preview state
    private(set) var cameraPreviewStarted = false
    private(set) var cameraRecordStarted = false
    // Camera Preview Wrapper
    var previewLayerWrapper = UIView()
    // Camera Preview
    let previewLayer = AVCaptureVideoPreviewLayer()
    // Capture Session
    let session = AVCaptureSession()
    // Photo Output
    let photoOutput = AVCapturePhotoOutput()
    // Video Output
    let movieFileOutput = AVCaptureMovieFileOutput()
    var videoDeviceInput: AVCaptureDeviceInput!
    
    let FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    let PHOTO_EXTENSION = "jpeg"
    let VIDEO_EXTENSION = "mp4"
    var outputDirectory = FileManager.default.urls(for: .applicationDirectory, in: .userDomainMask)[0]

    init(webView: UIView, parentView: UIView, settings: CameraSettings, notifyListeners:  @escaping CapacitorNotifyListeners) {
        self.webView = webView
        self.parentView = parentView
        self.settings = settings
        self.notifyListeners = notifyListeners
    }

    // MARK: - PreviewCamera plugin methods
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }

    public func startCameraPreview(settings: CameraSettings?) throws {
        if cameraPreviewStarted == true {
            throw PreviewCameraError.previewAlreadyStarted
        }
        self.settings = settings ?? self.settings
        try self.setupCamera()
        DispatchQueue.main.async { [weak self] in
            guard let strongSelf = self else { return }
            
            strongSelf.showCameraPreview()
            strongSelf.cameraPreviewStarted = true
        }

    }

    public func stopPreviewCamera() throws {
        if cameraPreviewStarted == false {
            throw PreviewCameraError.previewNotStarted
        }
        try self.tearDownCamera()
        DispatchQueue.main.async { [weak self] in
            guard let strongSelf = self else { return }

            strongSelf.hideCameraPreview()
            strongSelf.cameraPreviewStarted = false
        }
    }

    public func takePhoto() throws {
        if cameraPreviewStarted == false {
            throw PreviewCameraError.previewNotStarted
        }
        
        photoOutput.capturePhoto(with: AVCapturePhotoSettings(), delegate: self)
    }

    // AVCapturePhotoCaptureDelegate method for AVCapturePhotoOutput.capturePhoto
    public func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let data = photo.fileDataRepresentation() else {
            self.capturePhotoFinished(errorMessage: "The photo and attachment data cannot be flattened.")
            return
        }
        
        let filePath = createFile(FILENAME_FORMAT, PHOTO_EXTENSION)
        
        do {
            try data.write(to: filePath)
            self.capturePhotoFinished(filePath)
        } catch {
            self.capturePhotoFinished(errorMessage: error.localizedDescription)
        }
    }
    
    public func startRecord() throws {
        if cameraPreviewStarted == false {
            throw PreviewCameraError.previewNotStarted
        }
        if cameraRecordStarted == true {
            throw PreviewCameraError.recordAlreadyStarted
        }
        
        session.startRunning()
        
        let filePath = createFile(FILENAME_FORMAT, VIDEO_EXTENSION)
        
        movieFileOutput.startRecording(to: filePath, recordingDelegate: self)
        
        cameraRecordStarted = true
    }
    
    // AVCaptureFileOutputRecordingDelegate method for AVCaptureMovieFileOutput.startRecording
    public func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
        
        var success = true
        
        if error != nil {
            success = false
//            success = (((error! as NSError).userInfo[AVErrorRecordingSuccessfullyFinishedKey] as AnyObject).boolValue)!
            let errorMessage = "Movie file finishing error: \(String(describing: error))"
            self.captureVideoFinished(errorMessage: errorMessage)
        }
        
        if success {
            self.captureVideoFinished(outputFileURL)
        }
    }

    public func stopRecord() throws {
        if cameraPreviewStarted == false {
            throw PreviewCameraError.recordNotStarted
        }
        if cameraRecordStarted == false {
            throw PreviewCameraError.recordAlreadyStopped
        }
        movieFileOutput.stopRecording()
        cameraRecordStarted = false
    }

    public func flipCamera() throws {
        if cameraPreviewStarted == false {
            throw PreviewCameraError.previewNotStarted
        }
        if cameraRecordStarted == true {
            throw PreviewCameraError.recordAlreadyStarted
        }
        // TODO: implement method
        
        let currentVideoDevice = self.videoDeviceInput.device
        let currentPosition = currentVideoDevice.position
        
        let backVideoDeviceDiscoverySession = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInDualCamera, .builtInWideAngleCamera],
                                                                               mediaType: .video, position: .back)
        let frontVideoDeviceDiscoverySession = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInTrueDepthCamera, .builtInWideAngleCamera],
                                                                                mediaType: .video, position: .front)
        var newVideoDevice: AVCaptureDevice? = nil
        
        switch currentPosition {
        case .unspecified, .front:
            newVideoDevice = backVideoDeviceDiscoverySession.devices.first
        
        case .back:
            newVideoDevice = frontVideoDeviceDiscoverySession.devices.first
            
        @unknown default:
            print("Unknown capture position. Defaulting to back, dual-camera.")
            newVideoDevice = AVCaptureDevice.default(.builtInDualCamera, for: .video, position: .back)
        }
        
        if let videoDevice = newVideoDevice {
            do {
                let videoDeviceInput  = try AVCaptureDeviceInput(device: videoDevice)
                
                self.session.beginConfiguration()
                
                self.session.removeInput(self.videoDeviceInput)
                
                if self.session.canAddInput(videoDeviceInput) {
                    
                    self.session.addInput(videoDeviceInput)
                    self.videoDeviceInput = videoDeviceInput
                } else {
                    self.session.addInput(self.videoDeviceInput)
                }
                
                self.session.removeOutput(self.movieFileOutput)
                if self.session.canAddOutput(movieFileOutput) {
                    self.session.addOutput(movieFileOutput)
                }
                
                previewLayer.videoGravity = .resizeAspectFill
                previewLayer.session = session
            }
        }
        session.commitConfiguration()

    }

    public func getFlashModes() throws {
        // TODO: implement method, no need for pre check
    }

    public func setFlashModes() throws {
        if cameraPreviewStarted == false {
            throw PreviewCameraError.previewNotStarted
        }
        // TODO: implement method
    }

    // MARK: - PreviewCamera helper methods
    private func showCameraPreview() {
        let screenWidth = UIScreen.main.bounds.size.width
        let screenHeight = UIScreen.main.bounds.size.height
        previewLayerWrapper.frame = CGRect(x: 0, y: 0, width: screenWidth, height: screenHeight)
        
        // configure webview before preview camera start
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        webView.superview?.addSubview(previewLayerWrapper)
        webView.superview?.bringSubviewToFront(webView)
        
        // display previewLayer: AVCaptureVideoPreviewLayer on previewLayerWrapper: UIView
        previewLayerWrapper.layer.insertSublayer(previewLayer, at: 0)
        previewLayer.frame = previewLayerWrapper.frame
        previewLayer.session?.startRunning()
    }
    
    private func hideCameraPreview() {
        // TODO: remove used resources aka previewLayer.session?.removeInput(someInput)
        // TODO: remove used resources aka previewLayer.session?.removeOutput(someOutput)
        previewLayer.session?.stopRunning()
        previewLayer.removeFromSuperlayer()
        previewLayerWrapper.removeFromSuperview()
        // Configure webview before camera preview stop
        webView.isOpaque = true
    }
    
    private func setupCamera() throws {
        
//        session.beginConfiguration()
        
        // Add video input.
//
//        do {
//            var defaultVideoDevice: AVCaptureDevice?
//
//            // Choose camera
//
//            if let dualCameraDevice = AVCaptureDevice.default(.builtInDualCamera, for: .video, position: .back) {
//                defaultVideoDevice = dualCameraDevice
//            } else if let backCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) {
//                // If a rear dual wide camera is not available, default to the rear wide angle camera.
//                defaultVideoDevice = backCameraDevice
//            } else if let frontCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front) {
//                // If the rear wide angle camera isn't available, default to the front wide angle camera.
//                defaultVideoDevice = frontCameraDevice
//            }
//            guard let videoDevice = defaultVideoDevice else {
//                print("Default video device is unavailable.")
//                session.commitConfiguration()
//                throw PreviewCameraError.setupCameraFailure
//            }
//            let videoDeviceInput = try AVCaptureDeviceInput(device: videoDevice)
//
//            if session.canAddInput(videoDeviceInput) {
//                session.addInput(videoDeviceInput)
//                self.videoDeviceInput = videoDeviceInput
//            } else {
//                print("Couldn't add video device input to the session.")
//                session.commitConfiguration()
//                throw PreviewCameraError.setupCameraFailure
//            }
//        } catch {
//            print("Couldn't create video device input: \(error)")
//            session.commitConfiguration()
//            throw PreviewCameraError.setupCameraFailure
//        }
//
//        // Add an audio input device.
//        do {
//            let audioDevice = AVCaptureDevice.default(for: .audio)
//            let audioDeviceInput = try AVCaptureDeviceInput(device: audioDevice!)
//
//            if session.canAddInput(audioDeviceInput) {
//                session.addInput(audioDeviceInput)
//            } else {
//                print("Could not add audio device input to the session")
//            }
//        } catch {
//            print("Could not create audio device input: \(error)")
//            // throw PreviewCameraError.setupCameraFailure
//        }
//
//        // Add the photo output
//        if session.canAddOutput(photoOutput) {
//
//        } else {
//            print("Could not add photo output to the session")
//            session.commitConfiguration()
//            throw PreviewCameraError.setupCameraFailure
//        }
//
//        session.commitConfiguration()
//
//        previewLayer.videoGravity = .resizeAspectFill
//        previewLayer.session = session
        
        session.beginConfiguration()
        
        guard let device = AVCaptureDevice.default(for: .video) else {
            throw PreviewCameraError.defaultDeviceUsedToCaptureDataBusy
        }
        do {
            let videoDeviceInput = try AVCaptureDeviceInput(device: device)
            if session.canAddInput(videoDeviceInput) {
                session.addInput(videoDeviceInput)
                self.videoDeviceInput = videoDeviceInput
            }
            
            let audioDevice = AVCaptureDevice.default(for: .audio)
            let audioDeviceInput = try AVCaptureDeviceInput(device: audioDevice!)
            if session.canAddInput(audioDeviceInput) {
                session.addInput(audioDeviceInput)
            }
           
            
            if session.canAddOutput(photoOutput) {
                session.addOutput(photoOutput)
            }
            
            if session.canAddOutput(movieFileOutput) {
                session.addOutput(movieFileOutput)
            }
            
            previewLayer.videoGravity = .resizeAspectFill
            previewLayer.session = session
        } catch {
            print(error)
        }
        session.commitConfiguration()        
        
    }
    
    private func tearDownCamera() throws {
        guard let device = AVCaptureDevice.default(for: .video) else {
            throw PreviewCameraError.defaultDeviceUsedToCaptureDataBusy
        }
        do {
            let input = try AVCaptureDeviceInput(device: device)
            session.removeInput(input)
            session.removeOutput(photoOutput)
            session.removeOutput(movieFileOutput)
            
        } catch {
            print(error)
        }
    }
    
    // MARK: - Helper functions
    
    private func createFile(_ format: String, _ ext: String) -> URL {
        let outputFileName = NSUUID().uuidString + "." + ext
        
        let documentsUrl = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let outputFilePath = documentsUrl.appendingPathComponent(outputFileName)
        return outputFilePath
        
        //  let outputFilePath = (NSTemporaryDirectory() as NSString).appendingPathComponent((outputFileName as NSString).appendingPathExtension(ext)!)
        // return URL(fileURLWithPath: outputFilePath)
    }
    
    private func createFileVerA(_ format: String, _ ext: String) -> URL {
        let date = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = format
        
        let fileName = formatter.string(from: date) + "." + ext
        
        let paths = FileManager.default.urls(for: .applicationDirectory, in: .userDomainMask)
        let filePath = paths[0].appendingPathComponent(fileName)
        
        return filePath
    }
    
    private func createFileVerB(_ baseFolder: URL, _ format: String, _ ext: String) -> URL {
        
//        struct SimpleDateFormat {
//            private let pattern: String
//            private let formatter = DateFormatter()
//
//            init(_ pattern: String) {
//                self.pattern = pattern
//            }
//
//            func format(date: Date) -> String {
//                formatter.dateFormat = pattern
//                return formatter.string(from: date)
//            }
//        }
//        let fileName = SimpleDateFormat(format).format(date: Date()) + "." + ext
        
        let date = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = format
        
        let fileName = formatter.string(from: date) + "." + ext
        
        let filePath = baseFolder.appendingPathComponent(fileName)
    
        return filePath
    }
    
    private func capturePhotoFinished(_ filePath: URL? = nil, errorMessage: String? = nil) {
        notifyListeners("capturePhotoFinished", ["filePath": filePath?.absoluteString, "errorMessage": errorMessage])
        
    }
    
    private func captureVideoFinished(_ filePath: URL? = nil, errorMessage: String? = nil) {
        
        notifyListeners("captureVideoFinished", ["filePath": filePath?.absoluteString, "errorMessage": errorMessage])
    }
}
