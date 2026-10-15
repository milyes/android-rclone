import React, { useState, useEffect, useRef } from 'react';
import {
  AudioRecording,
  CommandMacro,
  RcloneRemote,
  SyncLog,
  DriveAudioFile,
  CapturedPhoto,
  NavigationTab,
  AppThemeMode,
  PlayerState
} from './types';
import { storage } from './services/storage';
import { TopAppBar } from './components/TopAppBar';
import { BottomNavigationBar } from './components/BottomNavigationBar';
import { LicenseDialog } from './components/LicenseDialog';
import { PlayStoreListingDialog } from './components/PlayStoreListingDialog';
import { ThemePickerDialog } from './components/ThemePickerDialog';
import { CameraCaptureDialog } from './components/CameraCaptureDialog';
import { LoginScreen } from './screens/LoginScreen';
import { RecordingScreen } from './screens/RecordingScreen';
import { CloudSyncScreen } from './screens/CloudSyncScreen';
import { TermuxMacroScreen } from './screens/TermuxMacroScreen';
import { AiStudioScreen } from './screens/AiStudioScreen';

export const App: React.FC = () => {
  // Auth state
  const [auth, setAuth] = useState(() => storage.getAuth());

  // App Theme
  const [themeMode, setThemeMode] = useState<AppThemeMode>(() => storage.getThemeMode());
  const [isDynamicColor, setIsDynamicColor] = useState<boolean>(() => storage.getDynamicColor());

  // Navigation tab
  const [currentTab, setCurrentTab] = useState<NavigationTab>('RECORDER_VAULT');

  // Dialog states
  const [showLicenseDialog, setShowLicenseDialog] = useState(false);
  const [showPlayStoreDialog, setShowPlayStoreDialog] = useState(false);
  const [showThemeDialog, setShowThemeDialog] = useState(false);
  const [showCameraDialog, setShowCameraDialog] = useState(false);
  const [licenseKey, setLicenseKey] = useState<string>(() => storage.getLicenseKey());

  // Core Data
  const [recordings, setRecordings] = useState<AudioRecording[]>(() => storage.getRecordings());
  const [macros, setMacros] = useState<CommandMacro[]>(() => storage.getMacros());
  const [rcloneRemotes, setRcloneRemotes] = useState<RcloneRemote[]>(() => storage.getRemotes());
  const [syncLogs, setSyncLogs] = useState<SyncLog[]>(() => storage.getSyncLogs());
  const [driveAudioFiles, setDriveAudioFiles] = useState<DriveAudioFile[]>(() => storage.getDriveFiles());
  const [isAutoSync, setIsAutoSync] = useState<boolean>(() => storage.getAutoSync());
  const [selectedFolder, setSelectedFolder] = useState<string | null>(() => storage.getSelectedFolder());

  // Audio Player State & Audio element
  const [playerState, setPlayerState] = useState<PlayerState>({
    playingRecordingId: null,
    isPlaying: false,
    currentPositionSeconds: 0,
    playbackSpeed: 1.0
  });

  const audioElementRef = useRef<HTMLAudioElement | null>(null);
  const playbackIntervalRef = useRef<number | null>(null);

  // Syncing State
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncProgress, setSyncProgress] = useState(0);
  const [activeSyncCommand, setActiveSyncCommand] = useState('');

  // Handle Theme switching in HTML body
  useEffect(() => {
    const root = document.documentElement;
    if (themeMode === 'LIGHT') {
      root.classList.add('theme-light');
      document.body.className = 'bg-[#F8FAFC] text-[#0F172A] antialiased';
    } else {
      root.classList.remove('theme-light');
      document.body.className = 'bg-[#0F172A] text-[#F8FAFC] antialiased';
    }
  }, [themeMode]);

  // Audio playback simulator / real player
  useEffect(() => {
    if (playerState.isPlaying && playerState.playingRecordingId !== null) {
      const rec = recordings.find(r => r.id === playerState.playingRecordingId);
      if (rec) {
        if (playbackIntervalRef.current) clearInterval(playbackIntervalRef.current);
        playbackIntervalRef.current = window.setInterval(() => {
          setPlayerState(prev => {
            if (!prev.isPlaying || prev.playingRecordingId !== rec.id) return prev;
            const next = prev.currentPositionSeconds + 1;
            if (next >= rec.durationSeconds) {
              return { ...prev, isPlaying: false, currentPositionSeconds: rec.durationSeconds };
            }
            return { ...prev, currentPositionSeconds: next };
          });
        }, Math.max(250, 1000 / playerState.playbackSpeed));
      }
    } else {
      if (playbackIntervalRef.current) {
        clearInterval(playbackIntervalRef.current);
        playbackIntervalRef.current = null;
      }
    }

    return () => {
      if (playbackIntervalRef.current) clearInterval(playbackIntervalRef.current);
    };
  }, [playerState.isPlaying, playerState.playingRecordingId, playerState.playbackSpeed, recordings]);

  // Player handlers
  const handlePlayPause = (recording: AudioRecording) => {
    if (playerState.playingRecordingId === recording.id) {
      if (playerState.isPlaying) {
        // Pause
        if (audioElementRef.current) audioElementRef.current.pause();
        setPlayerState(prev => ({ ...prev, isPlaying: false }));
      } else {
        // Resume
        if (recording.audioBlobUrl) {
          if (!audioElementRef.current) audioElementRef.current = new Audio();
          audioElementRef.current.src = recording.audioBlobUrl;
          audioElementRef.current.currentTime = playerState.currentPositionSeconds;
          audioElementRef.current.playbackRate = playerState.playbackSpeed;
          audioElementRef.current.play().catch(() => {});
        }
        setPlayerState(prev => ({ ...prev, isPlaying: true }));
      }
    } else {
      // Start new
      if (audioElementRef.current) audioElementRef.current.pause();
      if (recording.audioBlobUrl) {
        if (!audioElementRef.current) audioElementRef.current = new Audio();
        audioElementRef.current.src = recording.audioBlobUrl;
        audioElementRef.current.playbackRate = playerState.playbackSpeed;
        audioElementRef.current.play().catch(() => {});
      }
      setPlayerState({
        playingRecordingId: recording.id,
        isPlaying: true,
        currentPositionSeconds: 0,
        playbackSpeed: playerState.playbackSpeed
      });
    }
  };

  const handleSeek = (seconds: number, recording: AudioRecording) => {
    if (playerState.playingRecordingId === recording.id && audioElementRef.current) {
      audioElementRef.current.currentTime = seconds;
    }
    setPlayerState(prev => ({ ...prev, currentPositionSeconds: seconds }));
  };

  const handleSpeedChange = (speed: number) => {
    if (audioElementRef.current) {
      audioElementRef.current.playbackRate = speed;
    }
    setPlayerState(prev => ({ ...prev, playbackSpeed: speed }));
  };

  // Recordings updates
  const handleSaveNewRecording = (newRec: AudioRecording) => {
    const updated = [newRec, ...recordings];
    setRecordings(updated);
    storage.saveRecordings(updated);

    // If auto-sync is on, automatically sync
    if (isAutoSync) {
      setTimeout(() => {
        handleSyncRecording(newRec);
      }, 500);
    }
  };

  const handleDeleteRecording = (recording: AudioRecording) => {
    const updated = recordings.filter(r => r.id !== recording.id);
    setRecordings(updated);
    storage.saveRecordings(updated);
    if (playerState.playingRecordingId === recording.id) {
      setPlayerState({
        playingRecordingId: null,
        isPlaying: false,
        currentPositionSeconds: 0,
        playbackSpeed: playerState.playbackSpeed
      });
      if (audioElementRef.current) audioElementRef.current.pause();
    }
  };

  const handleRenameRecording = (recording: AudioRecording, newTitle: string) => {
    if (!newTitle.trim()) return;
    const updated = recordings.map(r => (r.id === recording.id ? { ...r, title: newTitle.trim() } : r));
    setRecordings(updated);
    storage.saveRecordings(updated);
  };

  const handleUpdateRecordingAi = (id: number, summary: string, transcription?: string) => {
    const updated = recordings.map(r =>
      r.id === id ? { ...r, aiSummary: summary, transcription: transcription || r.transcription } : r
    );
    setRecordings(updated);
    storage.saveRecordings(updated);
  };

  // Sync operations
  const handleSyncRecording = (recording: AudioRecording) => {
    if (isSyncing) return;
    setIsSyncing(true);
    setSyncProgress(0);

    const cmd = selectedFolder
      ? `saf-drive sync ${recording.fileName} -> ${selectedFolder}`
      : `google-drive sync ${recording.fileName} -> gdrive:/Z-CORE Captures/`;
    setActiveSyncCommand(cmd);

    // Set recording status to SYNCING
    const marking = recordings.map(r => (r.id === recording.id ? { ...r, syncStatus: 'SYNCING' as const } : r));
    setRecordings(marking);
    storage.saveRecordings(marking);

    // Initial log
    const initialLog: SyncLog = {
      id: Date.now(),
      recordingId: recording.id,
      commandExecuted: cmd,
      status: 'IN_PROGRESS',
      progressPercent: 0,
      bytesTransferred: `0 MB / ${recording.fileSizeMb} MB`,
      timestamp: Date.now()
    };
    const updatedLogs = [initialLog, ...syncLogs];
    setSyncLogs(updatedLogs);
    storage.saveSyncLogs(updatedLogs);

    // Progress increments
    let progress = 0;
    const interval = setInterval(() => {
      progress += 25;
      setSyncProgress(Math.min(100, progress));

      if (progress >= 100) {
        clearInterval(interval);
        setIsSyncing(false);

        // Update recording to CLOUD_SYNCED
        const syncedList = recordings.map(r =>
          r.id === recording.id
            ? {
                ...r,
                isSynced: true,
                syncStatus: 'CLOUD_SYNCED' as const,
                cloudPath: `gdrive:/Z-CORE/Captures/${r.fileName}`
              }
            : r
        );
        setRecordings(syncedList);
        storage.saveRecordings(syncedList);

        // Success log
        const successLog: SyncLog = {
          id: Date.now() + 1,
          recordingId: recording.id,
          commandExecuted: cmd,
          status: 'SUCCESS',
          progressPercent: 100,
          bytesTransferred: `${recording.fileSizeMb} MB`,
          timestamp: Date.now()
        };
        const finalLogs = [successLog, ...storage.getSyncLogs()];
        setSyncLogs(finalLogs);
        storage.saveSyncLogs(finalLogs);
      }
    }, 350);
  };

  const handleSyncAll = () => {
    const unsynced = recordings.filter(r => !r.isSynced && r.syncStatus !== 'CLOUD_SYNCED');
    if (unsynced.length > 0) {
      unsynced.forEach((rec, idx) => {
        setTimeout(() => {
          handleSyncRecording(rec);
        }, idx * 1600);
      });
    } else {
      handleRunCustomRcloneCommand('rclone sync ./storage/ gdrive:/Z-CORE/Captures/ --progress');
    }
  };

  const handleRunCustomRcloneCommand = (cmd: string) => {
    if (isSyncing) return;
    setIsSyncing(true);
    setSyncProgress(0);
    setActiveSyncCommand(cmd);

    let progress = 0;
    const interval = setInterval(() => {
      progress += 20;
      setSyncProgress(Math.min(100, progress));

      if (progress >= 100) {
        clearInterval(interval);
        setIsSyncing(false);

        const log: SyncLog = {
          id: Date.now(),
          recordingId: null,
          commandExecuted: cmd,
          status: 'SUCCESS',
          progressPercent: 100,
          bytesTransferred: 'Transfert terminé',
          timestamp: Date.now()
        };
        const updated = [log, ...syncLogs];
        setSyncLogs(updated);
        storage.saveSyncLogs(updated);
      }
    }, 300);
  };

  // Remotes management
  const handleAddRemote = (name: string, type: string, path: string) => {
    const newRemote: RcloneRemote = { id: Date.now(), name, type, defaultPath: path };
    const updated = [...rcloneRemotes, newRemote];
    setRcloneRemotes(updated);
    storage.saveRemotes(updated);
  };

  const handleDeleteRemote = (id: number) => {
    const updated = rcloneRemotes.filter(r => r.id !== id);
    setRcloneRemotes(updated);
    storage.saveRemotes(updated);
  };

  // Drive API Explorer management
  const handleRefreshDriveFiles = () => {
    setDriveAudioFiles(storage.getDriveFiles());
  };

  const handleDeleteDriveFile = (id: string) => {
    const updated = driveAudioFiles.filter(f => f.id !== id);
    setDriveAudioFiles(updated);
    storage.saveDriveFiles(updated);
  };

  // Macro execution
  const handleExecuteMacro = (macro: CommandMacro) => {
    handleRunCustomRcloneCommand(macro.commandText);
  };

  const handleToggleFavoriteMacro = (id: number) => {
    const updated = macros.map(m => (m.id === id ? { ...m, isFavorite: !m.isFavorite } : m));
    setMacros(updated);
    storage.saveMacros(updated);
  };

  const handleAddMacro = (
    name: string,
    command: string,
    category: 'RCLONE' | 'TERMUX' | 'ADB' | 'CLEANUP',
    description: string
  ) => {
    const newMacro: CommandMacro = {
      id: Date.now(),
      name,
      commandText: command,
      category,
      description,
      isFavorite: false,
      requiresRoot: false
    };
    const updated = [...macros, newMacro];
    setMacros(updated);
    storage.saveMacros(updated);
  };

  // Auth actions
  const handleLoginSuccess = (email: string) => {
    const nextAuth = { loggedIn: true, email };
    setAuth(nextAuth);
    storage.saveAuth(nextAuth);
  };

  const handleLogout = () => {
    const nextAuth = { loggedIn: false, email: '' };
    setAuth(nextAuth);
    storage.saveAuth(nextAuth);
  };

  // If user is not logged in, show LoginScreen
  if (!auth.loggedIn) {
    return <LoginScreen onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-[#0F172A] text-[#F8FAFC]">
      {/* Top App Bar */}
      <TopAppBar
        onLogout={handleLogout}
        onOpenLicense={() => setShowLicenseDialog(true)}
        onOpenTheme={() => setShowThemeDialog(true)}
        onOpenCamera={() => setShowCameraDialog(true)}
        onOpenPlayStore={() => setShowPlayStoreDialog(true)}
        licenseKey={licenseKey}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-5xl w-full mx-auto p-4">
        {currentTab === 'RECORDER_VAULT' && (
          <RecordingScreen
            recordings={recordings}
            playerState={playerState}
            onPlayPause={handlePlayPause}
            onSeek={handleSeek}
            onSpeedChange={handleSpeedChange}
            onSyncClick={rec => {
              handleSyncRecording(rec);
              setCurrentTab('CLOUD_SYNC');
            }}
            onAiAnalyzeClick={rec => {
              setCurrentTab('AI_STUDIO');
            }}
            onDeleteRecording={handleDeleteRecording}
            onRenameRecording={handleRenameRecording}
            onSaveNewRecording={handleSaveNewRecording}
          />
        )}

        {currentTab === 'CLOUD_SYNC' && (
          <CloudSyncScreen
            recordings={recordings}
            syncLogs={syncLogs}
            rcloneRemotes={rcloneRemotes}
            driveAudioFiles={driveAudioFiles}
            isAutoSync={isAutoSync}
            selectedFolder={selectedFolder}
            onToggleAutoSync={val => {
              setIsAutoSync(val);
              storage.saveAutoSync(val);
            }}
            onSelectFolder={val => {
              setSelectedFolder(val);
              storage.saveSelectedFolder(val);
            }}
            onSyncRecording={handleSyncRecording}
            onSyncAll={handleSyncAll}
            onRunCustomRcloneCommand={handleRunCustomRcloneCommand}
            onAddRemote={handleAddRemote}
            onDeleteRemote={handleDeleteRemote}
            onRefreshDriveFiles={handleRefreshDriveFiles}
            onDeleteDriveFile={handleDeleteDriveFile}
            isSyncing={isSyncing}
            syncProgress={syncProgress}
            activeCommand={activeSyncCommand}
          />
        )}

        {currentTab === 'TERMUX_MACROS' && (
          <TermuxMacroScreen
            macros={macros}
            onToggleFavorite={handleToggleFavoriteMacro}
            onExecuteMacro={handleExecuteMacro}
            onAddMacro={handleAddMacro}
          />
        )}

        {currentTab === 'AI_STUDIO' && (
          <AiStudioScreen
            recordings={recordings}
            onUpdateRecordingAi={handleUpdateRecordingAi}
          />
        )}
      </main>

      {/* Bottom Navigation Bar */}
      <BottomNavigationBar
        currentTab={currentTab}
        onTabChange={tab => setCurrentTab(tab)}
      />

      {/* Dialogs */}
      <LicenseDialog
        isOpen={showLicenseDialog}
        onClose={() => setShowLicenseDialog(false)}
        onLicenseChanged={key => setLicenseKey(key)}
      />

      <PlayStoreListingDialog
        isOpen={showPlayStoreDialog}
        onClose={() => setShowPlayStoreDialog(false)}
      />

      <ThemePickerDialog
        isOpen={showThemeDialog}
        currentTheme={themeMode}
        isDynamicColor={isDynamicColor}
        onSelectTheme={mode => {
          setThemeMode(mode);
          storage.saveThemeMode(mode);
        }}
        onToggleDynamicColor={val => {
          setIsDynamicColor(val);
          storage.saveDynamicColor(val);
        }}
        onClose={() => setShowThemeDialog(false)}
      />

      <CameraCaptureDialog
        isOpen={showCameraDialog}
        onClose={() => setShowCameraDialog(false)}
        onPhotoCaptured={photo => {
          if (isAutoSync) {
            handleRunCustomRcloneCommand(
              `rclone copy ./storage/pictures_vault/${photo.name} gdrive:/Z-CORE/Pictures/`
            );
          }
        }}
      />
    </div>
  );
};
export default App;
