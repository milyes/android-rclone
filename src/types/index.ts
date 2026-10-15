export type SyncStatus = 'LOCAL_ONLY' | 'SYNCING' | 'CLOUD_SYNCED' | 'SYNC_FAILED' | 'ERROR';

export interface AudioRecording {
  id: number;
  title: string;
  fileName: string;
  durationSeconds: number;
  fileSizeMb: number;
  localPath: string;
  cloudPath: string;
  isSynced: boolean;
  syncStatus: SyncStatus;
  timestamp: number;
  sourceStream: string;
  transcription?: string | null;
  aiSummary?: string | null;
  waveformData: string;
  audioBlobUrl?: string;
}

export type MacroCategory = 'ALL' | 'RCLONE' | 'TERMUX' | 'ADB' | 'CLEANUP';

export interface CommandMacro {
  id: number;
  name: string;
  category: 'RCLONE' | 'TERMUX' | 'ADB' | 'CLEANUP';
  commandText: string;
  description: string;
  isFavorite: boolean;
  requiresRoot: boolean;
}

export interface RcloneRemote {
  id: number;
  name: string;
  type: string;
  defaultPath: string;
}

export type SyncLogStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';

export interface SyncLog {
  id: number;
  recordingId: number | null;
  commandExecuted: string;
  status: SyncLogStatus;
  progressPercent: number;
  bytesTransferred: string;
  timestamp: number;
  errorMessage?: string | null;
}

export interface DriveAudioFile {
  id: string;
  name: string;
  sizeBytes: number;
  mimeType: string;
  webViewLink?: string;
  createdTime: number;
  durationSeconds: number;
}

export interface CapturedPhoto {
  id: string;
  name: string;
  dataUrl: string;
  timestamp: string;
  sizeBytes: number;
}

export type NavigationTab = 'RECORDER_VAULT' | 'CLOUD_SYNC' | 'TERMUX_MACROS' | 'AI_STUDIO';

export type AppThemeMode = 'DARK' | 'LIGHT' | 'SYSTEM';

export interface LicenseInfo {
  key: string;
  holder: string;
  tier: string;
  signature: string;
  isActive: boolean;
  activatedDate: string;
}

export interface PlayerState {
  playingRecordingId: number | null;
  isPlaying: boolean;
  currentPositionSeconds: number;
  playbackSpeed: number;
}

export interface RecorderState {
  isRecording: boolean;
  isPaused: boolean;
  durationSeconds: number;
  liveAmplitudes: number[];
  recordingName: string;
}
