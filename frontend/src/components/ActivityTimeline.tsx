import React, { useState, useCallback } from 'react';
import { ActivityTimelineProps, ActivityLog } from '../types/ActivityLog';

/**
 * Componente de timeline de atividades
 * AC: Exibe histórico de ações com avatar, descrição e tempo relativo
 * AC: Colapsável com ícone 📜, suporta carregamento incremental
 */
export const ActivityTimeline: React.FC<ActivityTimelineProps> = ({
  activities,
  isOpen,
  onClose,
  onLoadMore,
  hasMore = false,
  loading = false,
}) => {
  const [expandedActivities, setExpandedActivities] = useState<Set<string>>(new Set());

  // Formata tempo relativo
  const formatRelativeTime = useCallback((dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSecs < 10) return 'agora mesmo';
    if (diffSecs < 60) return `há ${diffSecs}s`;
    if (diffMins < 60) return `há ${diffMins} min`;
    if (diffHours < 24) return `há ${diffHours}h`;
    if (diffDays === 1) return 'ontem';
    if (diffDays < 7) return ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'][date.getDay()];
    
    return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }, []);

  // Gera descrição da atividade baseada no tipo
  const getActivityDescription = useCallback((activity: ActivityLog): string => {
    const { action, targetName, details } = activity;

    switch (action) {
      case 'ITEM_ADDED':
        return `adicionou "${targetName || 'um item'}"`;
      case 'ITEM_CHECKED':
        return `marcou "${targetName || 'um item'}" como concluído`;
      case 'ITEM_UNCHECKED':
        return `desmarcou "${targetName || 'um item'}"`;
      case 'ITEM_UPDATED':
        return `editou "${targetName || 'um item'}"`;
      case 'ITEM_REMOVED':
        return `removeu "${targetName || 'um item'}"`;
      case 'MEMBER_JOINED':
        if (details?.method === 'LINK') {
          return `entrou via link de convite`;
        }
        return `foi convidado`;
      case 'MEMBER_LEFT':
        return `saiu da lista`;
      case 'MEMBER_REMOVED':
        return `foi removido${details?.removedBy ? ` por ${details.removedBy}` : ''}`;
      default:
        return 'realizou uma ação';
    }
  }, []);

  // Toggle expandir/descolapsar detalhes
  const toggleExpand = useCallback((activityId: string) => {
    setExpandedActivities((prev) => {
      const next = new Set(prev);
      if (next.has(activityId)) {
        next.delete(activityId);
      } else {
        next.add(activityId);
      }
      return next;
    });
  }, []);

  // Carregar mais atividades (scroll infinito)
  const handleLoadMore = useCallback(() => {
    if (loading || !hasMore || !onLoadMore) return;
    onLoadMore();
  }, [loading, hasMore, onLoadMore]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-y-0 right-0 w-full max-w-md bg-white shadow-xl transform transition-transform duration-300 z-50 flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b">
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <span>📜</span>
          <span>Atividades</span>
        </h2>
        <button
          onClick={onClose}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          aria-label="Fechar timeline"
        >
          <svg
            className="w-6 h-6 text-gray-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      </div>

      {/* Lista de atividades */}
      <div className="flex-1 overflow-y-auto p-4">
        {activities.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <p className="text-lg mb-2">Nenhuma atividade ainda</p>
            <p className="text-sm">As ações nesta lista aparecerão aqui.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {activities.map((activity, index) => {
              const isExpanded = expandedActivities.has(activity.id);
              const isFirst = index === 0;

              return (
                <div
                  key={activity.id}
                  className={`flex gap-3 ${
                    isFirst ? 'animate-fade-in' : ''
                  }`}
                  data-testid={`activity-${activity.id}`}
                >
                  {/* Avatar */}
                  <div className="flex-shrink-0">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white font-medium text-sm">
                      {(activity.userName || activity.userId).charAt(0).toUpperCase()}
                    </div>
                  </div>

                  {/* Conteúdo */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-900">
                      <span className="font-medium">{activity.userName || 'Usuário'}</span>{' '}
                      {getActivityDescription(activity)}
                    </p>
                    
                    {/* Tempo relativo */}
                    <p className="text-xs text-gray-500 mt-1">
                      {formatRelativeTime(activity.createdAt)}
                    </p>

                    {/* Detalhes expandidos (opcional) */}
                    {isExpanded && activity.details && (
                      <div className="mt-2 p-2 bg-gray-50 rounded text-xs text-gray-600">
                        <pre className="whitespace-pre-wrap font-mono">
                          {JSON.stringify(activity.details, null, 2)}
                        </pre>
                      </div>
                    )}

                    {/* Botão expandir/descolapsar */}
                    {activity.details && (
                      <button
                        onClick={() => toggleExpand(activity.id)}
                        className="text-xs text-blue-600 hover:text-blue-700 mt-1 focus:outline-none focus:underline"
                      >
                        {isExpanded ? 'Ocultar detalhes' : 'Ver detalhes'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Loading indicator */}
        {loading && (
          <div className="flex justify-center py-4">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        )}

        {/* Botão carregar mais */}
        {hasMore && !loading && onLoadMore && (
          <div className="flex justify-center py-4">
            <button
              onClick={handleLoadMore}
              className="px-4 py-2 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors text-sm font-medium"
            >
              Carregar mais atividades
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
