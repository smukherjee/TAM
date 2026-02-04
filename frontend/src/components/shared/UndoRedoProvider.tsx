/**
 * UndoRedoProvider
 * Context provider for undo/redo functionality with Ctrl+Z/Y support.
 * Implements FR-037: Undo/Redo for path and zone editing.
 */

import React, {
  createContext,
  useContext,
  useCallback,
  useEffect,
  useRef,
  useState,
  ReactNode,
} from 'react';

// Maximum history size
const MAX_HISTORY_SIZE = 50;

export interface UndoRedoState<T> {
  /** Current state value */
  current: T;
  /** Whether undo is available */
  canUndo: boolean;
  /** Whether redo is available */
  canRedo: boolean;
  /** Number of undo steps available */
  undoCount: number;
  /** Number of redo steps available */
  redoCount: number;
}

export interface UndoRedoActions<T> {
  /** Update state and push to history */
  push: (state: T) => void;
  /** Undo last action */
  undo: () => T | undefined;
  /** Redo last undone action */
  redo: () => T | undefined;
  /** Reset history with initial state */
  reset: (initialState: T) => void;
  /** Clear all history */
  clear: () => void;
  /** Get current state */
  getCurrent: () => T;
}

export interface UndoRedoContextValue<T = unknown> {
  state: UndoRedoState<T>;
  actions: UndoRedoActions<T>;
}

// Create context with default undefined
const UndoRedoContext = createContext<UndoRedoContextValue | undefined>(undefined);

export interface UndoRedoProviderProps<T> {
  /** Initial state value */
  initialState: T;
  /** Callback when state changes */
  onChange?: (state: T, action: 'push' | 'undo' | 'redo' | 'reset') => void;
  /** Enable keyboard shortcuts */
  enableKeyboard?: boolean;
  /** Children components */
  children: ReactNode;
}

