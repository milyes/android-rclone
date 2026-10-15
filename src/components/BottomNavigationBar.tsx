import React from 'react';
import { Mic, CloudUpload, Terminal, Sparkles } from 'lucide-react';
import { NavigationTab } from '../types';

interface BottomNavigationBarProps {
  currentTab: NavigationTab;
  onTabChange: (tab: NavigationTab) => void;
}

export const BottomNavigationBar: React.FC<BottomNavigationBarProps> = ({
  currentTab,
  onTabChange
}) => {
  const tabs = [
    {
      id: 'RECORDER_VAULT' as NavigationTab,
      label: 'Recordings',
      icon: Mic,
      testTag: 'tab_recorder'
    },
    {
      id: 'CLOUD_SYNC' as NavigationTab,
      label: 'Drive Sync',
      icon: CloudUpload,
      testTag: 'tab_cloud_sync'
    },
    {
      id: 'TERMUX_MACROS' as NavigationTab,
      label: 'Macros',
      icon: Terminal,
      testTag: 'tab_macros'
    },
    {
      id: 'AI_STUDIO' as NavigationTab,
      label: 'Gemini AI',
      icon: Sparkles,
      testTag: 'tab_ai_studio'
    }
  ];

  return (
    <nav
      data-testid="bottom_navigation_bar"
      className="fixed bottom-0 left-0 right-0 z-30 bg-[#1E293B] border-t border-slate-700/60 pb-safe"
    >
      <div className="max-w-5xl mx-auto flex items-center justify-around py-2 px-4">
        {tabs.map(tab => {
          const Icon = tab.icon;
          const isSelected = currentTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              data-testid={tab.testTag}
              className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all cursor-pointer ${
                isSelected
                  ? 'text-[#00E5FF] font-semibold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <div
                className={`p-1 rounded-full transition-colors ${
                  isSelected ? 'bg-cyan-500/15' : 'bg-transparent'
                }`}
              >
                <Icon className="w-5 h-5" />
              </div>
              <span className="text-[11px] mt-0.5">{tab.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
