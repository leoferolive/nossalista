import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../auth/session', () => ({
  clearStoredSession: vi.fn(),
  getStoredAuthToken: vi.fn(() => 'token-de-teste'),
}));

describe('client response interceptor', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it('preserves the session for requests that handle 401 locally', async () => {
    const { clearStoredSession } = await import('../auth/session');
    const { default: client } = await import('./client');
    const rejected = (
      client.interceptors.response as typeof client.interceptors.response & {
        handlers?: Array<{ rejected?: (error: unknown) => Promise<unknown> }>;
      }
    ).handlers?.[0]?.rejected;

    const error = {
      response: { status: 401 },
      config: { preserveSessionOnUnauthorized: true },
    };

    await expect(rejected?.(error)).rejects.toBe(error);
    expect(clearStoredSession).not.toHaveBeenCalled();
  });
});
