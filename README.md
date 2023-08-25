# @numbersprotocol/preview-camera

Preview Camera Plugin for iOS, Android

## Install

```bash
npm install https://github.com/sultanmyrza/preview-camera.git
npx cap sync
```

## iOS

iOS requires the following usage description be added and filled out for your app in `Info.plist`:

- `NSCameraUsageDescription` (`Privacy - Camera Usage Description`)
- `NSPhotoLibraryAddUsageDescription` (`Privacy - Photo Library Additions Usage Description`)
- `NSPhotoLibraryUsageDescription` (`Privacy - Photo Library Usage Description`)

Read about [Configuring `Info.plist`](https://capacitorjs.com/docs/ios/configuration#configuring-infoplist) in the [iOS Guide](https://capacitorjs.com/docs/ios) for more information on setting iOS permissions in Xcode

## Android

This API requires the following permissions be added to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

You can also specify those permissions only for the Android versions where they will be requested:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`startPreview()`](#startpreview)
* [`stopPreview()`](#stoppreview)
* [`takePhoto()`](#takephoto)
* [`capturePhoto()`](#capturephoto)
* [`startRecord()`](#startrecord)
* [`stopRecord()`](#stoprecord)
* [`flipCamera()`](#flipcamera)
* [`getFlashModes()`](#getflashmodes)
* [`setFlashMode()`](#setflashmode)
* [`isTorchOn()`](#istorchon)
* [`enableTorch(...)`](#enabletorch)
* [`isTorchAvailable()`](#istorchavailable)
* [`focus(...)`](#focus)
* [`minAvailableZoom()`](#minavailablezoom)
* [`maxAvailableZoom()`](#maxavailablezoom)
* [`zoom(...)`](#zoom)
* [`setQuality(...)`](#setquality)
* [`saveFileToUserDevice(...)`](#savefiletouserdevice)
* [`checkPermissions()`](#checkpermissions)
* [`requestPermissions()`](#requestpermissions)
* [`addListener('captureSuccessResult', ...)`](#addlistenercapturesuccessresult)
* [`addListener('captureErrorResult', ...)`](#addlistenercaptureerrorresult)
* [`addListener('captureVideoFinished', ...)`](#addlistenercapturevideofinished)
* [`addListener('capturePhotoFinished', ...)`](#addlistenercapturephotofinished)
* [`addListener('accelerometerOrientation', ...)`](#addlisteneraccelerometerorientation)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### startPreview()

```typescript
startPreview() => Promise<void>
```

--------------------


### stopPreview()

```typescript
stopPreview() => Promise<void>
```

--------------------


### takePhoto()

```typescript
takePhoto() => Promise<void>
```

--------------------


### capturePhoto()

```typescript
capturePhoto() => Promise<void>
```

--------------------


### startRecord()

```typescript
startRecord() => Promise<void>
```

--------------------


### stopRecord()

```typescript
stopRecord() => Promise<void>
```

--------------------


### flipCamera()

```typescript
flipCamera() => Promise<void>
```

--------------------


### getFlashModes()

```typescript
getFlashModes() => Promise<any>
```

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### setFlashMode()

```typescript
setFlashMode() => Promise<void>
```

--------------------


### isTorchOn()

```typescript
isTorchOn() => Promise<{ result: boolean; }>
```

**Returns:** <code>Promise&lt;{ result: boolean; }&gt;</code>

--------------------


### enableTorch(...)

```typescript
enableTorch(options: { enable: boolean; }) => Promise<void>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ enable: boolean; }</code> |

--------------------


### isTorchAvailable()

```typescript
isTorchAvailable() => Promise<{ result: boolean; }>
```

**Returns:** <code>Promise&lt;{ result: boolean; }&gt;</code>

--------------------


### focus(...)

```typescript
focus(options: { x: number; y: number; }) => Promise<void>
```

| Param         | Type                                   |
| ------------- | -------------------------------------- |
| **`options`** | <code>{ x: number; y: number; }</code> |

--------------------


### minAvailableZoom()

```typescript
minAvailableZoom() => Promise<{ result: number; }>
```

**Returns:** <code>Promise&lt;{ result: number; }&gt;</code>

--------------------


### maxAvailableZoom()

```typescript
maxAvailableZoom() => Promise<{ result: number; }>
```

**Returns:** <code>Promise&lt;{ result: number; }&gt;</code>

--------------------


### zoom(...)

```typescript
zoom(options: { factor: number; }) => Promise<void>
```

| Param         | Type                             |
| ------------- | -------------------------------- |
| **`options`** | <code>{ factor: number; }</code> |

--------------------


### setQuality(...)

```typescript
setQuality(options: { quality: 'low' | 'hq'; }) => Promise<void>
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ quality: 'low' \| 'hq'; }</code> |

--------------------


### saveFileToUserDevice(...)

```typescript
saveFileToUserDevice(options: { filePath: string; }) => Promise<void>
```

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ filePath: string; }</code> |

--------------------


### checkPermissions()

```typescript
checkPermissions() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### requestPermissions()

```typescript
requestPermissions() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### addListener('captureSuccessResult', ...)

```typescript
addListener(eventName: 'captureSuccessResult', listenerFunc: (data: CaptureSuccessResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                                     |
| ------------------ | ---------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'captureSuccessResult'</code>                                                      |
| **`listenerFunc`** | <code>(data: <a href="#capturesuccessresult">CaptureSuccessResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('captureErrorResult', ...)

```typescript
addListener(eventName: 'captureErrorResult', listenerFunc: (data: CaptureErrorResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                                 |
| ------------------ | ------------------------------------------------------------------------------------ |
| **`eventName`**    | <code>'captureErrorResult'</code>                                                    |
| **`listenerFunc`** | <code>(data: <a href="#captureerrorresult">CaptureErrorResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('captureVideoFinished', ...)

```typescript
addListener(eventName: 'captureVideoFinished', listenerFunc: (data: CaptureResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                       |
| ------------------ | -------------------------------------------------------------------------- |
| **`eventName`**    | <code>'captureVideoFinished'</code>                                        |
| **`listenerFunc`** | <code>(data: <a href="#captureresult">CaptureResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('capturePhotoFinished', ...)

```typescript
addListener(eventName: 'capturePhotoFinished', listenerFunc: (data: CaptureResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                       |
| ------------------ | -------------------------------------------------------------------------- |
| **`eventName`**    | <code>'capturePhotoFinished'</code>                                        |
| **`listenerFunc`** | <code>(data: <a href="#captureresult">CaptureResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('accelerometerOrientation', ...)

```typescript
addListener(eventName: 'accelerometerOrientation', listenerFunc: (data: { orientation: CustomOrientation; }) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                                                 |
| ------------------ | ---------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'accelerometerOrientation'</code>                                                              |
| **`listenerFunc`** | <code>(data: { orientation: <a href="#customorientation">CustomOrientation</a>; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### PermissionStatus

- `camera` permission allows to take photo and record video without audio.
- `microphone` permission allows to record video with audio.

| Prop             | Type                                                        |
| ---------------- | ----------------------------------------------------------- |
| **`camera`**     | <code><a href="#permissionstate">PermissionState</a></code> |
| **`microphone`** | <code><a href="#permissionstate">PermissionState</a></code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### CaptureSuccessResult

Represents the result of a successful capture operation.

| Prop           | Type                                     | Description                                                                    |
| -------------- | ---------------------------------------- | ------------------------------------------------------------------------------ |
| **`mimeType`** | <code>'image/jpeg' \| 'video/mp4'</code> | The MIME type of the captured media. Examples: "image/jpeg", "video/mp4".      |
| **`name`**     | <code>string</code>                      | The name of the captured media file. Examples: "my-photo.jpg", "my-video.mp4". |
| **`path`**     | <code>string</code>                      | The path to the captured media file. Example: "file://path-to-my-video.mp4".   |
| **`size`**     | <code>number</code>                      | The size of the captured media file in bytes. Example: "7046447".              |


#### CaptureErrorResult

Represents the result of a failed capture operation.

| Prop               | Type                | Description                                            |
| ------------------ | ------------------- | ------------------------------------------------------ |
| **`errorMessage`** | <code>string</code> | The error message describing the cause of the failure. |


#### CaptureResult

| Prop               | Type                | Description                                                                                 |
| ------------------ | ------------------- | ------------------------------------------------------------------------------------------- |
| **`filePath`**     | <code>string</code> | File path for photo or video taken by camera. Example: file://your-app-dir/.../my-video.mp4 |
| **`errorMessage`** | <code>string</code> | Error message if any                                                                        |


### Type Aliases


#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>


#### CustomOrientation

<code>'portraitUp' | 'portraitDown' | 'landscapeRight' | 'landscaperLeft'</code>

</docgen-api>
