#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(PreviewCameraPlugin, "PreviewCamera",
           CAP_PLUGIN_METHOD(echo, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(startPreview, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(stopPreview, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(takePhoto, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(startRecord, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(stopRecord, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(flipCamera, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getFlashModes, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setFlashModes, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(isTorchOn, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(enableTorch, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(isTorchAvailable, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(focus, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(minAvailableZoom, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(maxAvailableZoom, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(zoom, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setQuality, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(saveFileToUserDevice, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(checkPermissions, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(requestPermissions, CAPPluginReturnPromise);
)
