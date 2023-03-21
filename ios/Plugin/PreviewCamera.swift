import Foundation
import UIKit
import Capacitor
import AVFoundation
import MobileCoreServices

typealias CapacitorNotifyListeners = (_ eventName: String, _ data: [String : Any?]?) -> Void

enum CaptureQuality {
    case low, hq
}

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
    
    var torchMode = AVCaptureDevice.TorchMode.off
    var isTorchModeAvailable = false
    var customOrientation = "portraitUp"
    
    var captureQuality = CaptureQuality.hq

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

        // Update the orientation before taking photo.
        if let photoOutputConnection = photoOutput.connection(with: .video){
            photoOutputConnection.videoOrientation = videoOrientation()
        }
        
        do {
            try self.videoDeviceInput.device.lockForConfiguration()
            if self.videoDeviceInput.device.isTorchModeSupported(.on) && self.torchMode == .on {
                self.videoDeviceInput.device.torchMode = .on
            }
            self.videoDeviceInput.device.unlockForConfiguration()
        } catch {
            print("Failed to set torch for photo")
        }
        
        let photoSettings = AVCapturePhotoSettings()
        
        if captureQuality == .low {
            photoSettings.isHighResolutionPhotoEnabled = false
            if #available(iOS 13.0, *) {
                photoSettings.photoQualityPrioritization = .speed
            }
        } else {
            photoSettings.isHighResolutionPhotoEnabled = true
            if #available(iOS 13.0, *) {
                photoSettings.photoQualityPrioritization = .quality
            }
        }
        
        photoOutput.capturePhoto(with: photoSettings, delegate: self)
    }

    // AVCapturePhotoCaptureDelegate method for AVCapturePhotoOutput.capturePhoto
    public func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let data = photo.fileDataRepresentation() else {
            self.capturePhotoFinished(errorMessage: "The photo and attachment data cannot be flattened.")
            return
        }
        let sizeInMB = ByteCountFormatter.string(fromByteCount: Int64(data.count), countStyle: .file)
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
        
        // self.session.sessionPreset = .hd4K3840x2160
        self.session.sessionPreset = .high
        if self.captureQuality == .low {
            // self.session.sessionPreset = .hd1920x1080
            self.session.sessionPreset = .medium
        }
        
        // Update the orientation on the movie file output video connection before recording.
        let movieFileOutputConnection = movieFileOutput.connection(with: .video)
        movieFileOutputConnection?.videoOrientation = self.videoOrientation()
        
        session.startRunning()
        
        let filePath = createFile(FILENAME_FORMAT, VIDEO_EXTENSION)
        
        do {
            try self.videoDeviceInput.device.lockForConfiguration()
            if self.videoDeviceInput.device.isTorchModeSupported(.on) && self.torchMode == .on {
                self.videoDeviceInput.device.torchMode = .on
            }
            self.videoDeviceInput.device.unlockForConfiguration()
        } catch {
            print("Failed to set torch for video")
        }
        
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
        
        let videoSizeInMB = ByteCountFormatter.string(fromByteCount: output.recordedFileSize, countStyle: .file)
        
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
                
                self.session.sessionPreset = .high
                if self.captureQuality == .low {
                    self.session.sessionPreset = .medium
                }
                
                self.session.removeInput(self.videoDeviceInput)
                
                if self.session.canAddInput(videoDeviceInput) {
                    
                    self.session.addInput(videoDeviceInput)
                    self.videoDeviceInput = videoDeviceInput
                    if videoDeviceInput.device.isTorchModeSupported(.on) {
                        self.isTorchModeAvailable = true
                    } else {
                        self.isTorchModeAvailable = false
                    }
                } else {
                    self.session.addInput(self.videoDeviceInput)
                }
                
                self.session.removeOutput(self.movieFileOutput)
                if self.session.canAddOutput(movieFileOutput) {
                    self.session.addOutput(movieFileOutput)
                    
//                    do {
//                        try self.videoDeviceInput.device.lockForConfiguration()
//                        for format in self.videoDeviceInput.device.formats {
//                            if #available(iOS 15.0, *) {
//                                if format.isHighPhotoQualitySupported {
//                                    self.videoDeviceInput.device.activeFormat = format
//                                    break
//                                }
//                            }
//                        }
//                        self.videoDeviceInput.device.unlockForConfiguration()
//                    } catch {
//                        print("Could not lock device for configuration: \(error)")
//                    }
                }
                
                previewLayer.videoGravity = .resizeAspectFill
                DispatchQueue.main.async { [weak self] in
                    guard let strongSelf = self else { return }

                    strongSelf.previewLayer.frame = strongSelf.previewLayerWrapper.safeAreaLayoutGuide.layoutFrame
                }
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
    
    public func isTorchOn()  -> Bool {
        return self.torchMode == .on
    }
    
    public func enableTorch(enable: Bool) {
        if self.videoDeviceInput.device.isFlashAvailable == false {
            return
        }
        if enable == true {
            self.torchMode = .on
        } else {
            self.torchMode = .off
        }
    }
    
    public func isTorchAvailable() -> Bool {
        guard let videoDeviceInput = self.videoDeviceInput else {
            return false
        }
        return videoDeviceInput.device.isTorchModeSupported(.on)
    }
    
    public func focus(_ x: Float, _ y: Float) {
        guard let device = self.videoDeviceInput?.device else {
            // throw CameraError.session(SessionError.cameraNotReady)
            return
        }
        if !device.isFocusPointOfInterestSupported {
            //  throw CameraError.device(DeviceError.focusNotSupported)
            return
        }
        let point = CGPoint(x: CGFloat(x), y: CGFloat(y))
        
        let normalizedPoint = self.previewLayer.captureDevicePointConverted(fromLayerPoint: point)
        
        do {
            try device.lockForConfiguration()
            
            device.focusPointOfInterest = normalizedPoint
            device.focusMode = .continuousAutoFocus
            
            if device.isExposurePointOfInterestSupported {
                device.exposurePointOfInterest = normalizedPoint
                device.exposureMode = .continuousAutoExposure
            }
            
            device.unlockForConfiguration()
            
        } catch {
            // throw CameraError.device(DeviceError.configureError)
            print("Failed to focus")
        }
        
    }
    
    public func minAvailableZoom() -> Float {
        let minZoom = self.videoDeviceInput?.device.minAvailableVideoZoomFactor ?? 0.0
        return Float(minZoom)
    }
    
    public func maxAvailableZoom() -> Float {
        let maxZoom = self.videoDeviceInput?.device.maxAvailableVideoZoomFactor ?? 0.0
        return Float(maxZoom)
    }
    
    public func zoom(_ zoomFactor: Float) {
        guard let device = self.videoDeviceInput?.device else {
            return
        }
        
        let safeZoomFactor = max(min(CGFloat(zoomFactor), self.videoDeviceInput.device.maxAvailableVideoZoomFactor),CGFloat(1))
        
        do {
            try device.lockForConfiguration()
            device.videoZoomFactor = safeZoomFactor
            device.unlockForConfiguration()
        } catch {
            // TODO: handle error case
            print("Failed to zoom \(zoomFactor)")
        }
    }
    
    public func setQuality(_ quality: String) {
        if quality == "low" {
            captureQuality = .low
            session.sessionPreset = .medium
        } else {
            captureQuality = .hq
            session.sessionPreset = .high
        }
    }
    
    public func saveFileToUserDevice(_ filePath: String) {
        guard let fileExtension = URL(fileURLWithPath: filePath).pathExtension as CFString?,
              let fileType = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, fileExtension, nil)?.takeRetainedValue() else {
            print("Unable to determine file type.")
            return
        }
        // Remove `file://` prefix if any
        let filePathClean = filePath.replacingOccurrences(of: "file://", with: "")
        if UTTypeConformsTo(fileType, kUTTypeImage) {
            saveImageToPhotoAlbum(filePathClean)
        } else if UTTypeConformsTo(fileType, kUTTypeMovie) {
            saveVideoToPhotoAlbum(videoPath: filePathClean)
        } else {
            print("File type is neither image nor video.")
        }
    }
    
    
    public func saveImageToPhotoAlbum(_ filePath: String) {
        // Load image from the file path
        if let image = UIImage(contentsOfFile: filePath) {
            // Save image to the Photo Album
            UIImageWriteToSavedPhotosAlbum(image, self, #selector(imageSavedToPhotosAlbum(_:didFinishSavingWithError:contextInfo:)), nil)
        } else {
            print("Error: Failed to load image from file path")
        }
//
//        do {
//            let imageUrl = URL(fileURLWithPath: filePath)
//            let imageData = try Data(contentsOf: imageUrl)
//            guard let image = UIImage(data: imageData) else {
//                print("Unable to create UIImage from data.")
//                return
//            }
//            UIImageWriteToSavedPhotosAlbum(image, self, #selector(imageSavedToPhotosAlbum(_:didFinishSavingWithError:contextInfo:)), nil)
//        } catch {
//            print("Error reading image data: \(error.localizedDescription)")
//        }
    }
    
    @objc func imageSavedToPhotosAlbum(_ image: UIImage, didFinishSavingWithError error: Error?, contextInfo: UnsafeRawPointer) {
        // Check for errors
        if let error = error {
            print("Error: Failed to save image - \(error.localizedDescription)")
        } else {
            print("Success: Image saved to Photo Album")
        }
    }
    
    func saveVideoToPhotoAlbum(videoPath: String) {
        if UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(videoPath) {
            UISaveVideoAtPathToSavedPhotosAlbum(videoPath, self, #selector(videoSavedToPhotosAlbum(_:didFinishSavingWithError:contextInfo:)), nil)
        } else {
            print("Video is not compatible with the Photos Album.")
        }
    }
    
    @objc func videoSavedToPhotosAlbum(_ videoPath: NSString, didFinishSavingWithError error: NSError?, contextInfo: UnsafeRawPointer) {
        if let error = error {
            print("Error saving video: \(error.localizedDescription)")
        } else {
            print("Video saved to Photos Album.")
        }
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
        previewLayer.frame = previewLayerWrapper.safeAreaLayoutGuide.layoutFrame
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
                
                photoOutput.isHighResolutionCaptureEnabled = true
                if #available(iOS 13.0, *) {
                    photoOutput.maxPhotoQualityPrioritization = .quality
                }
            }
            
            if session.canAddOutput(movieFileOutput) {
                session.addOutput(movieFileOutput)
            }
            
            previewLayer.videoGravity = .resizeAspectFill
            
            DispatchQueue.main.async { [weak self] in
                guard let strongSelf = self else { return }

                strongSelf.previewLayer.frame = strongSelf.previewLayerWrapper.safeAreaLayoutGuide.layoutFrame
            }
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
    
    
    // MARK: - Utility functions
    
    private func videoOrientation() -> AVCaptureVideoOrientation {
        
        var videoOrientation: AVCaptureVideoOrientation = .portrait
        if self.customOrientation == "landscapeRight" {
            videoOrientation = .landscapeLeft
        }
        if self.customOrientation == "landscapeLeft" {
            videoOrientation = .landscapeRight
        }
        return videoOrientation;
        
//        let orientation: UIDeviceOrientation = UIDevice.current.orientation
//
//        switch orientation {
//
//        case .faceUp, .faceDown, .unknown:
//
//            // let interfaceOrientation = UIApplication.shared.statusBarOrientation
//
//            if #available(iOS 13.0, *) {
//                if let interfaceOrientation = UIApplication.shared.windows.first(where: { $0.isKeyWindow })?.windowScene?.interfaceOrientation {
//
//                    switch interfaceOrientation {
//
//                    case .portrait, .portraitUpsideDown, .unknown:
//                        videoOrientation = .portrait
//                    case .landscapeLeft:
//                        videoOrientation = .landscapeRight
//                    case .landscapeRight:
//                        videoOrientation = .landscapeLeft
//                    @unknown default:
//                        videoOrientation = .portrait
//                    }
//                }
//            } else {
//                // Fallback on earlier versions
//            }
//
//        case .portrait, .portraitUpsideDown:
//            videoOrientation = .portrait
//        case .landscapeLeft:
//            videoOrientation = .landscapeRight
//        case .landscapeRight:
//            videoOrientation = .landscapeLeft
//        @unknown default:
//            videoOrientation = .portrait
//        }
//
//
//        if #available(iOS 13.0, *) {
//            if let interfaceOrientation = UIApplication.shared.windows.first(where: { $0.isKeyWindow })?.windowScene?.interfaceOrientation {
//
//                switch interfaceOrientation {
//
//                case .portrait, .portraitUpsideDown, .unknown:
//                    videoOrientation = .portrait
//                case .landscapeLeft:
//                    videoOrientation = .landscapeRight
//                case .landscapeRight:
//                    videoOrientation = .landscapeLeft
//                @unknown default:
//                    videoOrientation = .portrait
//                }
//            }
//        } else {
//            // Fallback on earlier versions
//        }
//
//        var customOrientation: UIDeviceOrientation = .portrait
//        if #available(iOS 13.0, *) {
//            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
//                let interfaceOrientation = windowScene.interfaceOrientation
//                switch interfaceOrientation {
//                case .landscapeRight:
//                    customOrientation = .landscapeRight
//                case .landscapeLeft:
//                    customOrientation = .landscapeLeft
//                default:
//                    customOrientation = .portrait
//                }
//            }
//        } else {
//            switch UIApplication.shared.statusBarOrientation {
//            case .landscapeRight:
//                customOrientation = .landscapeRight
//            case .landscapeLeft:
//                customOrientation = .landscapeLeft
//            default:
//                customOrientation = .portrait
//            }
//        }
//
//        switch customOrientation {
//        case .landscapeLeft, .landscapeRight:
//            videoOrientation = .landscapeRight
//        default:
//            videoOrientation = .portrait
//        }
//
//        return videoOrientation
    }
    
    private func isFrontCamera() -> Bool {
        let currentVideoDevice = self.videoDeviceInput.device
        let currentPosition = currentVideoDevice.position
        
        if currentPosition == .unspecified || currentPosition == .back {
            return false
        }
        
        return true
    }
}
