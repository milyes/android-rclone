import { AudioRecording, CommandMacro, RcloneRemote, SyncLog, DriveAudioFile, CapturedPhoto, AppThemeMode } from '../types';

const INITIAL_RECORDINGS: AudioRecording[] = [
  {
    id: 1,
    title: 'Flux WAY Vocal Capture',
    fileName: 'enregistrement_ghost_vocal.wav',
    durationSeconds: 60,
    fileSizeMb: 10.4,
    localPath: './storage/enregistrement_ghost_vocal.wav',
    cloudPath: 'gdrive:/Z-CORE/Captures/enregistrement_ghost_vocal.wav',
    isSynced: false,
    syncStatus: 'LOCAL_ONLY',
    timestamp: Date.now() - 3600000,
    sourceStream: 'Flux WAY (Z-CORE)',
    transcription: "Capture vocale d'une minute issue du flux WAY. Écoutable et prêt pour transfert cloud vers gdrive:/Z-CORE/Captures/.",
    aiSummary: "Enregistrement vocal de 60s capturé en local sur l'instance Z-CORE. Prêt à être transféré via rclone vers Google Drive.",
    waveformData: '20,45,80,65,30,90,70,85,95,60,40,75,85,90,65,50,80,95,70,40'
  },
  {
    id: 2,
    title: 'Archives Vocales 2023',
    fileName: 'enregistrement2023-02-06 07-11-38.wav',
    durationSeconds: 185,
    fileSizeMb: 31.2,
    localPath: './storage/archives/enregistrement2023-02-06.wav',
    cloudPath: 'gdrive:/Archives/2023/enregistrement2023-02-06 07-11-38.wav',
    isSynced: true,
    syncStatus: 'CLOUD_SYNCED',
    timestamp: Date.now() - 86400000000,
    sourceStream: 'Drive Archive',
    transcription: 'Archive audio historique de février 2023 déjà présente sur le Google Drive principal.',
    aiSummary: 'Fichier audio archivé de 3m05s. Sauvegarde confirmée sur le stockage cloud distant.',
    waveformData: '15,30,40,50,45,60,70,65,80,75,60,50,40,30,25,35,45,55,60,50'
  }
];

const INITIAL_MACROS: CommandMacro[] = [
  {
    id: 1,
    name: 'Sync Ghost Vocal to Drive',
    category: 'RCLONE',
    commandText: 'rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/',
    description: "Copie chiffrée de l'enregistrement Z-CORE vers Google Drive.",
    isFavorite: true,
    requiresRoot: false
  },
  {
    id: 2,
    name: 'Sync Root Drive Folder',
    category: 'RCLONE',
    commandText: 'rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/',
    description: 'Envoie le fichier directement à la racine de Google Drive.',
    isFavorite: false,
    requiresRoot: false
  },
  {
    id: 3,
    name: 'Rclone Cloud Cleanup (>30 days)',
    category: 'CLEANUP',
    commandText: 'rclone delete gdrive:/Z-CORE/Captures/ --min-age 30d',
    description: 'Supprime les fichiers vieux de plus de 30 jours sur Google Drive.',
    isFavorite: true,
    requiresRoot: false
  },
  {
    id: 4,
    name: 'Rclone Empty Trash',
    category: 'CLEANUP',
    commandText: 'rclone cleanup gdrive:',
    description: "Vide la corbeille Google Drive pour libérer de l'espace cloud.",
    isFavorite: false,
    requiresRoot: false
  },
  {
    id: 5,
    name: 'Local Cache Cleanup',
    category: 'CLEANUP',
    commandText: "rm -rf ~/.cache/* && echo 'Cache vidé'",
    description: 'Supprime les fichiers temporaires locaux du terminal Termux.',
    isFavorite: false,
    requiresRoot: false
  },
  {
    id: 6,
    name: 'Setup Termux API Tools',
    category: 'TERMUX',
    commandText: 'pkg update && pkg install termux-api',
    description: "Installe les extensions Termux API pour gérer le matériel et l'affichage.",
    isFavorite: true,
    requiresRoot: false
  },
  {
    id: 7,
    name: 'Acquire Wake Lock',
    category: 'TERMUX',
    commandText: 'termux-wake-lock',
    description: "Empêche l'écran de se mettre en veille pendant les transferts.",
    isFavorite: false,
    requiresRoot: false
  },
  {
    id: 8,
    name: 'Release Wake Lock',
    category: 'TERMUX',
    commandText: 'termux-wake-unlock',
    description: "Permet à l'écran de se remettre en veille normalement.",
    isFavorite: false,
    requiresRoot: false
  },
  {
    id: 9,
    name: 'ADB Wake Screen',
    category: 'ADB',
    commandText: 'adb shell input keyevent 26',
    description: "Simule l'appui sur le bouton d'alimentation pour réveiller le téléphone.",
    isFavorite: false,
    requiresRoot: false
  },
  {
    id: 10,
    name: 'ADB Swipe Unlock',
    category: 'ADB',
    commandText: 'adb shell input swipe 500 1500 500 500 200',
    description: "Simule un glissement vers le haut pour afficher l'écran de déverrouillage.",
    isFavorite: false,
    requiresRoot: false
  }
];

