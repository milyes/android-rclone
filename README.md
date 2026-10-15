# Audio Sync Hub

Voice recorder, Google Drive & rclone sync manager, Termux command macros, and AI audio analyzer.
Rewritten from Android to a high-performance React (Vite + TypeScript + Tailwind CSS) application.

## Features

- **Capture Vocale (Recordings)**:
  - HD voice recording studio with live waveform visualizer (`AudioWaveformVisualizer`).
  - Audio playback with playback speed controls (1x, 1.25x, 1.5x, 2x), seeking bar, audio player card.
  - Recording management: Rename, delete, share, and cloud synchronization.
- **Google Drive Sync Engine**:
  - Live sync metrics (Fichiers Synchro, En Attente, Erreurs).
  - Google Drive destination folder picker (SAF simulation and default folder).
  - Auto-sync background toggling.
  - Google Drive API Explorer (REST v3) with file search and deletion.
  - Interactive Rclone CLI command console with quick presets.
  - Rclone remotes management (add, delete, sync).
  - Transfer history and `SyncStatusChart` visualization.
- **Macros de Nettoyage & Termux**:
  - Automated maintenance, transfer, and cleanup macros.
  - Target device badge: "+1 438 985-5041 (Réseau Fido - Trailer Ghost)".
  - Category filters: `ALL`, `RCLONE`, `TERMUX`, `ADB`, `CLEANUP`.
  - Copy command and execute intent simulation with real-time feedback.
  - Custom macro creation modal.
  - Official Termux download and setup guide.
- **Gemini AI Studio (GEMINI CORE V7 PRO FINAL)**:
  - Powered by NETSECUREPRO.CA (Signature Logique: `ZOUBIROU-IA-2025`).
  - Audio file selection and prompt customization.
  - Audio analysis and synthesis generation.
- **Dialogs & Tools**:
  - Official NETSECUREPRO.CA License Dialog (`NSP-CA-2025-V7-FINAL-ZOUBIROU-IA`).
  - Google Play Store Listing & Rating dialog.
  - Theme picker (Dark Z-CORE, Light M3, System, Dynamic Colors).
  - Camera capture dialog with live camera preview, photo capture, and cloud sync.
- **Authentication**:
  - Login / Register screen with email, password, and Google Sign-In options.

## Getting Started

```bash
npm install
npm run dev
```

App runs on `http://0.0.0.0:3000`.
