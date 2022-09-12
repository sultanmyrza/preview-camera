package io.numbersprotocol.capturelite.plugins.previewcamera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.ImageCapture
import com.getcapacitor.Bridge
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall


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
                val displayMetrics = DisplayMetrics()
                bridge.activity.windowManager.defaultDisplay.getMetrics(displayMetrics)

                previewLayerWrapper!!.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }

            (bridge.webView.parent as ViewGroup).addView(previewLayerWrapper)
            bridge.webView.bringToFront();
            hideWebViewBackground()

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
                bridge.webView.setBackgroundColor(Color.WHITE)
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