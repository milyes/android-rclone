import React, { useState } from 'react';
import {
  Cloud,
  CloudUpload,
  AlertCircle,
  FolderOpen,
  RefreshCw,
  Terminal,
  Search,
  CheckCircle2,
  Play,
  Trash2,
  Plus,
  CloudOff,
  ExternalLink,
  X,
  ShieldCheck
} from 'lucide-react';
import { AudioRecording, RcloneRemote, SyncLog, DriveAudioFile } from '../types';
import { SyncStatusChart } from '../components/SyncStatusChart';

type SyncFilter = 'ALL' | 'UNSYNCED' | 'SYNCED' | 'SYNCING';

interface CloudSyncScreenProps {
  recordings: AudioRecording[];
  syncLogs: SyncLog[];
  rcloneRemotes: RcloneRemote[];
  driveAudioFiles: DriveAudioFile[];
  isAutoSync: boolean;
  selectedFolder: string | null;
  onToggleAutoSync: (enabled: boolean) => void;
  onSelectFolder: (folder: string | null) => void;
  onSyncRecording: (recording: AudioRecording) => void;
  onSyncAll: () => void;
  onRunCustomRcloneCommand: (cmd: string) => void;
  onAddRemote: (name: string, type: string, path: string) => void;
  onDeleteRemote: (id: number) => void;
  onRefreshDriveFiles: () => void;
  onDeleteDriveFile: (id: string) => void;
  isSyncing: boolean;
  syncProgress: number;
  activeCommand: string;
}

