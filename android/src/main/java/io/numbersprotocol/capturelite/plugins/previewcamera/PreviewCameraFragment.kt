package io.numbersprotocol.capturelite.plugins.previewcamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager

import android.hardware.camera2.*
import android.hardware.display.DisplayManager
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.window.WindowManager
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import io.numbersprotocol.capturelite.plugins.previewcamera.databinding.FragmentPreviewCameraBinding
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"

/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

/**
 * A simple [Fragment] subclass.
 * Use the [PreviewCameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreviewCameraFragment : Fragment() {
    /** Android ViewBinding */
    private var _fragmentCameraBinding: FragmentPreviewCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var windowManager: WindowManager

    private var currentDisplayOrientation = 0;

    private var cameraSetupCompleted = false
    private var captureQuality = "hq" // can be "low" or "hq" TODO: change to enums

    var flashMode = ImageCapture.FLASH_MODE_OFF
    var flashModeAvailable = true;

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@PreviewCameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                // imageAnalyzer?.targetRotation = view.display.rotation

                if (view.display.rotation != currentDisplayOrientation) {
                    currentDisplayOrientation = view.display.rotation
                    bindCameraUseCases();
                }
            }
        } ?: Unit
    }

    // TODO: remove cameraManager
    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // TODO: remove characteristics
    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        // cameraManager.getCameraCharacteristics(args.cameraId)
        cameraManager.getCameraCharacteristics(cameraId!!)
    }


    // TODO: Remove or Rename and change types of parameters
    private var cameraId: String? = null
    private var pixelFormat: Int? = null

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        // if (!PermissionsFragment.hasPermissions(requireContext())) {
        //    Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
        //        CameraFragmentDirections.actionCameraToPermissions()
        //    )
        // }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        arguments?.let {
            cameraId = it.getString(ARG_PARAM1)
            pixelFormat = it.getInt(ARG_PARAM2)

            val flashEnabled = it.getBoolean(ARG_PARAM3)
            flashMode = if (flashEnabled) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentPreviewCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Set up the intent filter that will receive events from our main activity
        // val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        // broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        //Initialize WindowManager to retrieve display metrics
        windowManager = WindowManager(view.context)

        // Determine the output directory

        outputDirectory = getOutputDirectory(requireContext())

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = fragmentCameraBinding.viewFinder.display.displayId

            // Build UI controls
            // updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
        }
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            flashModeAvailable = CameraSelector.LENS_FACING_BACK == lensFacing

            flashMode = if (flashModeAvailable && flashMode == ImageCapture.FLASH_MODE_ON) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }

            // Enable or disable switching between cameras
            // updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
            this.cameraSetupCompleted = true
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    fun takePhoto(call: PluginCall, notifyListener: CapacitorNotifyListener) {
        imageCapture?.let { imageCapture ->
            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PreviewCameraPlugin-Image")
                }
            }

            // Create output options object which contains file + metadata
//            val mediaStoreOutputOptions = ImageCapture.OutputFileOptions.Builder(
//                requireActivity().contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues
//            )
//                .setMetadata(metadata)
//                .build()

            // Create output file to hold the image
            val photoFile = createFile(outputDirectory, name, PHOTO_EXTENSION)
            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)

                        val data = JSObject().apply {
                            put("filePath", null)
                            put("errorMessage", "Photo capture failed: ${exc.message}")
                        }

                        notifyListener("capturePhotoFinished", data)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        Log.d(TAG, "Photo capture succeeded: $savedUri")
                        output.savedUri

                        val file = File(savedUri?.path)
                        val fileSize = file.length() / 1024

                        val data = JSObject().apply {
                            put("filePath", savedUri)
                            put("errorMessage", null)
                        }

                        notifyListener("capturePhotoFinished", data)
                    }
                })
        }
    }

    fun videoCaptureStart(call: PluginCall, notifyListeners: (String, JSObject) -> Unit) {
        // TODO: handle edge cases if video was started before etc
        // TODO: handle edge cases if preview was not started before etc
        if (this.videoCapture == null) {
            call.reject("Use case for Video Capture not configured see bindCameraUseCases method")
            return;
        }

        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PreviewCameraPlugin-Video")
            }
        }
