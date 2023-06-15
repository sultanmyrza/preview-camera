package io.numbersprotocol.capturelite.plugins.previewcamera

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import androidx.camera.core.ImageCapture
import com.getcapacitor.Bridge
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI


typealias CapacitorNotifyListener = (String, JSObject) -> Unit

class PreviewCamera(private val bridge: Bridge) {

    private var cameraPreviewStarted: Boolean = false
    private var cameraRecordStarted: Boolean = false
    private var previewWrapperFrameId = View.generateViewId()
    private var previewLayerWrapper: FrameLayout? = null
    private var flashEnabled = false


    //  private var previewLayer: BlankFragment? = null
    private var previewCameraFragment: PreviewCameraFragment? = null

    fun echo(value: String): String {
        Log.i("Echo", value)
        return value
    }

    fun isTorchOn(): Boolean {
        return previewCameraFragment?.flashMode == ImageCapture.FLASH_MODE_ON
    }

    fun enableTorch(enable: Boolean) {
        bridge.activity.runOnUiThread {
            previewCameraFragment?.enableTorch(enable)
            flashEnabled = enable
        }
    }

    fun isTorchAvailable(): Boolean {
        return previewCameraFragment?.isTorchAvailable() ?: false
    }

    fun startPreview(call: PluginCall) {
        val cameraManager =
            bridge.activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraList = enumerateCameras(cameraManager)
        val firstCamera = cameraList.first()
        val cameraId = firstCamera.cameraId
        val format = firstCamera.format

        bridge.activity.runOnUiThread {
            previewLayerWrapper = bridge.activity.findViewById(previewWrapperFrameId)

            if (previewLayerWrapper == null) {
                previewLayerWrapper = FrameLayout(bridge.activity)
                previewLayerWrapper!!.id = previewWrapperFrameId

                // TODO: uncomment if camera is not showing
                // Get Screen Width, Height and set PreviewWrapper stretch to max width and height

                previewLayerWrapper!!.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }

            (bridge.webView.parent as ViewGroup).addView(previewLayerWrapper)
            bridge.webView.bringToFront();
            bridge.webView.setBackgroundColor(Color.TRANSPARENT)
//            hideWebViewBackground()

            val fragmentManger = bridge.activity.supportFragmentManager
            val fragmentTransaction = fragmentManger.beginTransaction()
            previewCameraFragment =
                PreviewCameraFragment.newInstance(cameraId, format, flashEnabled)

            fragmentTransaction.add(previewLayerWrapper!!.id, previewCameraFragment!!)
            fragmentTransaction.commit()

            call.resolve()
        }
    }

    fun stopPreview(call: PluginCall) {
        bridge.activity.runOnUiThread {
            val previewWrapper: FrameLayout? = bridge.activity.findViewById(previewWrapperFrameId)

            if (previewWrapper != null) {
                if (previewCameraFragment != null) {
                    val fragmentManager = bridge.activity.supportFragmentManager
                    val fragmentTransaction = fragmentManager.beginTransaction()
                    fragmentTransaction.remove(previewCameraFragment!!)
                    fragmentTransaction.commit()
                }

                (bridge.webView.parent as ViewGroup).removeView(previewWrapper)

            }

             showWebViewBackground()

            call.resolve()
        }
    }

    fun takePhoto(call: PluginCall, notifyListeners: CapacitorNotifyListener) {

        previewCameraFragment?.takePhoto(call, notifyListeners)

    }

    fun flipCamera(call: PluginCall) {
        bridge.activity.runOnUiThread {
            previewCameraFragment?.flipCamera(call)
            call.resolve()
        }
    }

    fun startRecord(call: PluginCall, notifyListeners: CapacitorNotifyListener) {
        previewCameraFragment?.videoCaptureStart(call, notifyListeners)
    }

    fun stopRecord(call: PluginCall) {
        previewCameraFragment?.videoCaptureStop(call)
    }

    private fun hideWebViewBackground() {
        // Because of the fact that the PreviewCamera will be rendered behind the WebView, you will
        // have to call hideBackground() to make the WebView and the <html> element transparent.
        // Every other element that needs transparency, you will have to handle yourself.
        bridge.activity
            .runOnUiThread {
                bridge.webView.setBackgroundColor(Color.TRANSPARENT)
                bridge.webView
                    .loadUrl("javascript:document.documentElement.style.backgroundColor = 'transparent';void(0);")
                // isBackgroundHidden = true
            }
    }

    private fun showWebViewBackground() {
        bridge.activity
            .runOnUiThread {
                bridge.webView.setBackgroundColor(Color.BLACK)
                bridge.webView
                    .loadUrl("javascript:document.documentElement.style.backgroundColor = '';void(0);")
                // isBackgroundHidden = false
            }
    }

    fun focus(x: Float, y: Float) {
        previewCameraFragment?.focus(x, y)
    }

    fun minAvailableZoom(): Float {
        return previewCameraFragment?.minAvailableZoom() ?: 0f
    }

    fun maxAvailableZoom(): Float {
        return previewCameraFragment?.maxAvailableZoom() ?: 0f
    }

    fun zoom(zoomFactor: Float) {
        previewCameraFragment?.zoom(zoomFactor)
    }