export const CloudSyncScreen: React.FC<CloudSyncScreenProps> = ({
  recordings,
  syncLogs,
  rcloneRemotes,
  driveAudioFiles,
  isAutoSync,
  selectedFolder,
  onToggleAutoSync,
  onSelectFolder,
  onSyncRecording,
  onSyncAll,
  onRunCustomRcloneCommand,
  onAddRemote,
  onDeleteRemote,
  onRefreshDriveFiles,
  onDeleteDriveFile,
  isSyncing,
  syncProgress,
  activeCommand
}) => {
  const [selectedFilter, setSelectedFilter] = useState<SyncFilter>('ALL');
  const [customCommand, setCustomCommand] = useState(
    'rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/'
  );
  const [driveSearchQuery, setDriveSearchQuery] = useState('');
  const [showAddRemoteModal, setShowAddRemoteModal] = useState(false);
  const [newRemoteName, setNewRemoteName] = useState('');
  const [newRemoteType, setNewRemoteType] = useState('drive');
  const [newRemotePath, setNewRemotePath] = useState('/Z-CORE/Captures/');

  // Metrics
  const totalFiles = recordings.length;
  const totalSizeMb = recordings.reduce((acc, r) => acc + r.fileSizeMb, 0);
  const syncedCount = recordings.filter(r => r.isSynced || r.syncStatus === 'CLOUD_SYNCED').length;
  const pendingCount = recordings.filter(
    r => !r.isSynced && r.syncStatus !== 'SYNCING' && r.syncStatus !== 'SYNC_FAILED'
  ).length;
  const syncingCount = recordings.filter(r => r.syncStatus === 'SYNCING').length;
  const failedCount = recordings.filter(
    r => r.syncStatus === 'SYNC_FAILED' || r.syncStatus === 'ERROR'
  ).length;

  const filteredRecordings = recordings.filter(rec => {
    switch (selectedFilter) {
      case 'ALL':
        return true;
      case 'UNSYNCED':
        return !rec.isSynced && rec.syncStatus !== 'CLOUD_SYNCED';
      case 'SYNCED':
        return rec.isSynced || rec.syncStatus === 'CLOUD_SYNCED';
      case 'SYNCING':
        return rec.syncStatus === 'SYNCING';
      default:
        return true;
    }
  });

  const filteredDriveFiles = driveAudioFiles.filter(f => {
    if (!driveSearchQuery.trim()) return true;
    return f.name.toLowerCase().includes(driveSearchQuery.toLowerCase());
  });

  const handlePickFolder = () => {
    const defaultVal = selectedFolder || 'gdrive:/Z-CORE/Captures/';
    const folder = prompt('Entrez le dossier Google Drive distant ou chemin SAF:', defaultVal);
    if (folder !== null) {
      onSelectFolder(folder.trim() || null);
    }
  };

  const handleAddRemoteSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRemoteName.trim()) return;
    onAddRemote(newRemoteName.trim(), newRemoteType.trim(), newRemotePath.trim());
    setNewRemoteName('');
    setShowAddRemoteModal(false);
  };

  return (
    <div className="space-y-4 max-w-3xl mx-auto pb-24">
      {/* 1. Cloud Sync Engine Header & Overview Banner */}
      <div
        data-testid="cloud_status_card"
        className="p-5 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-xl space-y-4"
      >
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center">
              <Cloud className="w-6 h-6" />
            </div>
            <div>
              <h2 className="font-bold text-base text-slate-100">Google Drive Sync Engine</h2>
              <p className="text-xs text-slate-400">Bridge Cloud RClone &amp; Termux • NETSECUREPRO.IA (V7 PRO)</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="hidden sm:inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-cyan-500/15 border border-cyan-500/30 text-[#00E5FF] text-[11px] font-mono font-bold">
              <ShieldCheck className="w-3 h-3 text-emerald-400" />
              NETSECUREPRO.IA
            </span>
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 text-xs font-bold">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span>CONNECTÉ</span>
            </div>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-3 gap-2.5">
          <div className="p-3 rounded-2xl bg-slate-800/70 border border-slate-700/70 text-center">
            <div className="flex items-center justify-center gap-1 text-emerald-400 text-xs font-semibold">
              <CheckCircle2 className="w-3.5 h-3.5" /> Fichiers Synchro
            </div>
            <p className="text-lg font-bold text-emerald-400 mt-1 font-mono">{syncedCount}/{totalFiles}</p>
            <p className="text-[10px] text-slate-400 font-mono">{totalSizeMb.toFixed(1)} MB</p>
          </div>

          <div className="p-3 rounded-2xl bg-slate-800/70 border border-slate-700/70 text-center">
            <div className="flex items-center justify-center gap-1 text-amber-400 text-xs font-semibold">
              <CloudUpload className="w-3.5 h-3.5" /> En Attente
            </div>
            <p className="text-lg font-bold text-amber-400 mt-1 font-mono">{pendingCount}</p>
            <p className="text-[10px] text-slate-400">Local Only</p>
          </div>

          <div className="p-3 rounded-2xl bg-slate-800/70 border border-slate-700/70 text-center">
            <div className="flex items-center justify-center gap-1 text-rose-400 text-xs font-semibold">
              <AlertCircle className="w-3.5 h-3.5" /> Erreurs
            </div>
            <p className="text-lg font-bold text-rose-400 mt-1 font-mono">{failedCount}</p>
            <p className="text-[10px] text-slate-400">À re-tester</p>
          </div>
        </div>

        {/* Drive Destination Folder */}
        <div
          onClick={handlePickFolder}
          className="p-3 rounded-xl bg-slate-800/50 border border-slate-700 hover:border-slate-600 flex items-center justify-between cursor-pointer transition-colors"
        >
          <div>
            <p className="text-xs font-bold text-slate-200">Dossier de Destination (Drive)</p>
            <p className="text-[11px] text-slate-400 font-mono truncate">
              {selectedFolder ? `SAF: ${selectedFolder}` : 'Dossier API par défaut: gdrive:/Z-CORE/Captures/'}
            </p>
          </div>
          <FolderOpen className="w-5 h-5 text-[#00E5FF]" />
        </div>

        {/* Auto Sync Switch */}
        <div className="flex items-center justify-between pt-1">
          <div>
            <p className="text-xs font-bold text-slate-200">Auto-Sync Arrière-Plan (Rclone)</p>
            <p className="text-[11px] text-slate-400">
              Upload automatique des nouveaux enregistrements vers gdrive:/Z-CORE/Captures/
            </p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer shrink-0 ml-3">
            <input
              type="checkbox"
              checked={isAutoSync}
              onChange={e => onToggleAutoSync(e.target.checked)}
              data-testid="auto_sync_switch"
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#00E5FF]" />
          </label>
        </div>

        {/* Sync Trigger button */}
        <button
          onClick={onSyncAll}
          disabled={isSyncing}
          data-testid="bg_sync_trigger_btn"
          className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-xs cursor-pointer transition-colors shadow-lg shadow-cyan-500/10 disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
          <span>
            {pendingCount > 0 ? `Synchroniser Tout (${pendingCount})` : 'Lancer Sync Arrière-Plan'}
          </span>
        </button>

        {/* Progress bar */}
        {isSyncing && (
          <div className="space-y-1.5 pt-1 animate-fade-in">
            <div className="flex items-center justify-between text-xs font-bold text-[#00E5FF]">
              <span>Transfert Rclone en cours...</span>
              <span>{syncProgress}%</span>
            </div>
            <div
              data-testid="sync_progress_bar"
              className="w-full h-2 rounded-full bg-slate-700 overflow-hidden"
            >
              <div
                style={{ width: `${syncProgress}%` }}
                className="h-full bg-[#00E5FF] transition-all duration-300 rounded-full"
              />
            </div>
            <p className="text-[10px] font-mono text-slate-400 truncate">{activeCommand}</p>
          </div>
        )}
      </div>

      {/* 2. Local Audio File Sync Dashboard Section */}
      <div
        data-testid="file_sync_dashboard_card"
        className="p-5 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-md space-y-4"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CloudUpload className="w-5 h-5 text-[#00E5FF]" />
            <h3 className="font-bold text-sm text-slate-100">Tableau des Fichiers Audio Local</h3>
          </div>
          <span className="text-xs text-slate-400 font-mono">{filteredRecordings.length} élément(s)</span>
        </div>

        {/* Filter Chips */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs">
          <button
            onClick={() => setSelectedFilter('ALL')}
            data-testid="sync_filter_chip_all"
            className={`px-3 py-1.5 rounded-full border font-medium cursor-pointer transition-colors ${
              selectedFilter === 'ALL'
                ? 'bg-cyan-500/20 border-[#00E5FF] text-[#00E5FF]'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
          >
            Tous ({totalFiles})
          </button>
          <button
            onClick={() => setSelectedFilter('UNSYNCED')}
            data-testid="sync_filter_chip_unsynced"
            className={`px-3 py-1.5 rounded-full border font-medium cursor-pointer transition-colors ${
              selectedFilter === 'UNSYNCED'
                ? 'bg-amber-500/20 border-amber-400 text-amber-300'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
          >
            Non Synchro ({pendingCount})
          </button>
          <button
            onClick={() => setSelectedFilter('SYNCED')}
            data-testid="sync_filter_chip_synced"
            className={`px-3 py-1.5 rounded-full border font-medium cursor-pointer transition-colors ${
              selectedFilter === 'SYNCED'
                ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
          >
            Drive Synchro ({syncedCount})
          </button>
          {syncingCount > 0 && (
            <button
              onClick={() => setSelectedFilter('SYNCING')}
              className={`px-3 py-1.5 rounded-full border font-medium cursor-pointer transition-colors ${
                selectedFilter === 'SYNCING'
                  ? 'bg-cyan-500/20 border-cyan-400 text-cyan-300'
                  : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
              }`}
            >
              En cours ({syncingCount})
            </button>
          )}
        </div>

        {/* Local Files List */}
        <div className="space-y-2.5">
          {filteredRecordings.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400 bg-slate-900/40 rounded-2xl border border-slate-800">
              Aucun fichier audio trouvé pour ce filtre.
            </div>
          ) : (
            filteredRecordings.map(rec => (
              <div
                key={rec.id}
                data-testid={`sync_file_item_${rec.id}`}
                className="p-3 rounded-2xl bg-slate-800/60 border border-slate-700/70 flex items-center justify-between gap-3"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${
                      rec.isSynced || rec.syncStatus === 'CLOUD_SYNCED'
                        ? 'bg-emerald-500/20 text-emerald-400'
                        : rec.syncStatus === 'SYNCING'
                        ? 'bg-cyan-500/20 text-[#00E5FF]'
                        : 'bg-amber-500/20 text-amber-400'
                    }`}
                  >
                    {rec.isSynced || rec.syncStatus === 'CLOUD_SYNCED' ? (
                      <CheckCircle2 className="w-4 h-4" />
                    ) : (
                      <CloudUpload className="w-4 h-4" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-100 truncate">{rec.title}</p>
                    <p className="text-[11px] font-mono text-slate-400 truncate">
                      {rec.fileName} • {rec.durationSeconds}s • {rec.fileSizeMb} MB
                    </p>
                  </div>
                </div>

                <div className="shrink-0">
                  {rec.isSynced || rec.syncStatus === 'CLOUD_SYNCED' ? (
                    <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-400 px-2.5 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Drive Synchro
                    </span>
                  ) : (
                    <button
                      onClick={() => onSyncRecording(rec)}
                      disabled={isSyncing}
                      className="px-3 py-1.5 rounded-lg border border-cyan-500/40 hover:bg-cyan-500/20 text-[#00E5FF] text-xs font-bold cursor-pointer transition-colors disabled:opacity-50"
                    >
                      {rec.syncStatus === 'SYNCING' ? 'En cours...' : 'Synchroniser'}
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* 3. Google Drive API REST v3 Audio Browser & Explorer */}
      <div
        data-testid="google_drive_api_card"
        className="p-5 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-md space-y-4"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center">
              <FolderOpen className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-[#00E5FF]">Google Drive API Explorer</h3>
              <p className="text-xs text-slate-400">Fichiers audio distants (Google Drive API v3)</p>
            </div>
          </div>

          <button
            onClick={onRefreshDriveFiles}
            title="Rafraîchir Drive"
            className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700 cursor-pointer transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={driveSearchQuery}
            onChange={e => setDriveSearchQuery(e.target.value)}
            placeholder="Rechercher dans Google Drive..."
            className="w-full bg-slate-800/80 border border-slate-700 rounded-xl pl-10 pr-9 py-2 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-[#00E5FF]"
          />
          {driveSearchQuery && (
            <button
              onClick={() => setDriveSearchQuery('')}
              className="absolute right-3 top-2.5 text-slate-400 hover:text-white cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Drive files list */}
        <div className="space-y-2">
          {filteredDriveFiles.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400 bg-slate-900/40 rounded-2xl border border-slate-800 space-y-2">
              <CloudOff className="w-8 h-8 text-slate-600 mx-auto" />
              <p>Aucun fichier audio trouvé sur Google Drive.</p>
            </div>
          ) : (
            filteredDriveFiles.map(file => (
              <div
                key={file.id}
                className="p-3 rounded-xl bg-slate-800/50 border border-slate-700/60 flex items-center justify-between gap-3 text-xs"
              >
                <div className="min-w-0">
                  <p className="font-semibold text-slate-200 truncate">{file.name}</p>
                  <p className="text-[11px] font-mono text-slate-400">
                    {(file.sizeBytes / (1024 * 1024)).toFixed(1)} MB • {file.durationSeconds}s
                  </p>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  {file.webViewLink && (
                    <a
                      href={file.webViewLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-2 rounded-lg text-slate-400 hover:text-[#00E5FF] hover:bg-slate-700 transition-colors"
                      title="Ouvrir dans Drive"
                    >
                      <ExternalLink className="w-4 h-4" />
                    </a>
                  )}
                  <button
                    onClick={() => onDeleteDriveFile(file.id)}
                    className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-700 transition-colors cursor-pointer"
                    title="Supprimer du Drive"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* 4. Interactive Rclone Command Console Launcher */}
      <div className="p-5 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-md space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-sm text-[#00E5FF]">Console Commandes RClone &amp; Termux</h3>
          <Terminal className="w-4 h-4 text-[#00E5FF]" />
        </div>

        <input
          type="text"
          value={customCommand}
          onChange={e => setCustomCommand(e.target.value)}
          data-testid="rclone_command_input"
          className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 font-mono text-xs text-cyan-300 focus:outline-none focus:border-[#00E5FF]"
        />

        <div className="flex items-center gap-2">
          <button
            onClick={() =>
              setCustomCommand('rclone copy ./storage/ gdrive:/Z-CORE/Captures/ --progress')
            }
            className="flex-1 py-1.5 px-2 rounded-lg border border-slate-700 hover:bg-slate-800 text-[11px] font-mono text-slate-300 cursor-pointer truncate"
          >
            rclone copy
          </button>
          <button
            onClick={() =>
              setCustomCommand('rclone sync ./storage/ gdrive:/Z-CORE/Captures/ --delete-after')
            }
            className="flex-1 py-1.5 px-2 rounded-lg border border-slate-700 hover:bg-slate-800 text-[11px] font-mono text-slate-300 cursor-pointer truncate"
          >
            rclone sync
          </button>
          <button
            onClick={() => onRunCustomRcloneCommand(customCommand)}
            disabled={isSyncing}
            data-testid="execute_rclone_btn"
            className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-xs cursor-pointer transition-colors disabled:opacity-50"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>Exécuter</span>
          </button>
        </div>
      </div>

      {/* 5. Rclone Remotes Configuration Section */}
      <div className="p-5 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-md space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-sm text-slate-200">Remotes Rclone Configurés</h3>
          <button
            onClick={() => setShowAddRemoteModal(true)}
            className="text-xs text-[#00E5FF] hover:underline flex items-center gap-1 cursor-pointer font-semibold"
          >
            <Plus className="w-3.5 h-3.5" /> Ajouter Remote
          </button>
        </div>

        <div className="space-y-2">
          {rcloneRemotes.map(rem => (
            <div
              key={rem.id}
              className="p-3 rounded-xl bg-slate-800/40 border border-slate-700/70 flex items-center justify-between text-xs"
            >
              <div>
                <span className="font-mono font-bold text-cyan-300">{rem.name}:</span>{' '}
                <span className="text-slate-400 font-mono text-[11px]">({rem.type}) {rem.defaultPath}</span>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() =>
                    onRunCustomRcloneCommand(`rclone sync ./storage/ ${rem.name}:${rem.defaultPath} --progress`)
                  }
                  className="px-2.5 py-1 rounded-lg border border-cyan-500/40 hover:bg-cyan-500/20 text-[#00E5FF] text-[11px] font-semibold cursor-pointer"
                >
                  Sync
                </button>
                <button
                  onClick={() => onDeleteRemote(rem.id)}
                  className="p-1 text-slate-400 hover:text-rose-400 cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 6. Recent Cloud Transfer Logs & Chart */}
      <div className="space-y-3">
        <h3 className="font-bold text-sm text-slate-200">Historique des Transferts Cloud</h3>
        <SyncStatusChart syncLogs={syncLogs} />

        <div className="space-y-2">
          {syncLogs.slice(0, 8).map(log => (
            <div
              key={log.id}
              className="p-3 rounded-xl bg-slate-800/40 border border-slate-700/60 flex items-center justify-between text-xs font-mono"
            >
              <div className="min-w-0 pr-2">
                <p className="text-slate-200 truncate text-[11px]">{log.commandExecuted}</p>
                <p className="text-[10px] text-slate-400 mt-0.5">
                  {new Date(log.timestamp).toLocaleTimeString()} • {log.bytesTransferred}
                </p>
              </div>
              <span
                className={`px-2 py-0.5 rounded text-[10px] font-bold shrink-0 ${
                  log.status === 'SUCCESS'
                    ? 'bg-emerald-500/20 text-emerald-400'
                    : log.status === 'FAILED'
                    ? 'bg-rose-500/20 text-rose-400'
                    : 'bg-cyan-500/20 text-[#00E5FF]'
                }`}
              >
                {log.status}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Add Remote Modal */}
      {showAddRemoteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <form
            onSubmit={handleAddRemoteSubmit}
            className="bg-[#1E293B] border border-slate-700 rounded-2xl p-5 max-w-sm w-full space-y-3"
          >
            <h4 className="font-bold text-sm text-slate-100">Ajouter un Remote Rclone</h4>
            <div>
              <label className="text-[11px] text-slate-400">Nom du remote</label>
              <input
                type="text"
                value={newRemoteName}
                onChange={e => setNewRemoteName(e.target.value)}
                placeholder="ex: mydrive"
                required
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF]"
              />
            </div>
            <div>
              <label className="text-[11px] text-slate-400">Type de stockage</label>
              <select
                value={newRemoteType}
                onChange={e => setNewRemoteType(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF]"
              >
                <option value="drive">Google Drive</option>
                <option value="s3">Amazon S3</option>
                <option value="dropbox">Dropbox</option>
                <option value="onedrive">Microsoft OneDrive</option>
              </select>
            </div>
            <div>
              <label className="text-[11px] text-slate-400">Chemin par défaut</label>
              <input
                type="text"
                value={newRemotePath}
                onChange={e => setNewRemotePath(e.target.value)}
                placeholder="ex: /Z-CORE/Captures/"
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF]"
              />
            </div>

            <div className="flex gap-2 justify-end pt-2">
              <button
                type="button"
                onClick={() => setShowAddRemoteModal(false)}
                className="px-3 py-1.5 rounded-lg border border-slate-600 text-xs text-slate-300 hover:bg-slate-700 cursor-pointer"
              >
                Annuler
              </button>
              <button
                type="submit"
                className="px-3 py-1.5 rounded-lg bg-[#00E5FF] text-slate-900 font-bold text-xs hover:bg-cyan-400 cursor-pointer"
              >
                Sauvegarder
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
