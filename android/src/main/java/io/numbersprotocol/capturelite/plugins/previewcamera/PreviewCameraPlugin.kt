package io.numbersprotocol.capturelite.plugins.previewcamera

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import kotlin.math.atan2


@CapacitorPlugin(
    name = "PreviewCamera",
    permissions = [
        Permission(
            strings = [Manifest.permission.CAMERA],
            alias = PreviewCameraPlugin.CAMERA_PERMISSION_ALIAS
        ), Permission(
            strings = [Manifest.permission.RECORD_AUDIO],
            alias = PreviewCameraPlugin.RECORD_AUDIO_PERMISSION_ALIAS
        )
    ]

)
class PreviewCameraPlugin : Plugin(), SensorEventListener {

    companion object {
        // FIXME: rename cameraPermissionAlias to camera
        const val CAMERA_PERMISSION_ALIAS = "cameraPermissionAlias"

        // FIXME: rename cameraPermissionAlias to microphone
        const val RECORD_AUDIO_PERMISSION_ALIAS = "recordAudioPermissionAlias"
    }

    private lateinit var implementation: PreviewCamera

    private var allowUsersRecordVideoWithoutSound = false

    private lateinit var sensorManager: SensorManager

    private var lastSensorNotificationTimeInMilliseconds = System.currentTimeMillis()
    private val sensorNotificationIntervalInMilliseconds = 1400

    private var customOrientation = ".portraitUp"

