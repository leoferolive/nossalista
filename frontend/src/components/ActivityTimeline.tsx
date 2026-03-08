import React, { useState, useCallback } from 'react'
import { ActivityTimelineProps, ActivityLog } from '../types/ActivityLog'

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
  const [expandedActivities, setExpandedActivities] = useState<Set<string>>(new Set())

  // Formata tempo relativo
  const formatRelativeTime = useCallback((dateString: string): string => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffSecs = Math.floor(diffMs / 1000)
    const diffMins = Math.floor(diffSecs / 60)
    const diffHours = Math.floor(diffMins / 60)
    const diffDays = Math.floor(diffHours / 24)

    if (diffSecs < 10) return 'agora mesmo'
    if (diffSecs < 60) return `há ${diffSecs}s`
    if (diffMins < 60) return `há ${diffMins} min`
    if (diffHours < 24) return `há ${diffHours}h`
    if (diffDays === 1) return 'ontem'
    if (diffDays < 7) return ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'][date.getDay()]

    return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
  }, [])

  // Gera descrição da atividade baseada no tipo
  const getActivityDescription = useCallback((activity: ActivityLog): string => {
    const { action, targetName, details } = activity

    switch (action) {
      case 'ITEM_ADDED':
        return `adicionou "${targetName || 'um item'}"`
      case 'ITEM_CHECKED':
        return `marcou "${targetName || 'um item'}" como concluído`
      case 'ITEM_UNCHECKED':
        return `desmarcou "${targetName || 'um item'}"`
      case 'ITEM_UPDATED':
        return `editou "${targetName || 'um item'}"`
      case 'ITEM_REMOVED':
        return `removeu "${targetName || 'um item'}"`
      case 'MEMBER_JOINED':
        if (details?.method === 'LINK') {
          return `entrou via link de convite`
        }
        return `foi convidado`
      case 'MEMBER_LEFT':
        return `saiu da lista`
      case 'MEMBER_REMOVED':
        return `foi removido${details?.removedBy ? ` por ${details.removedBy}` : ''}`
      default:
        return 'realizou uma ação'
    }
  }, [])

  // Toggle expandir/descolapsar detalhes
  const toggleExpand = useCallback((activityId: string) => {
    setExpandedActivities((prev) => {
      const next = new Set(prev)
      if (next.has(activityId)) {
        next.delete(activityId)
      } else {
        next.add(activityId)
      }
      return next
    })
  }, [])

  // Carregar mais atividades (scroll infinito)
  const handleLoadMore = useCallback(() => {
    if (loading || !hasMore || !onLoadMore) return
    onLoadMore()
  }, [loading, hasMore, onLoadMore])

  if (!isOpen) return null

  return (
    <div className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-nl-border bg-nl-surface-strong shadow-tropical">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-nl-border p-4">
        <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-nl-text">
          <span>📜</span>
          <span>Atividades</span>
        </h2>
        <button
          onClick={onClose}
          className="rounded-xl p-2 transition-colors hover:bg-nl-surface-strong focus-visible:ring-2 focus-visible:ring-nl-accent/30"
          aria-label="Fechar timeline"
        >
          <svg
            className="h-6 w-6 text-nl-muted"
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
      <div className="flex-1 overflow-y-auto p-4" style={{ overscrollBehavior: 'contain' }}>
        {activities.length === 0 ? (
          <div className="py-8 text-center text-nl-muted">
            <p className="text-lg mb-2">Nenhuma atividade ainda</p>
            <p className="text-sm">As ações nesta lista aparecerão aqui.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {activities.map((activity, index) => {
              const isExpanded = expandedActivities.has(activity.id)
              const isFirst = index === 0

              return (
                <div
                  key={activity.id}
                  className={`flex gap-3 ${isFirst ? 'animate-fade-in' : ''}`}
                  data-testid={`activity-${activity.id}`}
                >
                  {/* Avatar */}
                  <div className="flex-shrink-0">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-orange-500 to-amber-500 text-sm font-medium text-white">
                      {(activity.userName || activity.userId).charAt(0).toUpperCase()}
                    </div>
                  </div>

                  {/* Conteúdo */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-nl-text">
                      <span className="font-medium">{activity.userName || 'Usuário'}</span>{' '}
                      {getActivityDescription(activity)}
                    </p>

                    {/* Tempo relativo */}
                    <p className="mt-1 text-xs text-nl-muted">
                      {formatRelativeTime(activity.createdAt)}
                    </p>

                    {/* Detalhes expandidos (opcional) */}
                    {isExpanded && activity.details && (
                      <div className="mt-2 rounded bg-nl-surface-strong p-2 text-xs text-nl-muted">
                        <pre className="whitespace-pre-wrap font-mono">
                          {JSON.stringify(activity.details, null, 2)}
                        </pre>
                      </div>
                    )}

                    {/* Botão expandir/descolapsar */}
                    {activity.details && (
                      <button
                        onClick={() => toggleExpand(activity.id)}
                        className="mt-1 text-xs text-nl-primary transition-colors hover:text-nl-primary focus-visible:ring-2 focus-visible:ring-nl-accent/30"
                      >
                        {isExpanded ? 'Ocultar detalhes' : 'Ver detalhes'}
                      </button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* Loading indicator */}
        {loading && (
          <div className="flex justify-center py-4">
            <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-nl-accent"></div>
          </div>
        )}

        {/* Botão carregar mais */}
        {hasMore && !loading && onLoadMore && (
          <div className="flex justify-center py-4">
            <button
              onClick={handleLoadMore}
              className="rounded-xl bg-nl-surface-strong px-4 py-2 text-sm font-medium text-nl-accent transition-colors hover:bg-nl-surface-strong focus-visible:ring-2 focus-visible:ring-nl-accent/30"
            >
              Carregar mais atividades
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
