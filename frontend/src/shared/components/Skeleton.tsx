import type { CSSProperties } from 'react';

interface SkeletonProps {
  className?: string;
  width?: string | number;
  height?: string | number;
  borderRadius?: string;
  style?: CSSProperties;
}

export function Skeleton({
  className = '',
  width = '100%',
  height = '1rem',
  borderRadius = 'var(--radius-md)',
  style,
}: SkeletonProps) {
  return (
    <div
      className={`skeleton ${className}`.trim()}
      style={{
        width,
        height,
        borderRadius,
        ...style,
      }}
      aria-hidden="true"
    />
  );
}
