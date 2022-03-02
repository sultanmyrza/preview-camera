export interface PreviewCameraPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
