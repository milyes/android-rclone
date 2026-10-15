import React, { useState, useRef, useEffect } from 'react';
import { X, Camera, Image, CloudUpload, Trash2, CameraOff, RefreshCw } from 'lucide-react';
import { CapturedPhoto } from '../types';
import { storage } from '../services/storage';

interface CameraCaptureDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onPhotoCaptured?: (photo: CapturedPhoto) => void;
}

export const CameraCaptureDialog: React.FC<CameraCaptureDialogProps> = ({
  isOpen,
  onClose,
  onPhotoCaptured
}) => {
  const [photos, setPhotos] = useState<CapturedPhoto[]>([]);
  const [photoTitle, setPhotoTitle] = useState('capture_camera_zcore');
  const [selectedPhoto, setSelectedPhoto] = useState<CapturedPhoto | null>(null);
  const [cameraPermission, setCameraPermission] = useState<boolean | null>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    if (isOpen) {
      setPhotos(storage.getPhotos());
      startCamera();
    } else {
      stopCamera();
      setSelectedPhoto(null);
    }
    return () => {
      stopCamera();
    };
  }, [isOpen]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const startCamera = async () => {
    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }
      });
      setStream(mediaStream);
      setCameraPermission(true);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
    } catch (err) {
      console.warn('Camera access denied or unavailable:', err);
      setCameraPermission(false);
    }
  };

  const stopCamera = () => {
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      setStream(null);
    }
  };

  const capturePhoto = () => {
    if (!videoRef.current || !canvasRef.current) {
      // Create a simulated snapshot if video is not streaming
      createSimulatedPhoto();
      return;
    }

    const video = videoRef.current;
    const canvas = canvasRef.current;
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
      saveNewPhoto(dataUrl);
    }
  };

  const createSimulatedPhoto = () => {
    const canvas = document.createElement('canvas');
    canvas.width = 640;
    canvas.height = 480;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.fillStyle = '#0F172A';
      ctx.fillRect(0, 0, 640, 480);
      ctx.fillStyle = '#00E5FF';
      ctx.font = '24px monospace';
      ctx.fillText('Z-CORE CAMERA CAPTURE', 180, 220);
      ctx.fillStyle = '#94A3B8';
      ctx.font = '16px monospace';
      ctx.fillText(new Date().toLocaleString(), 220, 260);
      const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
      saveNewPhoto(dataUrl);
    }
  };

  const saveNewPhoto = (dataUrl: string) => {
    const dateStr = new Date().toISOString().replace(/[:.]/g, '-');
    const name = `${photoTitle || 'IMG_ZCORE'}_${dateStr}.jpg`;
    const newPhoto: CapturedPhoto = {
      id: 'photo_' + Date.now(),
      name,
      dataUrl,
      timestamp: new Date().toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }),
      sizeBytes: Math.round(dataUrl.length * 0.75)
    };

    const updated = [newPhoto, ...photos];
    setPhotos(updated);
    storage.savePhotos(updated);
    setSelectedPhoto(newPhoto);
    onPhotoCaptured?.(newPhoto);
    showToast(`Photo enregistrée: ${newPhoto.name}`);
  };

  const syncPhotoToCloud = (photo: CapturedPhoto) => {
    showToast(`Transfert Cloud déclenché pour ${photo.name}`);
  };

  const deletePhoto = (id: string) => {
    const updated = photos.filter(p => p.id !== id);
    setPhotos(updated);
    storage.savePhotos(updated);
    if (selectedPhoto?.id === id) {
      setSelectedPhoto(null);
    }
    showToast('Photo supprimée');
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fade-in">
      <div
        data-testid="camera_capture_dialog"
        className="bg-[#1E293B] border border-slate-700 w-full max-w-xl max-h-[92vh] rounded-3xl shadow-2xl flex flex-col overflow-hidden text-slate-100"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-700/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center">
              <Camera className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-base">Caméra Z-CORE Captures</h2>
              <p className="text-xs text-slate-400">Prise de vue &amp; Instant Auto-Sync</p>
            </div>
          </div>
          <button
            onClick={onClose}
            data-testid="close_camera_dialog_btn"
            className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-700/50 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Toast */}
        {toastMessage && (
          <div className="bg-cyan-500 text-slate-900 text-xs font-bold px-4 py-2 text-center animate-fade-in">
            {toastMessage}
          </div>
        )}

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4">
          {/* Camera Viewport */}
          <div className="relative rounded-2xl overflow-hidden bg-slate-950 border border-slate-700 aspect-video flex items-center justify-center">
            {cameraPermission === false ? (
              <div className="text-center p-6 space-y-2">
                <CameraOff className="w-10 h-10 text-slate-500 mx-auto" />
                <p className="text-xs font-semibold text-slate-300">Caméra désactivée ou indisponible</p>
                <button
                  onClick={startCamera}
                  data-testid="grant_camera_permission_btn"
                  className="text-xs py-1.5 px-3 rounded-lg bg-cyan-500/20 text-[#00E5FF] border border-cyan-500/40 hover:bg-cyan-500/30 cursor-pointer"
                >
                  Réessayer l'autorisation
                </button>
              </div>
            ) : (
              <>
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  className="w-full h-full object-cover"
                />
                <div className="absolute top-2 left-2 px-2 py-0.5 rounded bg-black/60 text-[10px] font-mono text-[#00E5FF] flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                  LIVE CAMERAX
                </div>
              </>
            )}
            <canvas ref={canvasRef} className="hidden" />
          </div>

          {/* Title input & Capture button */}
          <div className="space-y-2">
            <input
              type="text"
              value={photoTitle}
              onChange={e => setPhotoTitle(e.target.value)}
              placeholder="Nom / Préfixe du cliché"
              data-testid="photo_title_input"
              className="w-full bg-slate-800/80 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-[#00E5FF]"
            />

            <button
              onClick={capturePhoto}
              data-testid="capture_photo_btn"
              className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-sm cursor-pointer transition-colors shadow-lg shadow-cyan-500/10"
            >
              <Camera className="w-4 h-4" />
              Prendre la Photo
            </button>
          </div>

          {/* Gallery */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-xs font-bold text-slate-200">
                Clichés enregistrés ({photos.length})
              </h3>
            </div>

            {photos.length === 0 ? (
              <div className="p-8 rounded-2xl bg-slate-800/30 border border-slate-800 text-center text-slate-400 text-xs">
                Aucun cliché capturé pour l'instant
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-2.5">
                {photos.map(p => (
                  <div
                    key={p.id}
                    onClick={() => setSelectedPhoto(p)}
                    data-testid={`photo_item_${p.name}`}
                    className={`aspect-square rounded-xl overflow-hidden border cursor-pointer relative group transition-all ${
                      selectedPhoto?.id === p.id ? 'border-[#00E5FF] ring-2 ring-cyan-500/30' : 'border-slate-700'
                    }`}
                  >
                    <img src={p.dataUrl} alt={p.name} className="w-full h-full object-cover" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-1.5 flex flex-col justify-end">
                      <p className="text-[9px] text-white truncate font-medium">{p.name}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Selected Photo Modal preview */}
          {selectedPhoto && (
            <div className="p-3.5 rounded-2xl bg-cyan-950/40 border border-cyan-800/40 flex items-center justify-between">
              <div className="flex items-center gap-3 truncate">
                <img src={selectedPhoto.dataUrl} alt="" className="w-10 h-10 rounded-lg object-cover border border-slate-700 shrink-0" />
                <div className="truncate">
                  <p className="text-xs font-bold text-slate-200 truncate">{selectedPhoto.name}</p>
                  <p className="text-[10px] text-slate-400 font-mono">
                    {Math.round(selectedPhoto.sizeBytes / 1024)} KB • Prêt pour Drive Sync
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-1 shrink-0">
                <button
                  onClick={() => syncPhotoToCloud(selectedPhoto)}
                  data-testid="sync_photo_btn"
                  title="Synchroniser"
                  className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700/60 cursor-pointer"
                >
                  <CloudUpload className="w-4 h-4" />
                </button>
                <button
                  onClick={() => deletePhoto(selectedPhoto.id)}
                  data-testid="delete_photo_btn"
                  title="Supprimer"
                  className="p-2 rounded-lg text-rose-400 hover:bg-slate-700/60 cursor-pointer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
