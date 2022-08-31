package io.numbersprotocol.capturelite.plugins.previewcamera

import android.Manifest
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback


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
class PreviewCameraPlugin : Plugin() {

    companion object {
        const val CAMERA_PERMISSION_ALIAS = "cameraPermissionAlias"
        const val RECORD_AUDIO_PERMISSION_ALIAS = "recordAudioPermissionAlias"
    }

    private lateinit var implementation: PreviewCamera

    private var allowUsersRecordVideoWithoutSound = false

    override fun load() {
        implementation = PreviewCamera(bridge)
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
            implementation.startPreview(call)
        } else {
            requestPermissionForAlias(CAMERA_PERMISSION_ALIAS, call, "handleCameraPermissionResult")
            // requestPermissionForAlias(RECORD_AUDIO_PERMISSION_ALIAS, call, "handleRecordAudioPermissionResult")
        }
    }

    @PluginMethod
    fun stopPreview(call: PluginCall) {
        implementation.stopPreview(call);
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
}