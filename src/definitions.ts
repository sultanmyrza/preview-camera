import { PluginListenerHandle } from '@capacitor/core';

export interface CaptureResult {
  /** File path for photo or video taken by camera. Example: file://your-app-dir/.../my-video.mp4 */
  filePath?: string;
  /** Error message if any */
  errorMessage?: string;
}

export interface PreviewCameraPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  startPreview(): Promise<void>;
  stopPreview(): Promise<void>;
  takePhoto(): Promise<void>;
  capturePhoto(): Promise<void>;
  startRecord(): Promise<void>;
  stopRecord(): Promise<void>;
  flipCamera(): Promise<void>;
  getFlashModes(): Promise<any>;
  setFlashMode(): Promise<void>;
  isTorchOn(): Promise<{ result: boolean }>;
  enableTorch(options: { enable: boolean }): Promise<void>;
  addListener(
    eventName: 'captureVideoFinished',
    listenerFunc: (data: CaptureResult) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  addListener(
    eventName: 'capturePhotoFinished',
    listenerFunc: (data: CaptureResult) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}
