import './SystemSettings.css';

const SystemSettingsComponent: React.FC = () => {
  return (
    <div className="system-settings">
      <h2>
        Camera and microphone permissions are required. Go to settings and
        enable camera, microphone permissions.
      </h2>
    </div>
  );
};

export default SystemSettingsComponent;