const INITIAL_REMOTES: RcloneRemote[] = [
  { id: 1, name: 'gdrive', type: 'drive', defaultPath: '/Z-CORE/Captures/' },
  { id: 2, name: 's3-backup', type: 's3', defaultPath: '/vault/archives/' }
];

const INITIAL_DRIVE_FILES: DriveAudioFile[] = [
  {
    id: 'drv_1',
    name: 'enregistrement2023-02-06 07-11-38.wav',
    sizeBytes: 32715520,
    mimeType: 'audio/wav',
    webViewLink: 'https://drive.google.com/file/d/1_archive_ghost_vocal/view',
    createdTime: Date.now() - 86400000000,
    durationSeconds: 185
  },
  {
    id: 'drv_2',
    name: 'zcore_flux_way_master_take.wav',
    sizeBytes: 24500000,
    mimeType: 'audio/wav',
    webViewLink: 'https://drive.google.com/file/d/2_master_take/view',
    createdTime: Date.now() - 172800000,
    durationSeconds: 120
  }
];

const INITIAL_SYNC_LOGS: SyncLog[] = [
  {
    id: 1,
    recordingId: 2,
    commandExecuted: 'rclone copy ./storage/archives/enregistrement2023-02-06.wav gdrive:/Archives/2023/',
    status: 'SUCCESS',
    progressPercent: 100,
    bytesTransferred: '31.2 MB',
    timestamp: Date.now() - 1200000,
    errorMessage: null
  },
  {
    id: 2,
    recordingId: null,
    commandExecuted: 'rclone sync ./storage/ gdrive:/Z-CORE/Captures/ --progress',
    status: 'SUCCESS',
    progressPercent: 100,
    bytesTransferred: 'Transfert terminé',
    timestamp: Date.now() - 600000,
    errorMessage: null
  }
];

