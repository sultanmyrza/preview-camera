# @numbersprotocol/preview-camera

Preview Camera Plugin for iOS, Android

## Install

```bash
npm install @numbersprotocol/preview-camera
npx cap sync
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
* [`addListener('captureVideoFinished', ...)`](#addlistenercapturevideofinished)
* [`addListener('capturePhotoFinished', ...)`](#addlistenercapturephotofinished)
* [Interfaces](#interfaces)

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


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### CaptureResult

| Prop               | Type                | Description                                                                                 |
| ------------------ | ------------------- | ------------------------------------------------------------------------------------------- |
| **`filePath`**     | <code>string</code> | File path for photo or video taken by camera. Example: file://your-app-dir/.../my-video.mp4 |
| **`errorMessage`** | <code>string</code> | Error message if any                                                                        |

</docgen-api>
