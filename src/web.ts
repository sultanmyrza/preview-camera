import { WebPlugin } from '@capacitor/core';

import type { PreviewCameraPlugin } from './definitions';

export class PreviewCameraWeb extends WebPlugin implements PreviewCameraPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