    fun setQuality(quality: String) {
        bridge.activity.runOnUiThread {
            previewCameraFragment?.setQuality(quality)
        }
    }

    fun shouldRebuildCameraUseCases(newCustomOrientation: String) {
        if (previewCameraFragment?.recording != null) return;

        bridge.activity.runOnUiThread {
            var orientation = Surface.ROTATION_0
            if (newCustomOrientation == "landscapeRight")
                orientation = Surface.ROTATION_90
            if (newCustomOrientation == "portraitDown")
                orientation = Surface.ROTATION_180
            if (newCustomOrientation == "landscapeLeft")
                orientation = Surface.ROTATION_270
//            previewCameraFragment?.bindCameraUseCases(orientation)
            Log.d("Custom Orientation", newCustomOrientation)
            previewCameraFragment?.customOrientation = orientation
        }
    }

    fun saveFileToUserDevice(context: Context, filePath: String) {
        val extension = filePath.substringAfterLast(".", "")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        val absoluteFilePath = File(URI(filePath)).absolutePath
        if (mimeType?.startsWith("image/") == true) {
            savePhotoToGallery(context, absoluteFilePath)
        }
        if (mimeType?.startsWith("video/") == true) {
            saveVideoToGallery(context, absoluteFilePath)
        }
    }

    private fun savePhotoToGallery(context: Context, absoluteFilePath: String) {
        val resolver: ContentResolver = context.contentResolver

        val fileName = absoluteFilePath.split("/").last()

        // Create a bitmap from the file path
        val bitmap = BitmapFactory.decodeFile(absoluteFilePath)

        // Prepare the ContentValues for inserting the image
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/${getAppName(context)}")
            }
        }

        // Insert the image
        val imageUri: Uri? =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Save the image to the gallery
        imageUri?.let { uri ->
            val outputStream: OutputStream? = resolver.openOutputStream(uri)
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
        } ?: run {
            println("Error: Failed to insert image")
        }
    }

    /**
     * This function first prepares ContentValues to insert the video with the required metadata,
     * such as display name, MIME type, and relative path within the "Movies" folder.
     * The resolver.insert() method is called to insert the video, and the returned Uri is used to
     * open an OutputStream to save the video to the Gallery. The copyFile() function is used to copy
     * the video from the source path to the output stream.
     */
    private fun saveVideoToGallery(context: Context, absoluteFilePath: String) {
        val resolver: ContentResolver = context.contentResolver

        val fileName = absoluteFilePath.split("/").last()

        // Prepare the ContentValues for inserting the video
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/${getAppName(context)}")
            }
        }

        // Insert the video
        val videoUri: Uri? =
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Save the video to the gallery
        videoUri?.let { uri ->
            val outputStream: OutputStream? = resolver.openOutputStream(uri)
            copyFile(absoluteFilePath, outputStream)
        } ?: run {
            println("Error: Failed to insert video")
        }
    }

    fun getAppName(context: Context): String {
        val packageManager = context.packageManager
        val applicationInfo = context.applicationInfo
        return packageManager.getApplicationLabel(applicationInfo).toString()
    }

    @Throws(IOException::class)
    fun copyFile(sourcePath: String, outputStream: OutputStream?) {
        FileInputStream(sourcePath).use { inputStream ->
            outputStream?.use { output ->
                inputStream.copyTo(output)
            }
        }
    }


    companion object {

        /** Helper class used as a data holder for each selectable camera format item */
        private data class FormatItem(val title: String, val cameraId: String, val format: Int)

        /** Helper function used to convert a lens orientation enum into a human-readable string */
        private fun lensOrientationString(value: Int) = when (value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        /** Helper function used to list all compatible cameras and supported pixel formats */
        @SuppressLint("InlinedApi")
        private fun enumerateCameras(cameraManager: CameraManager): List<FormatItem> {
            val availableCameras: MutableList<FormatItem> = mutableListOf()

            // Get list of all compatible cameras
            val cameraIds = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )
                capabilities?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                ) ?: false
            }


            // Iterate over the list of cameras and return all the compatible ones
            cameraIds.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = lensOrientationString(
                    characteristics.get(CameraCharacteristics.LENS_FACING)!!
                )

                // Query the available capabilities and output formats
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )!!
                val outputFormats = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )!!.outputFormats

                // All cameras *must* support JPEG output so we don't need to check characteristics
                availableCameras.add(
                    FormatItem(
                        "$orientation JPEG ($id)", id, ImageFormat.JPEG
                    )
                )

                // Return cameras that support RAW capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
                    ) &&
                    outputFormats.contains(ImageFormat.RAW_SENSOR)
                ) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation RAW ($id)", id, ImageFormat.RAW_SENSOR
                        )
                    )
                }

                // Return cameras that support JPEG DEPTH capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
                    ) &&
                    outputFormats.contains(ImageFormat.DEPTH_JPEG)
                ) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation DEPTH ($id)", id, ImageFormat.DEPTH_JPEG
                        )
                    )
                }
            }

            return availableCameras
        }
    }
}

fun Float.isBetween(a: Double, b: Double): Boolean {
    return a <= this && this <= b
}