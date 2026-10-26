import React, { createContext, useContext, useEffect, useState } from 'react';

interface SplitSharedContextValue {
  splitShared: boolean;
  toggleSplitShared: () => void;
  setSplitShared: (value: boolean) => void;
  getEffectiveAmount: (amount: number, isShared: boolean) => number;
}

const SPLIT_SHARED_STORAGE_KEY = 'expense_tracker_split_shared';

const SplitSharedContext = createContext<SplitSharedContextValue | undefined>(undefined);

export function SplitSharedProvider({ children }: { children: React.ReactNode }) {
  const [splitShared, setSplitSharedState] = useState<boolean>(() => {
    try {
      const saved = localStorage.getItem(SPLIT_SHARED_STORAGE_KEY);
      if (saved !== null) {
        return saved === 'true';
      }
    } catch {
      // Ignore localStorage access errors
    }
    return false;
  });

  useEffect(() => {
    try {
      localStorage.setItem(SPLIT_SHARED_STORAGE_KEY, String(splitShared));
    } catch {
      // Ignore localStorage write errors
    }
  }, [splitShared]);

  const toggleSplitShared = () => {
    setSplitSharedState((prev) => !prev);
  };

  const setSplitShared = (val: boolean) => {
    setSplitSharedState(val);
  };

  const getEffectiveAmount = (amount: number, isShared: boolean): number => {
    if (splitShared && isShared) {
      return Number((amount / 2).toFixed(2));
    }
    return amount;
  };

  return (
    <SplitSharedContext.Provider
      value={{
        splitShared,
        toggleSplitShared,
        setSplitShared,
        getEffectiveAmount,
      }}
    >
      {children}
    </SplitSharedContext.Provider>
  );
}

export function useSplitShared(): SplitSharedContextValue {
  const ctx = useContext(SplitSharedContext);
  if (!ctx) {
    throw new Error('useSplitShared deve essere usato dentro SplitSharedProvider');
  }
  return ctx;
}