export function UndoRedoProvider<T>({
  initialState,
  onChange,
  enableKeyboard = true,
  children,
}: UndoRedoProviderProps<T>) {
  // History stacks
  const pastRef = useRef<T[]>([]);
  const futureRef = useRef<T[]>([]);
  const currentRef = useRef<T>(initialState);

  // Force re-render state
  const [, forceUpdate] = useState(0);
  const triggerUpdate = useCallback(() => forceUpdate((n) => n + 1), []);

  // Get current state
  const getCurrent = useCallback((): T => {
    return currentRef.current;
  }, []);

  // Push new state to history
  const push = useCallback(
    (newState: T) => {
      // Don't push if state is identical
      if (JSON.stringify(newState) === JSON.stringify(currentRef.current)) {
        return;
      }

      // Add current to past
      pastRef.current = [...pastRef.current, currentRef.current].slice(-MAX_HISTORY_SIZE);

      // Clear future on new action
      futureRef.current = [];

      // Update current
      currentRef.current = newState;

      triggerUpdate();
      onChange?.(newState, 'push');
    },
    [onChange, triggerUpdate]
  );

  // Undo last action
  const undo = useCallback((): T | undefined => {
    if (pastRef.current.length === 0) {
      return undefined;
    }

    // Pop from past
    const previous = pastRef.current[pastRef.current.length - 1];
    pastRef.current = pastRef.current.slice(0, -1);

    // Push current to future
    futureRef.current = [currentRef.current, ...futureRef.current].slice(0, MAX_HISTORY_SIZE);

    // Update current
    currentRef.current = previous;

    triggerUpdate();
    onChange?.(previous, 'undo');
    return previous;
  }, [onChange, triggerUpdate]);

  // Redo last undone action
  const redo = useCallback((): T | undefined => {
    if (futureRef.current.length === 0) {
      return undefined;
    }

    // Pop from future
    const next = futureRef.current[0];
    futureRef.current = futureRef.current.slice(1);

    // Push current to past
    pastRef.current = [...pastRef.current, currentRef.current].slice(-MAX_HISTORY_SIZE);

    // Update current
    currentRef.current = next;

    triggerUpdate();
    onChange?.(next, 'redo');
    return next;
  }, [onChange, triggerUpdate]);

  // Reset with new initial state
  const reset = useCallback(
    (newInitialState: T) => {
      pastRef.current = [];
      futureRef.current = [];
      currentRef.current = newInitialState;

      triggerUpdate();
      onChange?.(newInitialState, 'reset');
    },
    [onChange, triggerUpdate]
  );

  // Clear all history
  const clear = useCallback(() => {
    pastRef.current = [];
    futureRef.current = [];
    triggerUpdate();
  }, [triggerUpdate]);

  // Keyboard shortcut handler
  useEffect(() => {
    if (!enableKeyboard) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Check for Ctrl+Z (undo) or Ctrl+Shift+Z / Ctrl+Y (redo)
      const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
      const ctrlKey = isMac ? e.metaKey : e.ctrlKey;

      if (!ctrlKey) return;

      // Ignore if in input/textarea
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement
      ) {
        return;
      }

      if (e.key === 'z' || e.key === 'Z') {
        if (e.shiftKey) {
          // Redo: Ctrl+Shift+Z
          e.preventDefault();
          redo();
        } else {
          // Undo: Ctrl+Z
          e.preventDefault();
          undo();
        }
      } else if (e.key === 'y' || e.key === 'Y') {
        // Redo: Ctrl+Y
        e.preventDefault();
        redo();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [enableKeyboard, undo, redo]);

  // Build context value
  const contextValue: UndoRedoContextValue<T> = {
    state: {
      current: currentRef.current,
      canUndo: pastRef.current.length > 0,
      canRedo: futureRef.current.length > 0,
      undoCount: pastRef.current.length,
      redoCount: futureRef.current.length,
    },
    actions: {
      push,
      undo,
      redo,
      reset,
      clear,
      getCurrent,
    },
  };

  return (
    <UndoRedoContext.Provider value={contextValue as UndoRedoContextValue}>
      {children}
    </UndoRedoContext.Provider>
  );
}

/**
 * Hook to access undo/redo functionality.
 * Must be used within an UndoRedoProvider.
 */
export function useUndoRedo<T>(): UndoRedoContextValue<T> {
  const context = useContext(UndoRedoContext);
  if (!context) {
    throw new Error('useUndoRedo must be used within an UndoRedoProvider');
  }
  return context as UndoRedoContextValue<T>;
}

/**
 * Standalone hook for undo/redo without context.
 * Useful for component-level undo/redo.
 */
export function useUndoRedoState<T>(
  initialState: T,
  options: { maxHistory?: number; enableKeyboard?: boolean } = {}
): [T, UndoRedoActions<T>, UndoRedoState<T>] {
  const { maxHistory = MAX_HISTORY_SIZE, enableKeyboard = false } = options;

  const pastRef = useRef<T[]>([]);
  const futureRef = useRef<T[]>([]);
  const [current, setCurrent] = useState<T>(initialState);

  const getCurrent = useCallback((): T => current, [current]);

  const push = useCallback(
    (newState: T) => {
      setCurrent((prev) => {
        if (JSON.stringify(newState) === JSON.stringify(prev)) {
          return prev;
        }
        pastRef.current = [...pastRef.current, prev].slice(-maxHistory);
        futureRef.current = [];
        return newState;
      });
    },
    [maxHistory]
  );

  const undo = useCallback((): T | undefined => {
    if (pastRef.current.length === 0) return undefined;

    const previous = pastRef.current[pastRef.current.length - 1];
    pastRef.current = pastRef.current.slice(0, -1);

    setCurrent((curr) => {
      futureRef.current = [curr, ...futureRef.current].slice(0, maxHistory);
      return previous;
    });

    return previous;
  }, [maxHistory]);

  const redo = useCallback((): T | undefined => {
    if (futureRef.current.length === 0) return undefined;

    const next = futureRef.current[0];
    futureRef.current = futureRef.current.slice(1);

    setCurrent((curr) => {
      pastRef.current = [...pastRef.current, curr].slice(-maxHistory);
      return next;
    });

    return next;
  }, [maxHistory]);

  const reset = useCallback((newInitialState: T) => {
    pastRef.current = [];
    futureRef.current = [];
    setCurrent(newInitialState);
  }, []);

  const clear = useCallback(() => {
    pastRef.current = [];
    futureRef.current = [];
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    if (!enableKeyboard) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
      const ctrlKey = isMac ? e.metaKey : e.ctrlKey;

      if (!ctrlKey) return;
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      if (e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        undo();
      } else if ((e.key === 'z' && e.shiftKey) || e.key === 'y') {
        e.preventDefault();
        redo();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [enableKeyboard, undo, redo]);

  const state: UndoRedoState<T> = {
    current,
    canUndo: pastRef.current.length > 0,
    canRedo: futureRef.current.length > 0,
    undoCount: pastRef.current.length,
    redoCount: futureRef.current.length,
  };

  const actions: UndoRedoActions<T> = {
    push,
    undo,
    redo,
    reset,
    clear,
    getCurrent,
  };

  return [current, actions, state];
}

/**
 * UndoRedoToolbar component for displaying undo/redo buttons.
 */
export interface UndoRedoToolbarProps {
  className?: string;
  showCounts?: boolean;
}

export const UndoRedoToolbar: React.FC<UndoRedoToolbarProps> = ({
  className = '',
  showCounts = false,
}) => {
  const { state, actions } = useUndoRedo();

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <button
        onClick={() => actions.undo()}
        disabled={!state.canUndo}
        className="px-3 py-1 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed rounded transition-colors"
        title="Undo (Ctrl+Z)"
      >
        ↩ Undo
        {showCounts && state.undoCount > 0 && (
          <span className="ml-1 text-xs text-gray-500">({state.undoCount})</span>
        )}
      </button>
      <button
        onClick={() => actions.redo()}
        disabled={!state.canRedo}
        className="px-3 py-1 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed rounded transition-colors"
        title="Redo (Ctrl+Y)"
      >
        ↪ Redo
        {showCounts && state.redoCount > 0 && (
          <span className="ml-1 text-xs text-gray-500">({state.redoCount})</span>
        )}
      </button>
    </div>
  );
};

export default UndoRedoProvider;
