import React from 'react';
import { LogOut, ShieldCheck, SunMoon, Camera, ShoppingBag } from 'lucide-react';

interface TopAppBarProps {
  onLogout: () => void;
  onOpenLicense: () => void;
  onOpenTheme: () => void;
  onOpenCamera: () => void;
  onOpenPlayStore: () => void;
  licenseKey?: string;
}

export const TopAppBar: React.FC<TopAppBarProps> = ({
  onLogout,
  onOpenLicense,
  onOpenTheme,
  onOpenCamera,
  onOpenPlayStore,
  licenseKey
}) => {
  return (
    <header className="sticky top-0 z-30 bg-[#1E293B]/90 backdrop-blur-md border-b border-slate-700/60 px-4 py-3">
      <div className="max-w-5xl mx-auto flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-[#00E5FF]">
            Audio Sync Hub
          </h1>
          <p className="text-xs text-slate-400">
            Z-CORE &amp; Google Drive Sync • NETSECUREPRO.IA (V7 PRO)
          </p>
        </div>

        <div className="flex items-center gap-1 sm:gap-2">
          {/* Active NETSECUREPRO.IA License Pill */}
          <button
            onClick={onOpenLicense}
            title="Licence NETSECUREPRO.IA certifiée"
            data-testid="top_bar_license_pill"
            className="hidden sm:flex items-center gap-1.5 px-3 py-1 rounded-full bg-cyan-500/15 border border-cyan-500/30 text-[#00E5FF] hover:bg-cyan-500/25 transition-all text-[11px] font-bold cursor-pointer"
          >
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            <span>{licenseKey || 'NETSECUREPRO.IA'}</span>
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
          </button>

          <button
            onClick={onLogout}
            data-testid="top_bar_logout_btn"
            title="Déconnexion"
            className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700/60 transition-colors cursor-pointer"
          >
            <LogOut className="w-5 h-5" />
          </button>

          <button
            onClick={onOpenLicense}
            data-testid="top_bar_license_btn"
            title="Licence NETSECUREPRO.IA (V7 PRO)"
            className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700/60 transition-colors cursor-pointer"
          >
            <ShieldCheck className="w-5 h-5" />
          </button>

          <button
            onClick={onOpenTheme}
            data-testid="top_bar_theme_btn"
            title="Changer le thème"
            className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700/60 transition-colors cursor-pointer"
          >
            <SunMoon className="w-5 h-5" />
          </button>

          <button
            onClick={onOpenCamera}
            data-testid="top_bar_camera_btn"
            title="Ouvrir la caméra"
            className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700/60 transition-colors cursor-pointer"
          >
            <Camera className="w-5 h-5" />
          </button>

          <button
            onClick={onOpenPlayStore}
            data-testid="top_bar_play_store_btn"
            title="Google Play Store Listing"
            className="p-2 rounded-lg text-[#00E5FF] hover:bg-slate-700/60 transition-colors cursor-pointer"
          >
            <ShoppingBag className="w-5 h-5" />
          </button>
        </div>
      </div>
    </header>
  );
};
