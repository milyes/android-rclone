import React, { useState } from 'react';
import {
  Terminal,
  Smartphone,
  Copy,
  Info,
  Star,
  Play,
  Plus,
  X,
  Check,
  CheckCircle2,
  Download,
  ShieldCheck
} from 'lucide-react';
import { CommandMacro, MacroCategory } from '../types';

interface TermuxMacroScreenProps {
  macros: CommandMacro[];
  onToggleFavorite: (id: number) => void;
  onExecuteMacro: (macro: CommandMacro) => void;
  onAddMacro: (name: string, command: string, category: 'RCLONE' | 'TERMUX' | 'ADB' | 'CLEANUP', description: string) => void;
}

export const TermuxMacroScreen: React.FC<TermuxMacroScreenProps> = ({
  macros,
  onToggleFavorite,
  onExecuteMacro,
  onAddMacro
}) => {
  const [selectedCategory, setSelectedCategory] = useState<MacroCategory>('ALL');
  const [showGuideModal, setShowGuideModal] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [copiedId, setCopiedId] = useState<number | null>(null);
  const [copiedPhone, setCopiedPhone] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // New Macro inputs
  const [newName, setNewName] = useState('');
  const [newCommand, setNewCommand] = useState('');
  const [newCategory, setNewCategory] = useState<'RCLONE' | 'TERMUX' | 'ADB' | 'CLEANUP'>('TERMUX');
  const [newDescription, setNewDescription] = useState('');

  const targetPhoneNumber = '+1 438 985-5041';
  const targetDetails = '+14389855041 (Réseau Fido - Trailer Ghost)';

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 2500);
  };

  const handleCopyCommand = (macro: CommandMacro) => {
    navigator.clipboard.writeText(macro.commandText);
    setCopiedId(macro.id);
    showToast(`Commande copiée: ${macro.name}`);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleCopyPhone = () => {
    navigator.clipboard.writeText(targetDetails);
    setCopiedPhone(true);
    showToast(`Numéro & Réseau Fido copié: ${targetPhoneNumber}`);
    setTimeout(() => setCopiedPhone(false), 2000);
  };

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newCommand.trim()) return;
    onAddMacro(newName.trim(), newCommand.trim(), newCategory, newDescription.trim());
    setNewName('');
    setNewCommand('');
    setNewDescription('');
    setShowAddModal(false);
    showToast('Macro sauvegardée');
  };

  const filteredMacros = macros.filter(m => {
    if (selectedCategory === 'ALL') return true;
    return m.category === selectedCategory;
  });

  const categories: MacroCategory[] = ['ALL', 'RCLONE', 'TERMUX', 'ADB', 'CLEANUP'];

  return (
    <div className="space-y-4 max-w-3xl mx-auto pb-24 relative">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-16 left-1/2 -translate-x-1/2 z-50 bg-[#00E5FF] text-slate-900 font-bold px-4 py-2 rounded-xl shadow-xl text-xs flex items-center gap-2 animate-fade-in">
          <CheckCircle2 className="w-4 h-4" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Header Banner & Termux Guide */}
      <div className="p-5 rounded-3xl bg-[#1E293B] border border-slate-700/80 shadow-xl space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-bold text-base text-[#00E5FF]">Macros de Nettoyage, Rclone &amp; Termux</h2>
              <span className="hidden sm:inline-flex items-center gap-1 text-[10px] font-mono font-bold text-emerald-400 bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                <ShieldCheck className="w-3 h-3 text-emerald-400" />
                NETSECUREPRO.IA
              </span>
            </div>
            <p className="text-xs text-slate-400">Automatisez la maintenance, les transferts et le nettoyage</p>
          </div>

          <button
            onClick={() => setShowGuideModal(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-600 hover:bg-slate-800 text-xs text-slate-200 cursor-pointer font-medium transition-colors"
          >
            <Info className="w-3.5 h-3.5" />
            <span>Guide Termux</span>
          </button>
        </div>

        {/* Target Phone Number Display Badge */}
        <div className="p-3.5 rounded-2xl bg-cyan-950/40 border border-cyan-800/40 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center shrink-0">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-[#00E5FF]">Terminal Cible Trailer Ghost</span>
                <span className="text-[10px] uppercase font-bold bg-[#00E5FF] text-slate-900 px-1.5 py-0.2 rounded">
                  Réseau Fido
                </span>
              </div>
              <p className="text-xs font-bold font-mono text-slate-200 mt-0.5">
                Cellulaire: {targetPhoneNumber}
              </p>
            </div>
          </div>

          <button
            onClick={handleCopyPhone}
            title="Copier le numéro"
            className="p-2 rounded-xl text-[#00E5FF] hover:bg-cyan-500/20 transition-colors cursor-pointer"
          >
            {copiedPhone ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Category Filter Chips */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs">
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            className={`px-3.5 py-1.5 rounded-full border font-medium cursor-pointer transition-colors ${
              selectedCategory === cat
                ? 'bg-cyan-500/20 border-[#00E5FF] text-[#00E5FF]'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
          >
            {cat === 'ALL' ? 'Tous' : cat}
          </button>
        ))}
      </div>

      {/* Macros List */}
      <div className="space-y-3">
        {filteredMacros.map(macro => (
          <div
            key={macro.id}
            data-testid={`macro_card_${macro.id}`}
            className="p-4 rounded-2xl bg-[#1E293B] border border-slate-700/80 shadow-md space-y-2.5"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-500/20 text-[#00E5FF] border border-cyan-500/30">
                  {macro.category}
                </span>
                <h3 className="font-bold text-sm text-slate-100">{macro.name}</h3>
              </div>

              <button
                onClick={() => onToggleFavorite(macro.id)}
                title="Favori"
                className="p-1 text-slate-400 hover:text-amber-400 cursor-pointer transition-colors"
              >
                <Star
                  className={`w-4 h-4 ${
                    macro.isFavorite ? 'text-amber-400 fill-amber-400' : ''
                  }`}
                />
              </button>
            </div>

            {macro.description && (
              <p className="text-xs text-slate-400">{macro.description}</p>
            )}

            {/* Command box with actions */}
            <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between gap-2">
              <code className="text-xs font-mono text-cyan-300 truncate flex-1">
                {macro.commandText}
              </code>

              <div className="flex items-center gap-1 shrink-0">
                <button
                  onClick={() => onExecuteMacro(macro)}
                  data-testid={`execute_macro_intent_btn_${macro.id}`}
                  title="Lancer Intent Termux"
                  className="p-1.5 rounded-lg text-[#00E5FF] hover:bg-slate-800 transition-colors cursor-pointer"
                >
                  <Play className="w-4 h-4 fill-current" />
                </button>
                <button
                  onClick={() => handleCopyCommand(macro)}
                  data-testid={`copy_macro_btn_${macro.id}`}
                  title="Copier la commande"
                  className="p-1.5 rounded-lg text-slate-400 hover:text-white transition-colors cursor-pointer"
                >
                  {copiedId === macro.id ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Floating Action Button */}
      <button
        onClick={() => setShowAddModal(true)}
        className="fixed bottom-20 right-6 w-12 h-12 rounded-full bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 shadow-xl shadow-cyan-500/25 flex items-center justify-center cursor-pointer transition-transform active:scale-90 z-20"
        title="Ajouter une Macro"
      >
        <Plus className="w-6 h-6" />
      </button>

      {/* Termux Guide Modal */}
      {showGuideModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1E293B] border border-slate-700 w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-4 text-slate-100">
            <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
              <div className="flex items-center gap-2 text-[#00E5FF]">
                <Download className="w-5 h-5" />
                <h3 className="font-bold text-base">Télécharger Termux Officiel</h3>
              </div>
              <button
                onClick={() => setShowGuideModal(false)}
                className="p-1.5 text-slate-400 hover:text-white rounded-lg cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <p className="p-3 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300 font-semibold leading-relaxed">
                Important: Ne téléchargez PAS Termux depuis Google Play Store (version obsolète depuis 2020).
              </p>

              <div className="p-3 bg-slate-900 rounded-xl border border-slate-800 space-y-2 font-mono text-[11px] text-slate-300">
                <p className="font-bold text-[#00E5FF]">Sources officielles recommandées:</p>
                <p>1. F-Droid (Recommandé):<br /><span className="text-slate-400">f-droid.org/packages/com.termux</span></p>
                <p>2. GitHub Officiel Releases:<br /><span className="text-slate-400">github.com/termux/termux-app/releases</span></p>
                <p className="pt-1 text-emerald-400">
                  Après installation, installez le paquet API avec:<br />
                  <span className="text-cyan-300 font-bold">pkg update && pkg install termux-api</span>
                </p>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setShowGuideModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-700 hover:bg-slate-600 font-semibold text-xs text-slate-100 cursor-pointer"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Macro Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
          <form
            onSubmit={handleAddSubmit}
            className="bg-[#1E293B] border border-slate-700 w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-3 text-slate-100"
          >
            <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
              <h3 className="font-bold text-sm">Add New Macro</h3>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="p-1.5 text-slate-400 hover:text-white rounded-lg cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div>
              <label className="text-xs text-slate-300 font-semibold">Macro Name</label>
              <input
                type="text"
                value={newName}
                onChange={e => setNewName(e.target.value)}
                placeholder="ex: Mon script d'archivage"
                required
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF] mt-1"
              />
            </div>

            <div>
              <label className="text-xs text-slate-300 font-semibold">Shell Command</label>
              <textarea
                value={newCommand}
                onChange={e => setNewCommand(e.target.value)}
                placeholder="ex: rclone copy ./storage/ gdrive:/"
                rows={2}
                required
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 font-mono text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF] mt-1"
              />
            </div>

            <div>
              <label className="text-xs text-slate-300 font-semibold">Description (optional)</label>
              <input
                type="text"
                value={newDescription}
                onChange={e => setNewDescription(e.target.value)}
                placeholder="Description de la commande..."
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-[#00E5FF] mt-1"
              />
            </div>

            <div>
              <label className="text-xs text-slate-300 font-semibold">Category</label>
              <div className="flex gap-2 mt-1">
                {(['TERMUX', 'ADB', 'RCLONE', 'CLEANUP'] as const).map(cat => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setNewCategory(cat)}
                    className={`px-3 py-1 rounded-lg border text-xs font-semibold cursor-pointer ${
                      newCategory === cat
                        ? 'bg-cyan-500/20 border-[#00E5FF] text-[#00E5FF]'
                        : 'bg-slate-800 border-slate-700 text-slate-400'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3">
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="px-4 py-2 rounded-xl border border-slate-600 text-xs text-slate-300 hover:bg-slate-800 cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-2 rounded-xl bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-xs cursor-pointer"
              >
                Save
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