export const storage = {
  getRecordings(): AudioRecording[] {
    const raw = localStorage.getItem('ash_recordings');
    if (!raw) {
      localStorage.setItem('ash_recordings', JSON.stringify(INITIAL_RECORDINGS));
      return INITIAL_RECORDINGS;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return INITIAL_RECORDINGS;
    }
  },

  saveRecordings(recordings: AudioRecording[]): void {
    localStorage.setItem('ash_recordings', JSON.stringify(recordings));
  },

  getMacros(): CommandMacro[] {
    const raw = localStorage.getItem('ash_macros');
    if (!raw) {
      localStorage.setItem('ash_macros', JSON.stringify(INITIAL_MACROS));
      return INITIAL_MACROS;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return INITIAL_MACROS;
    }
  },

  saveMacros(macros: CommandMacro[]): void {
    localStorage.setItem('ash_macros', JSON.stringify(macros));
  },

  getRemotes(): RcloneRemote[] {
    const raw = localStorage.getItem('ash_remotes');
    if (!raw) {
      localStorage.setItem('ash_remotes', JSON.stringify(INITIAL_REMOTES));
      return INITIAL_REMOTES;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return INITIAL_REMOTES;
    }
  },

  saveRemotes(remotes: RcloneRemote[]): void {
    localStorage.setItem('ash_remotes', JSON.stringify(remotes));
  },

  getSyncLogs(): SyncLog[] {
    const raw = localStorage.getItem('ash_sync_logs');
    if (!raw) {
      localStorage.setItem('ash_sync_logs', JSON.stringify(INITIAL_SYNC_LOGS));
      return INITIAL_SYNC_LOGS;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return INITIAL_SYNC_LOGS;
    }
  },

  saveSyncLogs(logs: SyncLog[]): void {
    localStorage.setItem('ash_sync_logs', JSON.stringify(logs));
  },

  getDriveFiles(): DriveAudioFile[] {
    const raw = localStorage.getItem('ash_drive_files');
    if (!raw) {
      localStorage.setItem('ash_drive_files', JSON.stringify(INITIAL_DRIVE_FILES));
      return INITIAL_DRIVE_FILES;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return INITIAL_DRIVE_FILES;
    }
  },

  saveDriveFiles(files: DriveAudioFile[]): void {
    localStorage.setItem('ash_drive_files', JSON.stringify(files));
  },

  getPhotos(): CapturedPhoto[] {
    const raw = localStorage.getItem('ash_photos');
    if (!raw) return [];
    try {
      return JSON.parse(raw);
    } catch {
      return [];
    }
  },

  savePhotos(photos: CapturedPhoto[]): void {
    localStorage.setItem('ash_photos', JSON.stringify(photos));
  },

  getThemeMode(): AppThemeMode {
    return (localStorage.getItem('ash_theme_mode') as AppThemeMode) || 'DARK';
  },

  saveThemeMode(mode: AppThemeMode): void {
    localStorage.setItem('ash_theme_mode', mode);
  },

  getDynamicColor(): boolean {
    return localStorage.getItem('ash_dynamic_color') === 'true';
  },

  saveDynamicColor(enabled: boolean): void {
    localStorage.setItem('ash_dynamic_color', String(enabled));
  },

  getAutoSync(): boolean {
    const val = localStorage.getItem('ash_auto_sync');
    return val === null ? true : val === 'true';
  },

  saveAutoSync(enabled: boolean): void {
    localStorage.setItem('ash_auto_sync', String(enabled));
  },

  getSelectedFolder(): string | null {
    return localStorage.getItem('ash_selected_folder');
  },

  saveSelectedFolder(folder: string | null): void {
    if (folder) localStorage.setItem('ash_selected_folder', folder);
    else localStorage.removeItem('ash_selected_folder');
  },

  getAuth(): { loggedIn: boolean; email: string } {
    const raw = localStorage.getItem('ash_auth');
    if (!raw) {
      return { loggedIn: true, email: '1976ilies@gmail.com' };
    }
    try {
      return JSON.parse(raw);
    } catch {
      return { loggedIn: true, email: '1976ilies@gmail.com' };
    }
  },

  saveAuth(auth: { loggedIn: boolean; email: string }): void {
    localStorage.setItem('ash_auth', JSON.stringify(auth));
  },

  getLicenseKey(): string {
    return localStorage.getItem('ash_license_key') || 'NETSECUREPRO.IA';
  },

  saveLicenseKey(key: string): void {
    localStorage.setItem('ash_license_key', key.trim());
  },

  getLicenseInfo() {
    const key = this.getLicenseKey();
    const isNetsecurePro = key.toUpperCase().includes('NETSECUREPRO');
    return {
      key,
      holder: 'NETSECUREPRO.IA',
      secondaryHolder: 'NETSECUREPRO_IA',
      tier: 'GEMINI CORE V7 PRO FINAL',
      certificateId: 'NETSECUREPRO.IA-2025-V7-FINAL-ZOUBIROU-IA',
      signature: 'ZOUBIROU-IA-2025',
      isActive: true,
      verified: isNetsecurePro,
      statusLabel: isNetsecurePro ? 'ACTIVE & VÉRIFIÉE' : 'CLÉ PERSONNALISÉE ACTIVE',
      authorizedModules: [
        'Moteur GEMINI CORE V7 PRO FINAL (Audio & Multi-Cloud)',
        'Licence & Certification: NETSECUREPRO.IA (NETSECUREPRO_IA)',
        'Signature de Validation Logique: ZOUBIROU-IA-2025',
        'Voûte Chiffrée Z-CORE & Traitement Audio Réel',
        'Service de Sync Cloud Google Drive & Rclone Actif',
        'Exécuteur de Macros Termux & Pont ADB en Temps Réel'
      ]
    };
  }
};
