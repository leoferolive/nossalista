import { useCallback, useEffect, useRef } from 'react';
import { WebSocketStatus } from '../contexts/WebSocketContext';
import { ListStateResponse } from '../types/List';

type IncomingItemsRevisionStatus = 'accepted' | 'stale' | 'missing';

interface UseListRealtimeSyncParams {
  listId?: string;
  wsStatus: WebSocketStatus;
  fetchItems: (listId: string) => Promise<void>;
  getListState: (listId: string) => Promise<ListStateResponse>;
}

interface UseListRealtimeSyncReturn {
  handleIncomingItemsRevision: (revision?: number) => IncomingItemsRevisionStatus;
  resetTrackedRevision: () => void;
}

export function useListRealtimeSync({
  listId,
  wsStatus,
  fetchItems,
  getListState,
}: UseListRealtimeSyncParams): UseListRealtimeSyncReturn {
  const prevWsStatusRef = useRef<WebSocketStatus>(wsStatus);
  const latestItemsRevisionRef = useRef<number | null>(null);

  const handleIncomingItemsRevision = useCallback((revision?: number): IncomingItemsRevisionStatus => {
    if (typeof revision !== 'number') {
      return 'missing';
    }

    const latestRevision = latestItemsRevisionRef.current;
    if (latestRevision !== null && revision < latestRevision) {
      return 'stale';
    }

    latestItemsRevisionRef.current = Math.max(latestRevision ?? 0, revision);
    return 'accepted';
  }, []);

  const resetTrackedRevision = useCallback(() => {
    latestItemsRevisionRef.current = null;
  }, []);

  useEffect(() => {
    const prevStatus = prevWsStatusRef.current;
    prevWsStatusRef.current = wsStatus;

    if (prevStatus === 'RECONNECTING' && wsStatus === 'CONNECTED' && listId) {
      void getListState(listId)
        .then((state) => {
          const latestRevision = latestItemsRevisionRef.current;
          if (latestRevision === null || state.revision > latestRevision) {
            latestItemsRevisionRef.current = state.revision;
            return fetchItems(listId);
          }
          return undefined;
        })
        .catch(() => fetchItems(listId));
    }
  }, [fetchItems, getListState, listId, wsStatus]);

  return {
    handleIncomingItemsRevision,
    resetTrackedRevision,
  };
}
