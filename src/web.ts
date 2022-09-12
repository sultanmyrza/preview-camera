import { WebPlugin } from '@capacitor/core';

import type { PreviewCameraPlugin } from './definitions';

export class PreviewCameraWeb extends WebPlugin implements PreviewCameraPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
  startPreview(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  stopPreview(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  takePhoto(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  capturePhoto(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  startRecord(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  stopRecord(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  flipCamera(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  getFlashModes(): Promise<any> {
    throw new Error('Method not implemented.');
  }
  setFlashMode(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  isTorchOn(): Promise<{ result: boolean }> {
    throw new Error('Method not implemented.');
  }
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  enableTorch(_: { enable: boolean }): Promise<void> {
    throw new Error('Method not implemented.');
  }
  isTorchAvailable(): Promise<{ result: boolean }> {
    throw new Error('Method not implemented.');
  }
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  focus(_: { x: number; y: number }): Promise<void> {
    throw new Error('Method not implemented.');
  }
  minAvailableZoom(): Promise<{ result: number }> {
    throw new Error('Method not implemented.');
  }
  maxAvailableZoom(): Promise<{ result: number }> {
    throw new Error('Method not implemented.');
  }
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  zoom(_: { factor: number }): Promise<void> {
    throw new Error('Method not implemented.');
  }
}
