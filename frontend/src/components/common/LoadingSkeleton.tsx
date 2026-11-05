import React from 'react';
import clsx from 'clsx';

interface LoadingSkeletonProps {
  className?: string;
  rows?: number;
}

export const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({ className, rows = 1 }) => {
  return (
    <div className="space-y-3 animate-pulse">
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className={clsx('bg-slate-800/60 rounded-xl h-6 w-full', className)}
        />
      ))}
    </div>
  );
};
