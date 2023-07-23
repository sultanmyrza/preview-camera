import { IonChip } from '@ionic/react';
import { PreviewCameraMode } from '../pages/CameraPage';

import './CameraModeSwitcher.css';

interface CameraModeSwitcherProps {
  mode: PreviewCameraMode;
  setCameraMode: (mode: PreviewCameraMode) => void;
}

const CameraModeSwitcher: React.FC<CameraModeSwitcherProps> = ({
  mode,
  setCameraMode,
}) => {
  const cameraSwitcherClassName =
    mode === 'photo' ? 'center-photo-mode' : 'center-video-mode';
  const photoChipClassName = mode === 'photo' ? '' : 'unselected-chip';
  const videoChipClassName = mode === 'video' ? '' : 'unselected-chip';

  return (
    <div className={`camera-mode-switcher ${cameraSwitcherClassName}`}>
      <IonChip
        className={photoChipClassName}
        onClick={() => setCameraMode('photo')}
      >
        Photo
      </IonChip>
      <IonChip
        className={videoChipClassName}
        onClick={() => setCameraMode('video')}
      >
        Video
      </IonChip>
    </div>
  );
};

export default CameraModeSwitcher;
