import React, { useRef, useEffect } from 'react';

interface AudioWaveformVisualizerProps {
  amplitudes?: number[];
  progressRatio?: number;
  isLive?: boolean;
  onSeekRatio?: (ratio: Float32Array | number) => void;
  barColor?: string;
  activeBarColor?: string;
  inactiveBarColor?: string;
  className?: string;
}

export const AudioWaveformVisualizer: React.FC<AudioWaveformVisualizerProps> = ({
  amplitudes,
  progressRatio = 0,
  isLive = false,
  onSeekRatio,
  barColor = '#00E5FF',
  activeBarColor = '#00B0FF',
  inactiveBarColor = '#334155',
  className = ''
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const defaultAmps = [20, 35, 50, 75, 90, 60, 40, 80, 95, 70, 40, 65, 85, 50, 30, 60, 75, 40, 20, 50];
  const displayAmps = amplitudes && amplitudes.length > 0 ? amplitudes : defaultAmps;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId: number;

    const render = () => {
      const width = canvas.width;
      const height = canvas.height;
      ctx.clearRect(0, 0, width, height);

      const barCount = displayAmps.length;
      const barGap = 4;
      const totalGaps = (barCount - 1) * barGap;
      const barWidth = Math.max(3, (width - totalGaps) / barCount);

      // Baseline
      const centerY = height / 2;
      ctx.strokeStyle = 'rgba(51, 65, 85, 0.3)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(0, centerY);
      ctx.lineTo(width, centerY);
      ctx.stroke();

      const pulse = isLive ? (1 + 0.15 * Math.sin(Date.now() / 150)) : 1;

      displayAmps.forEach((amp, idx) => {
        const isLatestLiveBar = isLive && (idx >= barCount - 4);
        const scale = isLatestLiveBar ? pulse : 1.0;
        const norm = Math.max(8, Math.min(100, amp)) / 100;
        const barHeight = Math.min(height, height * norm * scale);

        const x = idx * (barWidth + barGap);
        const y = (height - barHeight) / 2;

        const isPlayed = (idx / barCount) <= progressRatio;
        let color = inactiveBarColor;
        if (isLive) {
          color = isLatestLiveBar ? barColor : 'rgba(0, 229, 255, 0.85)';
        } else if (isPlayed) {
          color = activeBarColor;
        }

        ctx.fillStyle = color;
        // Rounded bar
        const r = barWidth / 2;
        ctx.beginPath();
        ctx.roundRect(x, y, barWidth, barHeight, [r, r, r, r]);
        ctx.fill();

        // Top peak dot for live recording
        if (isLive && isLatestLiveBar) {
          ctx.beginPath();
          ctx.arc(x + barWidth / 2, Math.max(2, y - 3), barWidth / 2, 0, Math.PI * 2);
          ctx.fillStyle = barColor;
          ctx.fill();
        }
      });

      if (isLive) {
        animId = requestAnimationFrame(render);
      }
    };

    render();

    return () => {
      if (animId) cancelAnimationFrame(animId);
    };
  }, [displayAmps, progressRatio, isLive, barColor, activeBarColor, inactiveBarColor]);

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!onSeekRatio || isLive) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const ratio = Math.max(0, Math.min(1, x / rect.width));
    onSeekRatio(ratio);
  };

  return (
    <canvas
      ref={canvasRef}
      width={400}
      height={64}
      onClick={handleCanvasClick}
      className={`w-full h-14 ${!isLive ? 'cursor-pointer' : ''} ${className}`}
    />
  );
};
