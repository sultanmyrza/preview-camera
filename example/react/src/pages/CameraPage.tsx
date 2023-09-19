import {
  IonBackButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
  IonFabList,
  IonFooter,
  IonHeader,
  IonIcon,
  IonPage,
  IonToolbar,
  useIonToast,
  useIonViewDidEnter,
  useIonViewDidLeave,
  withIonLifeCycle,
} from '@ionic/react';
import {
  CaptureErrorResult,
  CaptureSuccessResult,
  CustomOrientation,
  PermissionStatus,
  PreviewCamera,
} from '@numbersprotocol/preview-camera';
import {
  aperture,
  apertureOutline,
  close,
  imagesOutline,
  syncOutline,
} from 'ionicons/icons';
import React, { useState } from 'react';
import CameraModeSwitcher from '../components/CameraModeSwitcher';
import SystemSettingsComponent from '../components/SystemSettings';
import './CameraPage.css';

export type PreviewCameraMode = 'photo' | 'video';

const CameraPage: React.FC = () => {
  const [allPermissionsGranted, setAllPermissionsGranted] = useState(true);

  const [cameraMode, setCameraMode] = useState<PreviewCameraMode>('photo');

  const [isRecording, setIsRecording] = useState(false);

  const captureButtonIcon = isRecording ? apertureOutline : aperture;

  const [cameraOrientation, setCameraOrientaton] = useState<CustomOrientation>(
    'portraitUp',
  );

  const [cameraQuality, setCameraQuality] = useState<'low' | 'hq'>('hq');

  const handleViewDidEnter = async () => {
    try {
      const permissions: PermissionStatus = await PreviewCamera.requestPermissions();

      const arePermissionsGranted =
        permissions.camera === 'granted' &&
        permissions.microphone === 'granted';

      setAllPermissionsGranted(arePermissionsGranted);

      if (arePermissionsGranted) {
        // Add event listeners for capture events
        PreviewCamera.addListener('captureSuccessResult', handleCaptureSuccess);
        PreviewCamera.addListener('captureErrorResult', handleCaptureError);
        PreviewCamera.addListener(
          'accelerometerOrientation',
          handleAccelerometerOrientation,
        );
        // Start the camera preview
        await PreviewCamera.startPreview();
      }
    } catch (error) {
      console.error('CameraPage: Error during handleViewDidEnter:', error);
    }
  };

  const handleViewDidLeave = async () => {
    try {
      await PreviewCamera.removeAllListeners();
      await PreviewCamera.stopPreview();
    } catch (error) {
      presentToast(`Error handleViewDidLeave: ${JSON.stringify(error)}`);
    }
  };

  useIonViewDidEnter(handleViewDidEnter);

  useIonViewDidLeave(handleViewDidLeave);

  const handlFlipCamera = async () => {
    try {
      await PreviewCamera.flipCamera();
    } catch (error) {
      presentToast(`Flip camera failed with error: ${JSON.stringify(error)}`);
    }
  };

  const handleCapture = () => {
    if (cameraMode === 'photo') takePhoto();
    if (cameraMode === 'video' && !isRecording) startRecord();
    if (cameraMode === 'video' && isRecording) stopRecord();
  };

  const takePhoto = async () => {
    try {
      await PreviewCamera.takePhoto();
    } catch (error) {
      presentToast(JSON.stringify(error));
    }
  };

  const startRecord = async () => {
    try {
      await PreviewCamera.startRecord();
      setIsRecording(true);
    } catch (error) {
      presentToast(JSON.stringify(error));
    }
  };

  const stopRecord = async () => {
    try {
      await PreviewCamera.stopRecord();
      setIsRecording(false);
    } catch (error) {
      presentToast(JSON.stringify(error));
    }
  };

  const handleCaptureSuccess = (data: CaptureSuccessResult) => {
    console.log('Capture Success:', data);
    const sizeInMB = Math.round(data.size);
    presentToast(`Capture Success: Size in bytes: ${sizeInMB}`);
  };

  const handleCaptureError = (data: CaptureErrorResult) => {
    console.error('Capture Error:', data.errorMessage);
    presentToast(`Capture Error: ${data.errorMessage}`);
  };

  const handleAccelerometerOrientation = (data: {
    orientation: CustomOrientation;
  }) => {
    setCameraOrientaton(data.orientation);
  };

  const handCameraQaulityChange = (quality: 'low' | 'hq') => {
    try {
      PreviewCamera.setQuality({ quality });
      setCameraQuality(quality);
    } catch (error: any) {
      presentToast(JSON.stringify(error));
    }
  };

  const [present] = useIonToast();

  const presentToast = (message: string) => {
    present({
      message: message,
      duration: 1500,
      position: 'top',
    });
  };

  return (
    <IonPage>
      <IonHeader className="camera-ion-header ion-no-border">
        <IonToolbar className="camera-ion-toolbar">
          <IonButtons slot="start">
            <IonBackButton text={''} icon={close}></IonBackButton>
          </IonButtons>
        </IonToolbar>
      </IonHeader>

      {allPermissionsGranted ? (
        <>
          <IonContent fullscreen className="camera-ion-content" />

          <IonFab slot="fixed" vertical="center" horizontal="end">
            <IonFabButton>{cameraQuality}</IonFabButton>

            <IonFabList side="top">
              <IonFabButton onClick={() => handCameraQaulityChange('hq')}>
                hq
              </IonFabButton>
              <IonFabButton onClick={() => handCameraQaulityChange('low')}>
                low
              </IonFabButton>
            </IonFabList>
          </IonFab>

          <IonFooter>
            <div className="camera-footer-container">
              <CameraModeSwitcher
                mode={cameraMode}
                setCameraMode={value => setCameraMode(value)}
              />
              <div className="camera-footer-buttons">
                <IonIcon
                  icon={imagesOutline}
                  className={`camera-footer-icon ${cameraOrientation}`}
                  onClick={() =>
                    presentToast('Implement based on your use case')
                  }
                />
                <IonIcon
                  icon={captureButtonIcon}
                  className={`camera-footer-icon-large ${cameraOrientation}`}
                  onClick={() => handleCapture()}
                />
                <IonIcon
                  icon={syncOutline}
                  className={`camera-footer-icon ${cameraOrientation}`}
                  onClick={() => handlFlipCamera()}
                />
              </div>
            </div>
          </IonFooter>
        </>
      ) : (
        <IonContent fullscreen className="camera-ion-content">
          <SystemSettingsComponent />
        </IonContent>
      )}
    </IonPage>
  );
};

export default withIonLifeCycle(CameraPage);
