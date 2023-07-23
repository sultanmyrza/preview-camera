import {
  IonContent,
  IonFab,
  IonFabButton,
  IonHeader,
  IonIcon,
  IonPage,
  IonTitle,
  IonToolbar,
} from '@ionic/react';
import { camera } from 'ionicons/icons';
import './Home.css';

const Home: React.FC = () => {
  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Preview Camera</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent fullscreen>
        <IonHeader collapse="condense">
          <IonToolbar>
            <IonTitle size="large">Preview Camera</IonTitle>
          </IonToolbar>
        </IonHeader>
      </IonContent>
      <IonFab slot="fixed" horizontal="end" vertical="bottom">
        <IonFabButton routerLink="/camera">
          <IonIcon icon={camera}></IonIcon>
        </IonFabButton>
      </IonFab>
    </IonPage>
  );
};

export default Home;
