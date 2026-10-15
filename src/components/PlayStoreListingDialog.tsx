import React, { useState } from 'react';
import { X, ShoppingBag, Copy, Share2, Star, Check } from 'lucide-react';

interface PlayStoreListingDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export const PlayStoreListingDialog: React.FC<PlayStoreListingDialogProps> = ({
  isOpen,
  onClose
}) => {
  const [copied, setCopied] = useState(false);
  const [rated, setRated] = useState(false);

  if (!isOpen) return null;

  const packageName = 'com.aistudio.audiosynchub.cbdfea';
  const storeTitle = 'Audio Sync Hub - Z-CORE Voice & Drive Sync';
  const shortDescription = 'Enregistreur vocal HD avec synchronisation auto Google Drive/rclone & macros Termux • Licence NETSECUREPRO.IA.';
  const fullDescription = `Audio Sync Hub Z-CORE est une solution complète d'enregistrement vocal et de synchronisation multi-cloud opérée sous licence officielle NETSECUREPRO.IA.

FONCTIONNALITÉS CLÉS:
• Enregistrement audio HD avec visualiseur de forme d'onde en temps réel.
• Synchronisation automatique Google Drive & rclone CLI via service Android arrière-plan.
• Gestionnaire de macros et d'intents de commandes Termux / Z_GHOST.
• Analyseur vocal IA propulsé par Google Gemini Core V7 Pro.
• Mode de sélection en masse pour les transferts cloud volumineux.
• Licence officielle: NETSECUREPRO.IA (Signature ZOUBIROU-IA-2025).

Version: 1.0.0 (V7 PRO FINAL)
Package: ${packageName}
Licence: NETSECUREPRO.IA
Cible SDK: 35 (Android 15 Ready)`;

  const handleCopy = () => {
    navigator.clipboard.writeText(`${storeTitle}\n\n${fullDescription}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: storeTitle,
        text: shortDescription,
        url: `https://play.google.com/store/apps/details?id=${packageName}`
      }).catch(() => {});
    } else {
      handleCopy();
    }
  };

  const handleRate = () => {
    setRated(true);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div
        data-testid="play_store_listing_dialog"
        className="bg-[#1E293B] border border-slate-700 w-full max-w-lg max-h-[90vh] rounded-3xl shadow-2xl flex flex-col overflow-hidden text-slate-100"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-700/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-base">Google Play Store</h2>
              <p className="text-xs text-slate-400">Fiche &amp; Statut de l'Application</p>
            </div>
          </div>
          <button
            onClick={onClose}
            data-testid="close_play_store_dialog_btn"
            className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-700/50 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4">
          <div className="p-4 rounded-2xl bg-slate-800/80 border border-slate-700/80">
            <h3 className="font-bold text-[#00E5FF] text-sm">{storeTitle}</h3>
            <p className="text-[11px] font-mono text-slate-400 mt-1">Package: {packageName}</p>
            <p className="text-xs text-slate-300 mt-2 leading-relaxed">{shortDescription}</p>
          </div>

          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 font-mono text-[11px] text-slate-400 whitespace-pre-wrap leading-relaxed max-h-36 overflow-y-auto">
            {fullDescription}
          </div>

          <a
            href={`https://play.google.com/store/apps/details?id=${packageName}`}
            target="_blank"
            rel="noopener noreferrer"
            data-testid="open_play_store_btn"
            className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-sm cursor-pointer transition-colors shadow-lg shadow-cyan-500/10"
          >
            <ShoppingBag className="w-4 h-4" />
            Ouvrir sur le Play Store
          </a>

          <div className="flex gap-2">
            <button
              onClick={handleCopy}
              data-testid="copy_listing_btn"
              className="flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl border border-slate-600 hover:bg-slate-700 text-xs font-semibold cursor-pointer text-slate-200 transition-colors"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
              {copied ? 'Copié !' : 'Copier Fiche'}
            </button>
            <button
              onClick={handleShare}
              data-testid="share_app_btn"
              className="flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl border border-slate-600 hover:bg-slate-700 text-xs font-semibold cursor-pointer text-slate-200 transition-colors"
            >
              <Share2 className="w-4 h-4" />
              Partager
            </button>
          </div>

          {/* Rating */}
          <div className="p-3 rounded-xl bg-cyan-950/30 border border-cyan-800/30 flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs">
              <Star className="w-4 h-4 text-amber-400 fill-amber-400" />
              <span className="font-semibold text-slate-200">Aimez-vous Audio Sync Hub ?</span>
            </div>
            <button
              onClick={handleRate}
              data-testid="rate_play_store_btn"
              className="text-xs font-bold text-[#00E5FF] hover:underline cursor-pointer"
            >
              {rated ? 'Merci (5★) !' : 'Noter (5★)'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
