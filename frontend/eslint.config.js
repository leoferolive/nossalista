import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', 'coverage']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    plugins: {
      'react-hooks': reactHooks,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-hooks/exhaustive-deps': 'error',

      // Complexidade — gates calibrados para React 19 + TS estrito
      complexity: ['error', { max: 10 }],
      'max-lines-per-function': [
        'error',
        {
          max: 60,
          skipBlankLines: true,
          skipComments: true,
          IIFEs: true,
        },
      ],
      'max-lines': [
        'error',
        {
          max: 400,
          skipBlankLines: true,
          skipComments: true,
        },
      ],
      'max-depth': ['error', 4],
      'max-params': ['error', 5],
    },
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
  },
  {
    files: ['**/*.{test,spec}.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
    },
  },
  {
    files: ['**/*.test.{ts,tsx}', 'e2e/**/*.ts', 'src/test/**/*.ts'],
    rules: {
      'max-lines-per-function': 'off',
      'max-lines': 'off',
      complexity: 'off',
    },
  },
  // ---------------------------------------------------------------------
  // Dívida técnica documentada em docs/quality-gate-debt.md (2026-05-11).
  // Arquivos pré-existentes com violações de complexidade/tamanho.
  // TODO: refatorar até 2026-08-31 e remover este bloco.
  // ---------------------------------------------------------------------
  {
    files: [
      'mock/mockServer.ts',
      'vite.config.ts',
      'src/components/ActivityTimeline.tsx',
      'src/components/AppHeader.tsx',
      'src/components/AuthLayout.tsx',
      'src/components/CreateListModal.tsx',
      'src/components/DeleteConfirmModal.tsx',
      'src/components/DeleteListModal.tsx',
      'src/components/EditItemModal.tsx',
      'src/components/EditListNameModal.tsx',
      'src/components/InviteModal.tsx',
      'src/components/ItemOptionsMenu.tsx',
      'src/components/ListCard.tsx',
      'src/components/ListItem.tsx',
      'src/components/LoginModal.tsx',
      'src/components/MembersModal.tsx',
      'src/components/ModalShell.tsx',
      'src/components/NotificationBell.tsx',
      'src/components/OnboardingTourOverlay.tsx',
      'src/components/RegisterModal.tsx',
      'src/components/ResponsiveActionMenu.tsx',
      'src/components/ResponsiveSheet.tsx',
      'src/components/UserProfile.tsx',
      'src/contexts/AuthContext.tsx',
      'src/contexts/NotificationContext.tsx',
      'src/contexts/OnboardingContext.tsx',
      'src/contexts/WebSocketContext.tsx',
      'src/hooks/useActivities.ts',
      'src/hooks/useItems.ts',
      'src/hooks/useLists.ts',
      'src/hooks/usePushNotifications.ts',
      'src/pages/AuthCallback.tsx',
      'src/pages/ForgotPassword.tsx',
      'src/pages/Home.tsx',
      'src/pages/JoinListPage.tsx',
      'src/pages/LandingPage.tsx',
      'src/pages/ListView.tsx',
      'src/pages/Login.tsx',
      'src/pages/Profile.tsx',
      'src/pages/Register.tsx',
      'src/pages/ResetPassword.tsx',
      'src/types/WebSocketMessage.ts',
    ],
    rules: {
      complexity: 'off',
      'max-lines-per-function': 'off',
      'max-lines': 'off',
    },
  },
])
