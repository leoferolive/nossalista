import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';

function buildLoginHref(redirectPath: string | null) {
  if (!redirectPath) {
    return '/login';
  }

  return `/login?redirect=${encodeURIComponent(redirectPath)}`;
}

export const ForgotPassword: React.FC = () => {
  const [searchParams] = useSearchParams();
  const redirectPath = searchParams.get('redirect');
  const loginHref = buildLoginHref(redirectPath);

  return (
    <AuthLayout
      badge="Recuperacao"
      title="Recupere Seu Acesso"
      description="O fluxo automatico de redefinicao ainda nao esta disponivel, mas voce consegue retomar o acesso pelos caminhos abaixo."
      footer={(
        <div className="rounded-3xl border border-orange-200 bg-orange-50/70 p-4 text-sm text-slate-700">
          <Link className="font-semibold text-teal-800 underline decoration-orange-400 underline-offset-4" to={loginHref}>
            Voltar para login
          </Link>
        </div>
      )}
    >
      <div className="space-y-4">
        <div className="rounded-[28px] border border-orange-200 bg-orange-50/80 p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.24em] text-amber-700">
            Status Atual
          </p>
          <p className="mt-3 text-sm leading-7 text-slate-700">
            A recuperacao de senha completa ainda nao foi implementada no backend. Por enquanto, use seu login com Google
            se ele estiver vinculado a conta ou volte ao login tradicional.
          </p>
        </div>

        <div className="rounded-[28px] border border-teal-200 bg-white p-5 text-sm leading-7 text-slate-600 shadow-sm">
          <p className="font-semibold text-slate-900">O Que Fazer Agora</p>
          <ul className="mt-3 space-y-2">
            <li>Use o botao do Google se voce ja acessou dessa forma antes.</li>
            <li>Volte ao login para tentar novamente com outro email.</li>
            <li>Se ainda nao tem conta, crie uma nova com email e senha.</li>
          </ul>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row">
          <Link
            to={loginHref}
            className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-2xl bg-gradient-to-r from-teal-700 to-teal-600 px-5 py-3 text-sm font-semibold text-white transition-colors hover:from-teal-800 hover:to-teal-700 focus-visible:ring-2 focus-visible:ring-orange-300"
          >
            Voltar Para Login
          </Link>
          <Link
            to={redirectPath ? `/register?redirect=${encodeURIComponent(redirectPath)}` : '/register'}
            className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-2xl border border-orange-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition-colors hover:border-orange-300 hover:bg-orange-50 hover:text-slate-950 focus-visible:ring-2 focus-visible:ring-orange-300"
          >
            Criar Conta
          </Link>
        </div>
      </div>
    </AuthLayout>
  );
};
