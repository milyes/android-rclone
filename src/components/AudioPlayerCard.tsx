import React, { useState } from 'react';
import { Play, Pause, FastForward, CloudUpload, Sparkles, Share2, ChevronDown, ChevronUp, Edit3, Trash2 } from 'lucide-react';
import { AudioRecording, PlayerState } from '../types';
import { AudioWaveformVisualizer } from './AudioWaveformVisualizer';

interface AudioPlayerCardProps {
  recording: AudioRecording;
  playerState: PlayerState;
  onPlayPause: () => void;
  onSeek: (seconds: number) => void;
  onSpeedChange: (speed: number) => void;
  onSyncClick: () => void;
  onAiAnalyzeClick: () => void;
  onDeleteClick: () => void;
  onRenameClick: (newTitle: string) => void;
  isSelectionMode?: boolean;
  isSelected?: boolean;
  onToggleSelect?: () => void;
}

export const AudioPlayerCard: React.FC<AudioPlayerCardProps> = ({
  recording,
  playerState,
  onPlayPause,
  onSeek,
  onSpeedChange,
  onSyncClick,
  onAiAnalyzeClick,
  onDeleteClick,
  onRenameClick,
  isSelectionMode = false,
  isSelected = false,
  onToggleSelect
}) => {
  const isCurrent = playerState.playingRecordingId === recording.id;
  const isPlaying = isCurrent && playerState.isPlaying;
  const currentPos = isCurrent ? playerState.currentPositionSeconds : 0;
  const progressRatio = recording.durationSeconds > 0 ? currentPos / recording.durationSeconds : 0;

  const [showDetails, setShowDetails] = useState(false);
  const [showRenameModal, setShowRenameModal] = useState(false);
  const [renameInput, setRenameInput] = useState(recording.title);

  const amplitudes = recording.waveformData
    ? recording.waveformData.split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n))
    : [];

  const formatTime = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = Math.floor(totalSeconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const handleSpeedCycle = () => {
    const speeds = [1.0, 1.25, 1.5, 2.0];
    const currentIndex = speeds.indexOf(playerState.playbackSpeed);
    const nextSpeed = speeds[(currentIndex + 1) % speeds.length];
    onSpeedChange(nextSpeed);
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: recording.title,
        text: `Enregistrement vocal Z-CORE: ${recording.title} (${recording.durationSeconds}s)`
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(`${recording.title} - ${recording.localPath}`);
      alert(`Lien audio copié: ${recording.title}`);
    }
  };

  const getStatusBadge = () => {
    switch (recording.syncStatus) {
      case 'CLOUD_SYNCED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
            GOOGLE DRIVE SYNCED
          </span>
        );
      case 'SYNCING':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-cyan-500/20 text-[#00E5FF] border border-cyan-500/30 animate-pulse">
            SYNC EN COURS...
          </span>
        );
      case 'SYNC_FAILED':
      case 'ERROR':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30">
            ERREUR SYNC
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-700/60 text-slate-400">
            Z-CORE LOCAL
          </span>
        );
    }
  };

  return (
    <div
      data-testid={`audio_player_card_${recording.id}`}
      className={`p-4 rounded-2xl border transition-all ${
        isSelected
          ? 'bg-cyan-950/30 border-[#00E5FF] ring-2 ring-cyan-500/20'
          : 'bg-[#1E293B] border-slate-700/80 shadow-md'
      }`}
    >
      {/* Header */}
      <div className="flex items-center justify-between gap-2 mb-3">
        <div className="flex items-center gap-2.5 flex-1 min-w-0">
          {isSelectionMode && (
            <input
              type="checkbox"
              checked={isSelected}
              onChange={() => onToggleSelect?.()}
              data-testid={`select_checkbox_${recording.id}`}
              className="w-4 h-4 rounded border-slate-700 text-cyan-500 focus:ring-cyan-500"
            />
          )}
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5">
              <h3 className="font-bold text-sm text-slate-100 truncate">{recording.title}</h3>
              <button
                onClick={() => {
                  setRenameInput(recording.title);
                  setShowRenameModal(true);
                }}
                data-testid={`edit_title_btn_${recording.id}`}
                className="p-1 text-slate-400 hover:text-[#00E5FF] rounded transition-colors cursor-pointer"
                title="Renommer"
              >
                <Edit3 className="w-3.5 h-3.5" />
              </button>
            </div>
            <p className="text-[11px] font-mono text-slate-400 truncate">
              {recording.fileName} • {recording.fileSizeMb} MB
            </p>
          </div>
        </div>

        <div>{getStatusBadge()}</div>
      </div>

      {/* Waveform */}
      <AudioWaveformVisualizer
        amplitudes={amplitudes}
        progressRatio={progressRatio}
        onSeekRatio={ratio => onSeek(Math.round((ratio as number) * recording.durationSeconds))}
        barColor="#00E5FF"
        activeBarColor="#00B0FF"
        inactiveBarColor="#334155"
        className="my-1"
      />

      {/* Time & Slider */}
      <div className="flex items-center gap-3 my-2 text-[11px] font-mono text-slate-400">
        <span className="text-[#00E5FF] font-semibold w-8 text-left">{formatTime(currentPos)}</span>
        <input
          type="range"
          min={0}
          max={recording.durationSeconds}
          value={currentPos}
          onChange={e => onSeek(parseInt(e.target.value, 10))}
          data-testid={`audio_slider_${recording.id}`}
          className="flex-1 accent-[#00E5FF] h-1.5 bg-slate-700 rounded-lg cursor-pointer"
        />
        <span className="w-8 text-right">{formatTime(recording.durationSeconds)}</span>
      </div>

      {/* Controls row */}
      <div className="flex items-center justify-between pt-1">
        <div className="flex items-center gap-2">
          {/* Play / Pause */}
          <button
            onClick={onPlayPause}
            data-testid={`play_pause_button_${recording.id}`}
            className="w-10 h-10 rounded-full bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 flex items-center justify-center transition-transform active:scale-95 shadow-md shadow-cyan-500/20 cursor-pointer"
          >
            {isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
          </button>

          {/* Speed Selector */}
          <button
            onClick={handleSpeedCycle}
            className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-slate-200 flex items-center gap-1 cursor-pointer transition-colors"
          >
            <FastForward className="w-3.5 h-3.5 text-[#00E5FF]" />
            <span>{playerState.playbackSpeed}x</span>
          </button>
        </div>

        <div className="flex items-center gap-1">
          {/* Sync */}
          <button
            onClick={onSyncClick}
            data-testid={`sync_button_${recording.id}`}
            title="Synchroniser avec rclone / Drive"
            className="p-2 rounded-lg text-slate-300 hover:text-[#00E5FF] hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <CloudUpload className="w-4 h-4" />
          </button>

          {/* AI Analyze */}
          <button
            onClick={onAiAnalyzeClick}
            data-testid={`ai_button_${recording.id}`}
            title="Analyse Gemini AI"
            className="p-2 rounded-lg text-cyan-400 hover:text-cyan-300 hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <Sparkles className="w-4 h-4" />
          </button>

          {/* Share */}
          <button
            onClick={handleShare}
            data-testid={`share_button_${recording.id}`}
            title="Partager"
            className="p-2 rounded-lg text-slate-300 hover:text-[#00E5FF] hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <Share2 className="w-4 h-4" />
          </button>

          {/* Expand Details */}
          <button
            onClick={() => setShowDetails(!showDetails)}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors cursor-pointer"
          >
            {showDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Expanded details */}
      {showDetails && (
        <div className="mt-3 pt-3 border-t border-slate-700/60 text-xs space-y-2 bg-slate-900/40 p-3 rounded-xl">
          <p><span className="text-[#00E5FF] font-semibold">FLUX:</span> {recording.sourceStream}</p>
          <p className="font-mono text-slate-400 text-[11px]"><span className="text-slate-300">LOCAL:</span> {recording.localPath}</p>
          <p className="font-mono text-slate-400 text-[11px]"><span className="text-slate-300">CLOUD:</span> {recording.cloudPath}</p>

          {recording.transcription && (
            <div className="mt-2 p-2 bg-slate-800/60 rounded-lg border border-slate-700/50">
              <p className="font-bold text-slate-300 text-[11px] mb-1">Transcription / Notes:</p>
              <p className="text-slate-300 text-xs leading-relaxed">{recording.transcription}</p>
            </div>
          )}

          {recording.aiSummary && (
            <div className="mt-2 p-2 bg-cyan-950/30 rounded-lg border border-cyan-800/40">
              <p className="font-bold text-[#00E5FF] text-[11px] mb-1 flex items-center gap-1">
                <Sparkles className="w-3 h-3" /> Synthèse Gemini IA:
              </p>
              <p className="text-slate-200 text-xs leading-relaxed">{recording.aiSummary}</p>
            </div>
          )}

          <div className="flex items-center justify-between pt-2 border-t border-slate-800">
            <button
              onClick={handleShare}
              data-testid={`share_text_btn_${recording.id}`}
              className="text-xs text-[#00E5FF] hover:underline flex items-center gap-1 cursor-pointer"
            >
              <Share2 className="w-3.5 h-3.5" /> Partager Fichier
            </button>
            <button
              onClick={onDeleteClick}
              className="text-xs text-rose-400 hover:text-rose-300 flex items-center gap-1 cursor-pointer"
            >
              <Trash2 className="w-3.5 h-3.5" /> Supprimer
            </button>
          </div>
        </div>
      )}

      {/* Rename Modal */}
      {showRenameModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E293B] border border-slate-700 rounded-2xl p-5 max-w-sm w-full space-y-4">
            <h4 className="font-bold text-sm text-slate-100">Renommer l'enregistrement</h4>
            <input
              type="text"
              value={renameInput}
              onChange={e => setRenameInput(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF]"
            />
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setShowRenameModal(false)}
                className="px-3 py-1.5 rounded-lg border border-slate-600 text-xs text-slate-300 hover:bg-slate-700 cursor-pointer"
              >
                Annuler
              </button>
              <button
                onClick={() => {
                  onRenameClick(renameInput);
                  setShowRenameModal(false);
                }}
                className="px-3 py-1.5 rounded-lg bg-[#00E5FF] text-slate-900 font-bold text-xs hover:bg-cyan-400 cursor-pointer"
              >
                Renommer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
