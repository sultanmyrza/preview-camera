import { registerPlugin } from '@capacitor/core';

import type { PreviewCameraPlugin } from './definitions';

const PreviewCamera = registerPlugin<PreviewCameraPlugin>('PreviewCamera', {
  web: () => import('./web').then(m => new m.PreviewCameraWeb()),
});

export * from './definitions';
export { PreviewCamera };