//        val mediaStoreOutput = MediaStoreOutputOptions
//            .Builder(
//                requireActivity().contentResolver,
//                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//            )
//            .setContentValues(contentValues)
//            .build()


        val videoFile = createFile(outputDirectory, name, VIDEO_EXTENSION)
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        try {
            recording = videoCapture!!.output
                .prepareRecording(requireActivity(), fileOutputOptions)
                .apply {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    } else {
                        // We handle permission at PreviewCameraPlugin.kt therefore by this time
                        // we should have permissions Granted already

                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        // return
                    }

                }
                .start(cameraExecutor) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            // showUI(UiState.RECORDING, recordEvent.getNameString())
                            val data = JSObject().apply {
                                put("VideoRecordEvent", "Start")
                            }
                            // call.resolve(data) // TODO: notify instead off resolve
                        }
                        is VideoRecordEvent.Finalize -> {
                            // showUI(UiState.FINALIZED, recordEvent.getNameString())
                            if (!recordEvent.hasError()) {
                                val outputUri = recordEvent.outputResults.outputUri
                                recordEvent.outputResults.outputUri
                                val msg = "Video capture succeeded: $outputUri"
                                Log.d(TAG, msg)

                                val file = File(outputUri?.path)
                                val fileSize = file.length() / 1024

                                val data = JSObject().apply {
                                    put("errorMessage", null)
                                    put("filePath", outputUri)
                                }
                                notifyListeners("captureVideoFinished", data)
                            } else {
                                recording?.close()
                                recording = null

                                val msg = "Video capture ends with error: ${recordEvent.error}"
                                Log.e(TAG, msg)

                                val data = JSObject().apply {
                                    put("errorMessage", msg)
                                    put("filePath", null)
                                }

                                notifyListeners("captureVideoFinished", data)
                            }
                        }
                        is VideoRecordEvent.Pause -> {
                            // fragmentCameraBinding.captureButton.setImageResource(R.drawable.ic_resume)
                            // TODO: notify ionic side
                            val msg = "VideoRecordEvent.Pause"
                            Log.e(TAG, msg)
                        }
                        is VideoRecordEvent.Resume -> {
                            // fragmentCameraBinding.captureButton.setImageResource(R.drawable.ic_pause)
                            // TODO: notify ionic side
                            val msg = "VideoRecordEvent.Pause"
                            Log.e(TAG, msg)
                        }
                    }
                }

            if (this.flashModeAvailable && flashMode == ImageCapture.FLASH_MODE_ON) {
                camera?.cameraControl?.enableTorch(true)
            } else {
                camera?.cameraControl?.enableTorch(false)
            }
        } catch (exc: IllegalStateException) {
            Log.d(TAG, exc.message ?: "IllegalStateException")
            call.reject("IllegalStateException", exc)
        }


    }

    fun videoCaptureStop(call: PluginCall) {
        recording?.stop()
        recording = null
        call.resolve()
    }

    fun flipCamera(call: PluginCall) {
        if (this.recording != null) {
            // TODO: reject flip and warn to stop recording before flipping the camera
        }

        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        flashModeAvailable = lensFacing == CameraSelector.LENS_FACING_BACK

        flashMode = if (flashModeAvailable && flashMode == ImageCapture.FLASH_MODE_ON) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }

        // Re-bind use cases to update selected camera
        bindCameraUseCases()
    }

    fun focus(x: Float, y: Float) {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            metrics.width().toFloat(), metrics.height().toFloat()
        )
        val autoFocusPoint = factory.createPoint(x, y)
        try {
            camera?.cameraControl?.startFocusAndMetering(
                FocusMeteringAction.Builder(
                    autoFocusPoint,
                    FocusMeteringAction.FLAG_AF
                ).apply {
                    //focus only when the user tap the preview
                    disableAutoCancel()
                }.build()
            )
        } catch (e: CameraInfoUnavailableException) {
            Log.d("ERROR", "cannot focus camera", e)
        }
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.getCurrentWindowMetrics().bounds

        Log.d(TAG, "Screen metrics: ${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = fragmentCameraBinding.viewFinder.display.rotation

        // CameraProvider TODO: handle IllegalStateException for cameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        var aspectRatio = aspectRatio(metrics.width(), metrics.height())
        if (this.captureQuality == "low") {
            aspectRatio = aspectRatio(1920, 1080)
            if (rotation % 2 == 0) {
                aspectRatio = aspectRatio(1080, 1920)
            }
        }
        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(aspectRatio)
//            .setTargetAspectRatio(aspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider) }

        var imageQuality = 100
        var videoQuality = Quality.HIGHEST
        var captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        if (captureQuality == "low") {
            imageQuality = 80
            videoQuality = Quality.LOWEST
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
        // ImageCapture
        imageCapture = ImageCapture.Builder()
            // .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(aspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            // .setCaptureMode(captureMode)
            .setJpegQuality(imageQuality)
            .setFlashMode(flashMode)
            .build()

        // VideoRecorder
        val recorder = Recorder.Builder().setQualitySelector(
            QualitySelector.from(
                videoQuality,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )
        ).build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner, cameraSelector, preview, imageCapture, videoCapture
            )

            val supportedQualities = QualitySelector.getSupportedQualities(camera!!.cameraInfo)
            for (quality in supportedQualities) {
                when (quality) {
                    Quality.UHD -> {
                        Log.e(TAG, "Supported quality: Ultra High Definition (UHD) - 2160p")
                    }
                    Quality.FHD -> {
                        //Add "Full High Definition (FHD) - 1080p" to the list
                        Log.e(TAG, "Supported quality: Full High Definition (FHD) - 1080p")
                    }
                    Quality.HD -> {
                        //Add "High Definition (HD) - 720p" to the list
                        Log.e(TAG, "Supported quality: High Definition (HD) - 720p")
                    }
                    Quality.SD -> {
                        //Add "Standard Definition (SD) - 480p" to the list
                        Log.e(TAG, "Supported quality: Standard Definition (SD) - 480p")
                    }
                }
            }

//            if (flashMode == ImageCapture.FLASH_MODE_ON)
//                camera?.cameraControl?.enableTorch(true)
//            if (flashMode == ImageCapture.FLASH_MODE_OFF)
//                camera?.cameraControl?.enableTorch(false)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    fun isTorchAvailable(): Boolean {
        return this.flashModeAvailable && CameraSelector.LENS_FACING_BACK == lensFacing
//        if (lensFacing === CameraSelector.LENS_FACING_BACK) return true
//        return false
//        return camera?.cameraInfo?.hasFlashUnit() ?: false
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps

//                        Toast.makeText(
//                            context,
//                            "CameraState: Pending Open",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
//                        Toast.makeText(
//                            context,
//                            "CameraState: Opening",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
//                        Toast.makeText(
//                            context,
//                            "CameraState: Open",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
//                        Toast.makeText(
//                            context,
//                            "CameraState: Closing",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
//                        Toast.makeText(
//                            context,
//                            "CameraState: Closed",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
//                        Toast.makeText(
//                            context,
//                            "Stream config error",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
//                        Toast.makeText(
//                            context,
//                            "Camera in use",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
//                        Toast.makeText(
//                            context,
//                            "Max cameras in use",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
//                        Toast.makeText(
//                            context,
//                            "Other recoverable error",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
//                        Toast.makeText(
//                            context,
//                            "Camera disabled",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
//                        Toast.makeText(
//                            context,
//                            "Fatal error",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
//                        Toast.makeText(
//                            context,
//                            "Do not disturb mode enabled",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }
            }
        }
    }


    override fun onDestroy() {

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        _fragmentCameraBinding = null
        super.onDestroy()
        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        // broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
    }

    fun enableTorch(enable: Boolean) {
        if (enable && flashModeAvailable) {
            this.flashMode = ImageCapture.FLASH_MODE_ON
        } else {
            this.flashMode = ImageCapture.FLASH_MODE_OFF
        }
        this.bindCameraUseCases()
    }

    fun minAvailableZoom(): Float {
        val defaultMinZoomRatio = 0f
        val minZoomRatio = camera?.cameraInfo?.zoomState?.value?.minZoomRatio
        return minZoomRatio ?: defaultMinZoomRatio
    }

    fun maxAvailableZoom(): Float {
        val defaultMaxZoomRatio = 0f
        val maxZoomRatio = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio
        return maxZoomRatio ?: defaultMaxZoomRatio
    }

    fun zoom(zoomFactor: Float) {
        camera?.cameraControl?.setZoomRatio(zoomFactor)
    }

    fun setQuality(quality: String) {
        if (quality == "low") {
            this.captureQuality = "low"
        } else {
            this.captureQuality = "hq"
        }
        if (cameraSetupCompleted) {
            bindCameraUseCases()
        }
    }

    companion object {
        private val TAG = PreviewCameraFragment::class.java.simpleName

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            return appContext.filesDir
            // val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            //    File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            // return if (mediaDir != null && mediaDir.exists())
            //    mediaDir else appContext.filesDir
        }

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpeg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String): File {
            val fileName = SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension

            return File(baseFolder, fileName)
        }

        // TODO: remove code below
        /** Maximum number of images that will be held in the reader's buffer */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        /** Maximum time allowed to wait for the result of an image capture */
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        /** Helper data class used to hold capture metadata with their associated image */
        data class CombinedCaptureResult(
            val image: Image,
            val metadata: CaptureResult,
            val orientation: Int,
            val format: Int
        ) : Closeable {
            override fun close() = image.close()
        }

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param cameraId Parameter 1.
         * @param pixelFormat Parameter 2.
         * @return A new instance of fragment PreviewCameraFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(cameraId: String, pixelFormat: Int, flashEnabled: Boolean) =
            PreviewCameraFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, cameraId)
                    putInt(ARG_PARAM2, pixelFormat)
                    putBoolean(ARG_PARAM3, flashEnabled)
                }
            }
    }
}