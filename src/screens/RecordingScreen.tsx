import React, { useState, useRef, useEffect } from 'react';
import { Mic, Square, Pause, Play, Trash2, Search, X, Radio, Bell, ShieldCheck } from 'lucide-react';
import { AudioRecording, PlayerState, RecorderState } from '../types';
import { AudioWaveformVisualizer } from '../components/AudioWaveformVisualizer';
import { AudioPlayerCard } from '../components/AudioPlayerCard';

interface RecordingScreenProps {
  recordings: AudioRecording[];
  playerState: PlayerState;
  onPlayPause: (recording: AudioRecording) => void;
  onSeek: (seconds: number, recording: AudioRecording) => void;
  onSpeedChange: (speed: number) => void;
  onSyncClick: (recording: AudioRecording) => void;
  onAiAnalyzeClick: (recording: AudioRecording) => void;
  onDeleteRecording: (recording: AudioRecording) => void;
  onRenameRecording: (recording: AudioRecording, newTitle: string) => void;
  onSaveNewRecording: (newRec: AudioRecording) => void;
}

export const RecordingScreen: React.FC<RecordingScreenProps> = ({
  recordings,
  playerState,
  onPlayPause,
  onSeek,
  onSpeedChange,
  onSyncClick,
  onAiAnalyzeClick,
  onDeleteRecording,
  onRenameRecording,
  onSaveNewRecording
}) => {
  const [recorderState, setRecorderState] = useState<RecorderState>({
    isRecording: false,
    isPaused: false,
    durationSeconds: 0,
    liveAmplitudes: [],
    recordingName: 'enregistrement_vocal'
  });

  const [customNameInput, setCustomNameInput] = useState('enregistrement_vocal');
  const [searchQuery, setSearchQuery] = useState('');
  const [micPermissionGranted, setMicPermissionGranted] = useState(true);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const timerIntervalRef = useRef<number | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const animationFrameRef = useRef<number | null>(null);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      stopRecordingCleanup();
    };
  }, []);

  const stopRecordingCleanup = () => {
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    if (audioContextRef.current && audioContextRef.current.state !== 'closed') {
      audioContextRef.current.close().catch(() => {});
      audioContextRef.current = null;
    }
  };

  const startLiveAudioAnalysis = (stream: MediaStream) => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      const audioCtx = new AudioCtx();
      audioContextRef.current = audioCtx;
      const source = audioCtx.createMediaStreamSource(stream);
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 64;
      source.connect(analyser);
      analyserRef.current = analyser;

      const bufferLength = analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      const updateAmplitudes = () => {
        if (!recorderState.isRecording && !analyserRef.current) return;
        analyser.getByteFrequencyData(dataArray);

        // Calculate average amplitude
        let sum = 0;
        for (let i = 0; i < bufferLength; i++) {
          sum += dataArray[i];
        }
        const avg = Math.round((sum / bufferLength) * 0.8 + 15);

        setRecorderState(prev => {
          if (!prev.isRecording || prev.isPaused) return prev;
          const nextAmps = [...prev.liveAmplitudes.slice(-19), Math.min(100, avg)];
          return { ...prev, liveAmplitudes: nextAmps };
        });

        animationFrameRef.current = requestAnimationFrame(updateAmplitudes);
      };

      animationFrameRef.current = requestAnimationFrame(updateAmplitudes);
    } catch (e) {
      console.warn('AudioContext failed, using simulated wave:', e);
    }
  };

  const handleStartRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      setMicPermissionGranted(true);

      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      mediaRecorder.ondataavailable = e => {
        if (e.data && e.data.size > 0) {
          audioChunksRef.current.push(e.data);
        }
      };

      mediaRecorder.onstop = () => {
        saveRecordedAudio();
      };

      mediaRecorder.start(250);

      setRecorderState({
        isRecording: true,
        isPaused: false,
        durationSeconds: 0,
        liveAmplitudes: [20, 25, 30, 20],
        recordingName: customNameInput.trim() || 'enregistrement_vocal'
      });

      startLiveAudioAnalysis(stream);

      // Start timer
      timerIntervalRef.current = window.setInterval(() => {
        setRecorderState(prev => {
          if (!prev.isRecording || prev.isPaused) return prev;
          return { ...prev, durationSeconds: prev.durationSeconds + 1 };
        });
      }, 1000);
    } catch (err) {
      console.warn('Microphone permission error or unavailable:', err);
      // Simulate recording if permission is not granted in preview iframe
      simulateRecording();
    }
  };

  const simulateRecording = () => {
    setRecorderState({
      isRecording: true,
      isPaused: false,
      durationSeconds: 0,
      liveAmplitudes: [20, 35, 50, 45, 60, 40],
      recordingName: customNameInput.trim() || 'enregistrement_ghost_vocal'
    });

    timerIntervalRef.current = window.setInterval(() => {
      setRecorderState(prev => {
        if (!prev.isRecording || prev.isPaused) return prev;
        const randomAmp = Math.floor(Math.random() * 60) + 30;
        const nextAmps = [...prev.liveAmplitudes.slice(-19), randomAmp];
        return {
          ...prev,
          durationSeconds: prev.durationSeconds + 1,
          liveAmplitudes: nextAmps
        };
      });
    }, 1000);
  };

  const handleTogglePause = () => {
    if (!recorderState.isRecording) return;
    if (recorderState.isPaused) {
      if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'paused') {
        mediaRecorderRef.current.resume();
      }
      setRecorderState(prev => ({ ...prev, isPaused: false }));
    } else {
      if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
        mediaRecorderRef.current.pause();
      }
      setRecorderState(prev => ({ ...prev, isPaused: true }));
    }
  };

  const handleStopAndSave = () => {
    if (!recorderState.isRecording) return;
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    } else {
      saveRecordedAudio();
    }
  };

  const handleCancelRecording = () => {
    stopRecordingCleanup();
    setRecorderState({
      isRecording: false,
      isPaused: false,
      durationSeconds: 0,
      liveAmplitudes: [],
      recordingName: 'enregistrement_vocal'
    });
  };

  const saveRecordedAudio = () => {
    const duration = Math.max(1, recorderState.durationSeconds);
    const title = recorderState.recordingName || 'Enregistrement Vocal Z-CORE';
    const timeCode = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    const fileName = `${title.toLowerCase().replace(/\s+/g, '_')}_${timeCode}.wav`;

    let blobUrl: string | undefined = undefined;
    if (audioChunksRef.current.length > 0) {
      const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
      blobUrl = URL.createObjectURL(audioBlob);
    }

    const ampsStr = recorderState.liveAmplitudes.length > 0
      ? recorderState.liveAmplitudes.join(',')
      : '20,45,80,65,30,90,70,85,95,60,40,75,85,90,65,50,80,95,70,40';

    const newRecording: AudioRecording = {
      id: Date.now(),
      title,
      fileName,
      durationSeconds: duration,
      fileSizeMb: parseFloat(((duration * 0.17) + 0.5).toFixed(1)),
      localPath: `./storage/${fileName}`,
      cloudPath: `gdrive:/Z-CORE/Captures/${fileName}`,
      isSynced: false,
      syncStatus: 'LOCAL_ONLY',
      timestamp: Date.now(),
      sourceStream: 'Flux WAY (Z-CORE Capture)',
      transcription: `Capture vocale en direct de ${duration}s enregistrée via le studio Z-CORE.`,
      aiSummary: `Enregistrement haute définition de ${duration}s prêt pour analyse Gemini AI et transfert Google Drive.`,
      waveformData: ampsStr,
      audioBlobUrl: blobUrl
    };

    onSaveNewRecording(newRecording);
    stopRecordingCleanup();
    setRecorderState({
      isRecording: false,
      isPaused: false,
      durationSeconds: 0,
      liveAmplitudes: [],
      recordingName: 'enregistrement_vocal'
    });
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const filteredRecordings = recordings.filter(r => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const dateStr = new Date(r.timestamp).toLocaleDateString('fr-FR');
    return (
      r.title.toLowerCase().includes(q) ||
      r.fileName.toLowerCase().includes(q) ||
      r.localPath.toLowerCase().includes(q) ||
      dateStr.includes(q)
    );
  });

  return (
    <div className="space-y-4 max-w-3xl mx-auto pb-20">
      {/* Mic Permission Banner */}
      {!micPermissionGranted && (
        <div className="p-3 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-between text-xs text-amber-300">
          <span>Permission microphone recommandée pour la capture audio réelle</span>
          <button
            onClick={handleStartRecording}
            data-testid="request_mic_permission_btn"
            className="px-3 py-1 bg-amber-500 text-slate-900 font-bold rounded-lg cursor-pointer"
          >
            Autoriser
          </button>
        </div>
      )}

      {/* Live Recording Card */}
      <div
        data-testid="recording_studio_card"
        className="p-6 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-xl flex flex-col items-center text-center relative overflow-hidden"
      >
        <div className="flex items-center gap-2">
          <span className="text-xs font-black tracking-wider uppercase text-[#00E5FF]">
            {recorderState.isRecording ? 'ENREGISTREMENT EN COURS' : 'STUDIO DE CAPTURE'}
          </span>
          <span className="text-[10px] font-mono font-bold text-emerald-400 bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 rounded-full flex items-center gap-1">
            <ShieldCheck className="w-3 h-3 text-emerald-400" />
            NETSECUREPRO.IA
          </span>
        </div>

        {recorderState.isRecording && (
          <div className="mt-2 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-cyan-500/15 text-[#00E5FF] text-[11px] font-semibold border border-cyan-500/30">
            <Bell className="w-3.5 h-3.5 animate-bounce" />
            <span>Service d'arrière-plan actif • Notification continue</span>
          </div>
        )}

        {/* Timer Display */}
        <div className="flex items-center justify-center gap-3 my-4">
          {recorderState.isRecording && (
            <span className="w-3.5 h-3.5 rounded-full bg-rose-500 animate-pulse" />
          )}
          <span
            data-testid="recording_timer_text"
            className={`text-4xl sm:text-5xl font-mono font-bold tracking-wider ${
              recorderState.isRecording ? 'text-rose-400' : 'text-slate-100'
            }`}
          >
            {formatTime(recorderState.durationSeconds)}
          </span>
        </div>

        {/* Live Audio Waveform Visualizer */}
        <div className="w-full my-2">
          <AudioWaveformVisualizer
            amplitudes={recorderState.liveAmplitudes}
            isLive={recorderState.isRecording}
            barColor="#00E5FF"
          />
        </div>

        {/* Custom Title Input when idle */}
        {!recorderState.isRecording && (
          <div className="w-full max-w-md my-3">
            <div className="relative">
              <Mic className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
              <input
                type="text"
                value={customNameInput}
                onChange={e => setCustomNameInput(e.target.value)}
                placeholder="Titre de l'enregistrement"
                data-testid="recording_title_input"
                className="w-full bg-slate-800/80 border border-slate-700 rounded-xl pl-10 pr-4 py-2.5 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-[#00E5FF]"
              />
            </div>
          </div>
        )}

        {/* Main Recording Toggle Button */}
        <button
          onClick={() => {
            if (recorderState.isRecording) {
              handleStopAndSave();
            } else {
              handleStartRecording();
            }
          }}
          data-testid="toggle_recording_button"
          className={`mt-2 flex items-center justify-center gap-3 w-full max-w-sm py-4 px-6 rounded-full font-bold text-base transition-all shadow-xl active:scale-95 cursor-pointer ${
            recorderState.isRecording
              ? 'bg-rose-500 hover:bg-rose-600 text-white shadow-rose-500/20'
              : 'bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 shadow-cyan-500/20'
          }`}
        >
          {recorderState.isRecording ? (
            <>
              <Square className="w-5 h-5 fill-current" />
              <span>Arrêter et Sauvegarder</span>
            </>
          ) : (
            <>
              <Mic className="w-5 h-5" />
              <span>Démarrer l'enregistrement</span>
            </>
          )}
        </button>

        {/* Action Controls during Recording */}
        {recorderState.isRecording && (
          <div className="flex items-center gap-4 mt-4">
            <button
              onClick={handleTogglePause}
              data-testid="pause_resume_recording_button"
              title={recorderState.isPaused ? 'Reprendre' : 'Mettre en pause'}
              className="w-12 h-12 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 flex items-center justify-center cursor-pointer transition-colors"
            >
              {recorderState.isPaused ? <Play className="w-5 h-5 fill-current" /> : <Pause className="w-5 h-5" />}
            </button>
            <button
              onClick={handleCancelRecording}
              data-testid="cancel_recording_button"
              title="Annuler l'enregistrement"
              className="w-12 h-12 rounded-full bg-rose-500/20 hover:bg-rose-500/30 text-rose-400 border border-rose-500/40 flex items-center justify-center cursor-pointer transition-colors"
            >
              <Trash2 className="w-5 h-5" />
            </button>
          </div>
        )}
      </div>

      {/* Recent Audio Snippets Header & Search */}
      <div className="flex items-center justify-between pt-2">
        <h2 className="text-sm font-bold text-slate-200">Enregistrements Récents</h2>
        <span className="text-xs text-slate-400 font-mono">
          {filteredRecordings.length} fichier(s)
        </span>
      </div>

      {/* Search Input */}
      <div className="relative">
        <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
        <input
          type="text"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="Rechercher par nom ou horodatage (ex: .wav, 2026, 07-24)..."
          data-testid="search_recordings_input_history"
          className="w-full bg-[#1E293B] border border-slate-700/80 rounded-2xl pl-10 pr-9 py-2 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-[#00E5FF]"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-2.5 text-slate-400 hover:text-white cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Recordings List */}
      <div className="space-y-3">
        {filteredRecordings.length === 0 ? (
          <div className="p-8 rounded-2xl bg-[#1E293B]/60 border border-slate-800 text-center text-slate-400 text-xs">
            Aucun enregistrement ne correspond à votre recherche.
          </div>
        ) : (
          filteredRecordings.map(rec => (
            <AudioPlayerCard
              key={rec.id}
              recording={rec}
              playerState={playerState}
              onPlayPause={() => onPlayPause(rec)}
              onSeek={secs => onSeek(secs, rec)}
              onSpeedChange={onSpeedChange}
              onSyncClick={() => onSyncClick(rec)}
              onAiAnalyzeClick={() => onAiAnalyzeClick(rec)}
              onDeleteClick={() => onDeleteRecording(rec)}
              onRenameClick={newTitle => onRenameRecording(rec, newTitle)}
            />
          ))
        )}
      </div>
    </div>
  );
};
