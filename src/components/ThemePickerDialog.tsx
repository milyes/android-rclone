import React from 'react';
import { X, SunMoon, Moon, Sun, Laptop, Palette } from 'lucide-react';
import { AppThemeMode } from '../types';

interface ThemePickerDialogProps {
  isOpen: boolean;
  currentTheme: AppThemeMode;
  isDynamicColor: boolean;
  onSelectTheme: (theme: AppThemeMode) => void;
  onToggleDynamicColor: (enabled: boolean) => void;
  onClose: () => void;
}

export const ThemePickerDialog: React.FC<ThemePickerDialogProps> = ({
  isOpen,
  currentTheme,
  isDynamicColor,
  onSelectTheme,
  onToggleDynamicColor,
  onClose
}) => {
  if (!isOpen) return null;

  const options = [
    {
      id: 'DARK' as AppThemeMode,
      title: 'Sombre Z-CORE (Recommandé)',
      subtitle: 'Interface optimisée pour enregistrement vocal & confort visuel',
      icon: Moon,
      testTag: 'theme_option_dark'
    },
    {
      id: 'LIGHT' as AppThemeMode,
      title: 'Clair (M3 Light)',
      subtitle: 'Haute visibilité pour environnements lumineux',
      icon: Sun,
      testTag: 'theme_option_light'
    },
    {
      id: 'SYSTEM' as AppThemeMode,
      title: 'Système',
      subtitle: 'S\'adapte automatiquement aux réglages système',
      icon: Laptop,
      testTag: 'theme_option_system'
    }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div
        data-testid="theme_picker_dialog"
        className="bg-[#1E293B] border border-slate-700 w-full max-w-md rounded-3xl shadow-2xl flex flex-col overflow-hidden text-slate-100"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-700/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center">
              <SunMoon className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-base">Thème &amp; Apparence</h2>
              <p className="text-xs text-slate-400">Mode Sombre &amp; Couleurs Dynamiques</p>
            </div>
          </div>
          <button
            onClick={onClose}
            data-testid="close_theme_dialog_btn"
            className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-700/50 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          <span className="text-[11px] font-bold text-[#00E5FF] tracking-wider uppercase">
            Mode d'affichage
          </span>

          <div className="space-y-2.5">
            {options.map(opt => {
              const Icon = opt.icon;
              const isSelected = currentTheme === opt.id;
              return (
                <button
                  key={opt.id}
                  onClick={() => onSelectTheme(opt.id)}
                  data-testid={opt.testTag}
                  className={`w-full flex items-center gap-3 p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-cyan-950/40 border-[#00E5FF] text-white shadow-sm'
                      : 'bg-slate-800/40 border-slate-700 hover:border-slate-600 text-slate-300'
                  }`}
                >
                  <div
                    className={`p-2 rounded-xl ${
                      isSelected ? 'bg-cyan-500/20 text-[#00E5FF]' : 'bg-slate-700/40 text-slate-400'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
                  </div>
                  <div className="flex-1">
                    <p className="text-xs font-bold leading-tight">{opt.title}</p>
                    <p className="text-[11px] text-slate-400 mt-0.5 leading-snug">{opt.subtitle}</p>
                  </div>
                  <div
                    className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                      isSelected ? 'border-[#00E5FF]' : 'border-slate-500'
                    }`}
                  >
                    {isSelected && <div className="w-2 h-2 rounded-full bg-[#00E5FF]" />}
                  </div>
                </button>
              );
            })}
          </div>

          <div className="border-t border-slate-700/60 pt-4 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <Palette className="w-5 h-5 text-[#00E5FF]" />
              <div>
                <p className="text-xs font-semibold text-slate-200">Couleurs Dynamiques</p>
                <p className="text-[10px] text-slate-400">Harmonise l'UI avec l'accent Z-CORE</p>
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={isDynamicColor}
                onChange={e => onToggleDynamicColor(e.target.checked)}
                data-testid="dynamic_color_switch"
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#00E5FF]"></div>
            </label>
          </div>

          <button
            onClick={onClose}
            data-testid="apply_theme_btn"
            className="w-full mt-2 py-3 rounded-xl bg-[#00E5FF] hover:bg-cyan-400 text-slate-900 font-bold text-xs cursor-pointer transition-colors"
          >
            Appliquer &amp; Fermer
          </button>
        </div>
      </div>
    </div>
  );
};