    override fun load() {
        implementation = PreviewCamera(bridge)
        sensorManager = bridge.activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        super.load()
    }

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value") ?: "No value provided"
        val ret = JSObject()
        ret.put("value", implementation.echo(value))
        call.resolve(ret)
    }

    @PluginMethod
    fun isTorchOn(call: PluginCall) {
        val result = JSObject()
        result.put("result", implementation.isTorchOn())
        call.resolve(result)
    }

    @PluginMethod
    fun enableTorch(call: PluginCall) {
        val value = call.getBoolean("enable") ?: false
        implementation.enableTorch(value)
        call.resolve()
    }

    @PluginMethod
    fun isTorchAvailable(call: PluginCall) {
        val result = JSObject()
        result.put("result", implementation.isTorchAvailable())
        call.resolve(result)
    }

    @PluginMethod
    fun startPreview(call: PluginCall) {

        val hasCameraPermission =
            PermissionState.GRANTED == getPermissionState(CAMERA_PERMISSION_ALIAS)

//        val hasAudioPermission =
//            PermissionState.GRANTED == getPermissionState(RECORD_AUDIO_PERMISSION_ALIAS)


        if (hasCameraPermission) {
            startAccelerometerBroadcast()
            implementation.startPreview(call)
        } else {
            requestPermissionForAlias(CAMERA_PERMISSION_ALIAS, call, "handleCameraPermissionResult")
            // requestPermissionForAlias(RECORD_AUDIO_PERMISSION_ALIAS, call, "handleRecordAudioPermissionResult")
        }
    }

    @PluginMethod
    override fun checkPermissions(call: PluginCall) {
        val cameraPermissionState = getPermissionState(CAMERA_PERMISSION_ALIAS)
        val microphonePermissionState = getPermissionState(RECORD_AUDIO_PERMISSION_ALIAS)

        val result = JSObject()
        result.put("camera", cameraPermissionState)
        result.put("microphone", microphonePermissionState)
        call.resolve(result)
    }

    @PluginMethod
    override fun requestPermissions(call: PluginCall) {
        requestAllPermissions(call, "requestAllPermissionsCallback")
    }

    @PermissionCallback
    fun requestAllPermissionsCallback(call: PluginCall) {
        val cameraPermissionState = getPermissionState(CAMERA_PERMISSION_ALIAS)
        val microphonePermissionState = getPermissionState(RECORD_AUDIO_PERMISSION_ALIAS)

        val result = JSObject()
        result.put("camera", cameraPermissionState)
        result.put("microphone", microphonePermissionState)
        call.resolve(result)
    }



    @PluginMethod
    fun saveFileToUserDevice(call: PluginCall) {
        val filePath = call.getString("filePath") ?: return call.resolve()
        implementation.saveFileToUserDevice(context, filePath)
        call.resolve()

    }

    private fun startAccelerometerBroadcast() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopAccelerometerBroadcast() {
        sensorManager.unregisterListener(this)
    }

    @PluginMethod
    fun stopPreview(call: PluginCall) {
        implementation.stopPreview(call);
        stopAccelerometerBroadcast()
    }

    @PluginMethod
    fun takePhoto(call: PluginCall) {
        // TODO: check if camera started etc (Hint: copy logic from ios version)
        implementation.takePhoto(call, this::notifyListeners)
    }


    @PluginMethod
    fun flipCamera(call: PluginCall) {
        // TODO: check if camera started etc (Hint: copy logic from ios version)
        implementation.flipCamera(call)
    }

    @PluginMethod
    fun focus(call: PluginCall) {
        val x = call.getFloat("x") ?: return call.resolve()
        val y = call.getFloat("y") ?: return call.resolve()
        implementation.focus(x, y)
        call.resolve()
    }

    @PluginMethod
    fun setQuality(call: PluginCall) {
        val quality = call.getString("quality") ?: "hq"
        implementation.setQuality(quality)
        call.resolve()
    }

    @PluginMethod
    fun minAvailableZoom(call: PluginCall) {
        val result = JSObject()
        result.put("result", implementation.minAvailableZoom())
        call.resolve(result)
    }

    @PluginMethod
    fun maxAvailableZoom(call: PluginCall) {
        val result = JSObject()
        result.put("result", implementation.maxAvailableZoom())
        call.resolve(result)
    }

    @PluginMethod
    fun zoom(call: PluginCall) {
        val zoomFactor = call.getFloat("factor") ?: return call.resolve()
        implementation.zoom(zoomFactor)
        call.resolve()
    }


    @PluginMethod
    fun startRecord(call: PluginCall) {
        // TODO: check if camera started etc (Hint: copy logic from ios version)

        implementation.startRecord(call, this::notifyListeners)


        if (PermissionState.GRANTED == getPermissionState(RECORD_AUDIO_PERMISSION_ALIAS)) {
            implementation.startRecord(call, this::notifyListeners)
        } else {
            requestPermissionForAlias(
                RECORD_AUDIO_PERMISSION_ALIAS,
                call,
                "handleRecordAudioPermissionResult"
            )
        }
    }

    @PluginMethod
    fun stopRecord(call: PluginCall) {
        // TODO: check if camera started etc (Hint: copy logic from ios version)
        implementation.stopRecord(call)
    }

    @PermissionCallback
    private fun handleCameraPermissionResult(call: PluginCall) {
        if (PermissionState.GRANTED == getPermissionState(CAMERA_PERMISSION_ALIAS)) {
            startAccelerometerBroadcast()
            implementation.startPreview(call)
        } else {
            call.reject("Camera permission not granted")
        }
    }

    @PermissionCallback
    private fun handleRecordAudioPermissionResult(call: PluginCall) {
        val hasAudioPermission =
            PermissionState.GRANTED == getPermissionState(RECORD_AUDIO_PERMISSION_ALIAS)
        if (hasAudioPermission) {
            implementation.startRecord(call, this::notifyListeners)
        } else {
            if (allowUsersRecordVideoWithoutSound) {
                implementation.startRecord(call, this::notifyListeners)
            } else {
                call.reject("Record Audio permission not granted")
            }
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (!shouldNotifyAccelerometerChange()) return
        if (sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val (accelerationX, accelerationY, accelerationZ) = sensorEvent.values
            val preciseInclinationInRad = atan2(accelerationY, accelerationX)
            val inclinationRad = String.format("%.1f", preciseInclinationInRad).toFloat()

            var newCustomOrientation = "portraitUp"
            if (inclinationRad.isBetween(0.6, 2.2))
                newCustomOrientation = "portraitUp"
            if (inclinationRad.isBetween(2.3, 3.3) || inclinationRad.isBetween(-3.3, -2.3))
                newCustomOrientation = "landscapeRight"
            if (inclinationRad.isBetween(-2.3, -0.9))
                newCustomOrientation = "portraitDown"
            if (inclinationRad.isBetween(-0.9, 0.0) || inclinationRad.isBetween(0.0, 0.6))
                newCustomOrientation = "landscapeLeft"


            if (newCustomOrientation != customOrientation) {
                implementation.shouldRebuildCameraUseCases(newCustomOrientation)
            }

            val data = JSObject()
            data.put("orientation", newCustomOrientation)
            notifyListeners("accelerometerOrientation", data)

            lastSensorNotificationTimeInMilliseconds = System.currentTimeMillis()
            customOrientation = newCustomOrientation
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // No logic needed here overriding onSensorChanged is enough for our use case
    }

    private fun shouldNotifyAccelerometerChange(): Boolean {
        val currentTime = System.currentTimeMillis()
        val difference = currentTime - lastSensorNotificationTimeInMilliseconds
        return difference > sensorNotificationIntervalInMilliseconds;
    }
}