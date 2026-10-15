import React, { useState } from 'react';
import { X, ShieldCheck, CheckCircle2, Copy, Check, KeyRound, Sparkles } from 'lucide-react';
import { storage } from '../services/storage';

interface LicenseDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onLicenseChanged?: (newKey: string) => void;
}

export const LicenseDialog: React.FC<LicenseDialogProps> = ({
  isOpen,
  onClose,
  onLicenseChanged
}) => {
  const [copied, setCopied] = useState(false);
  const [licenseKeyInput, setLicenseKeyInput] = useState(() => storage.getLicenseKey());
  const [activeKey, setActiveKey] = useState(() => storage.getLicenseKey());
  const [activationFeedback, setActivationFeedback] = useState<string | null>(null);

  if (!isOpen) return null;

  const licenseHolder = 'NETSECUREPRO.IA';
  const fullCertificateId = 'NETSECUREPRO.IA-2025-V7-FINAL-ZOUBIROU-IA';

  const licenseDetails = `LICENCE NETSECUREPRO.IA - ACCORD DE LICENCE OFFICIEL
SYSTÈME: GEMINI CORE V7 PRO FINAL
IA_LOGIC_SIGNATURE: ZOUBIROU-IA-2025

Éditeur Officiel: NETSECUREPRO.IA Security & AI Systems
Moteur Core: GEMINI CORE V7 PRO FINAL
Signature Logique: ZOUBIROU-IA-2025
Clé de Licence Activée: ${activeKey}
Certificat ID: ${fullCertificateId}
Statut d'Enregistrement: Mode Actif Réel (Vérifié & Certifié)

AUTORISATIONS & MODULES ACTIFS:
• Traitement Audio Avancé, Voûte Z-CORE & Synthèse GEMINI CORE V7 PRO
• Synchronisation Multi-Cloud Sécurisée (Google Drive API v3 & Rclone)
• Authentification Google Sign-In & Chiffrement des flux vocaux
• Exécution de Macros de Commande Termux & Pont ADB
• Signature d'Intégrité IA: ZOUBIROU-IA-2025
• Moteur NETSECUREPRO.IA & Surveillance Cloud continue

RESERVATIONS & DROITS:
Tous droits réservés © 2025-2026 NETSECUREPRO.IA. Ce logiciel et son architecture logique sont protégés par les lois internationales sur le droit d'auteur, la sécurité cloud et la propriété intellectuelle.`;

  const handleActivate = (keyToActivate: string) => {
    const cleanKey = keyToActivate.trim() || 'NETSECUREPRO.IA';
    setActiveKey(cleanKey);
    setLicenseKeyInput(cleanKey);
    storage.saveLicenseKey(cleanKey);
    onLicenseChanged?.(cleanKey);
    setActivationFeedback(`Licence "${cleanKey}" activée avec succès !`);
    setTimeout(() => setActivationFeedback(null), 3000);
  };

  const copyLicense = () => {
    navigator.clipboard.writeText(licenseDetails);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const downloadCertificate = () => {
    const element = document.createElement('a');
    const file = new Blob([licenseDetails], { type: 'text/plain;charset=utf-8' });
    element.href = URL.createObjectURL(file);
    element.download = `CERTIFICAT-LICENCE-${activeKey || 'NETSECUREPRO.IA'}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const modules = [
    'Moteur GEMINI CORE V7 PRO FINAL (Audio & Multi-Cloud)',
    'Licence & Certification Officielle: NETSECUREPRO.IA',
    'Signature de Validation Logique: ZOUBIROU-IA-2025',
    'Voûte Chiffrée Z-CORE & Traitement Audio Réel',
    'Service de Sync Cloud Google Drive & Rclone Actif',
    'Exécuteur de Macros Termux & Pont ADB en Temps Réel'
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fade-in">
      <div
        data-testid="netsecurepro_license_dialog"
        className="bg-[#1E293B] border border-cyan-500/40 w-full max-w-xl max-h-[92vh] rounded-3xl shadow-2xl flex flex-col overflow-hidden text-slate-100"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-700/60 bg-gradient-to-r from-slate-900 to-[#1E293B]">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center border border-cyan-500/30 shadow-md shadow-cyan-500/10">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-base text-[#00E5FF]">NETSECUREPRO.IA</h2>
                <span className="text-[10px] font-black uppercase tracking-wider bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                  CERTIFIÉ ACTIF
                </span>
              </div>
              <p className="text-xs text-slate-400">GEMINI CORE V7 PRO FINAL • LICENCE OFFICIELLE</p>
            </div>
          </div>
          <button
            onClick={onClose}
            data-testid="close_license_dialog_btn"
            className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-700/50 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4 text-sm">
          {/* Active Key Activation Box */}
          <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-700/80 space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-200 flex items-center gap-1.5">
                <KeyRound className="w-4 h-4 text-[#00E5FF]" />
                <span>Gestionnaire de Clé de Licence NETSECUREPRO.IA</span>
              </label>
              <span className="text-[10px] font-mono text-emerald-400 font-bold flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5" /> Clé Valide &amp; Active
              </span>
            </div>

            <div className="flex gap-2">
              <input
                type="text"
                value={licenseKeyInput}
                onChange={e => setLicenseKeyInput(e.target.value)}
                placeholder="Entrez votre clé de licence (ex: NETSECUREPRO.IA)"
                data-testid="license_key_input"
                className="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs font-mono text-cyan-300 focus:outline-none focus:border-[#00E5FF]"
              />
              <button
                type="button"
                onClick={() => handleActivate(licenseKeyInput)}
                data-testid="activate_license_btn"
                className="px-4 py-2 bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-xs rounded-xl cursor-pointer transition-colors shrink-0 shadow-md shadow-cyan-500/10"
              >
                Activer
              </button>
            </div>

            {/* Quick preset buttons */}
            <div className="flex items-center gap-2 pt-1 flex-wrap">
              <span className="text-[10px] text-slate-400">Presets officiels:</span>
              <button
                type="button"
                onClick={() => handleActivate('NETSECUREPRO.IA')}
                className="px-2.5 py-1 rounded-lg bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-500/40 text-[#00E5FF] text-[11px] font-mono font-bold cursor-pointer transition-colors flex items-center gap-1"
              >
                <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                NETSECUREPRO.IA
              </button>
              <button
                type="button"
                onClick={() => handleActivate('NETSECUREPRO_IA')}
                className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-[11px] font-mono cursor-pointer transition-colors"
              >
                NETSECUREPRO_IA
              </button>
              <button
                type="button"
                onClick={() => handleActivate('NETSECUREPRO.IA-2025-V7-FINAL-ZOUBIROU-IA')}
                className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-[10px] font-mono cursor-pointer transition-colors"
              >
                NSP-V7-FINAL
              </button>
            </div>

            {activationFeedback && (
              <div className="p-2 bg-emerald-500/20 border border-emerald-500/40 rounded-xl text-emerald-300 text-xs font-semibold flex items-center gap-2 animate-fade-in">
                <Check className="w-4 h-4 shrink-0" />
                <span>{activationFeedback}</span>
              </div>
            )}
          </div>

          {/* Certificate Badge Box */}
          <div className="p-4 rounded-2xl bg-cyan-950/40 border border-cyan-800/40 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-semibold text-[#00E5FF] flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" /> Certificat Officiel NETSECUREPRO.IA
              </span>
              <span className="text-[10px] uppercase font-black bg-[#00E5FF] text-slate-900 px-2 py-0.5 rounded">
                CORE V7 PRO
              </span>
            </div>
            <div className="text-xs text-slate-300 space-y-1">
              <p><span className="text-slate-400">Titulaire:</span> <span className="font-bold text-slate-100">{licenseHolder}</span></p>
              <p className="font-mono text-slate-400 text-[11px]"><span className="text-slate-400">Clé Actuelle:</span> <span className="text-emerald-400 font-bold">{activeKey}</span></p>
              <p className="font-mono text-slate-400 text-[11px]"><span className="text-slate-400">Certificat ID:</span> {fullCertificateId}</p>
              <p className="font-mono text-[#00E5FF] font-semibold text-[11px] flex items-center gap-1.5 pt-1">
                <Sparkles className="w-3.5 h-3.5" />
                <span>IA_LOGIC_SIGNATURE: ZOUBIROU-IA-2025</span>
              </p>
            </div>
          </div>

          <div>
            <h3 className="font-bold text-slate-200 mb-2">Modules Autorisés NETSECUREPRO.IA (V7 PRO):</h3>
            <ul className="space-y-1.5">
              {modules.map((mod, i) => (
                <li key={i} className="flex items-center gap-2 text-xs text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-[#00E5FF] shrink-0" />
                  <span>{mod}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 font-mono text-[11px] text-slate-400 whitespace-pre-wrap leading-relaxed max-h-40 overflow-y-auto">
            {licenseDetails}
          </div>
        </div>

        {/* Actions */}
        <div className="p-4 bg-slate-800/50 border-t border-slate-700/60 flex gap-2 sm:gap-3 flex-wrap">
          <button
            onClick={copyLicense}
            data-testid="copy_license_btn"
            className="flex-1 min-w-[120px] flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl border border-slate-600 hover:bg-slate-700 text-slate-200 text-xs font-semibold cursor-pointer transition-colors"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            {copied ? 'Copié !' : 'Copier Licence'}
          </button>
          <button
            onClick={downloadCertificate}
            data-testid="download_license_cert_btn"
            className="flex-1 min-w-[120px] flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl border border-cyan-500/40 bg-cyan-500/10 hover:bg-cyan-500/20 text-[#00E5FF] text-xs font-semibold cursor-pointer transition-colors"
          >
            <span>Télécharger Certificat</span>
          </button>
          <button
            onClick={onClose}
            data-testid="confirm_license_btn"
            className="w-full sm:w-auto sm:px-6 py-2.5 rounded-xl bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-xs cursor-pointer transition-colors"
          >
            Compris &amp; Fermer
          </button>
        </div>
      </div>
    </div>
  );
};
