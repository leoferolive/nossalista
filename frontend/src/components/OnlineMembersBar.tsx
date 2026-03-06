import React from 'react'
import { OnlineMember } from '../types/OnlineMember'

interface OnlineMembersBarProps {
  members: OnlineMember[]
  currentUserId: string
}

function getInitials(name: string, username: string): string {
  const source = name?.trim() || username.trim()
  const parts = source.split(/\s+/).filter(Boolean)
  if (parts.length === 0) {
    return '??'
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase()
  }
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase()
}

export const OnlineMembersBar: React.FC<OnlineMembersBarProps> = ({ members, currentUserId }) => {
  if (members.length === 0) {
    return null
  }

  const visibleMembers = members.slice(0, 5)
  const overflowCount = Math.max(0, members.length - 5)
  const onlyCurrentUserOnline = members.length === 1 && members[0].userId === currentUserId

  return (
    <section
      className="bg-emerald-50 border border-emerald-100 rounded-lg p-3 mb-4"
      aria-label="Membros online"
    >
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-medium text-emerald-800">
          {onlyCurrentUserOnline ? 'Apenas você online agora' : `Online agora: ${members.length}`}
        </p>

        <div className="flex items-center">
          {visibleMembers.map((member, index) => (
            <div
              key={member.userId}
              className={`relative ${index > 0 ? '-ml-2' : ''}`}
              style={{ zIndex: index + 1 }}
              title={member.name || member.username}
            >
              {member.avatarUrl ? (
                <img
                  src={member.avatarUrl}
                  alt={member.username}
                  className="w-8 h-8 rounded-full border-2 border-white object-cover"
                />
              ) : (
                <div className="w-8 h-8 rounded-full border-2 border-white bg-emerald-100 text-emerald-800 text-xs font-semibold flex items-center justify-center">
                  {getInitials(member.name, member.username)}
                </div>
              )}
              <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-400 rounded-full border border-white" />
            </div>
          ))}

          {overflowCount > 0 && (
            <div className="-ml-2 w-8 h-8 rounded-full border-2 border-white bg-emerald-200 text-emerald-900 text-xs font-semibold flex items-center justify-center">
              +{overflowCount}
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
