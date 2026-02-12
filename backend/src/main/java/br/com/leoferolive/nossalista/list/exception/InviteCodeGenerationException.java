package br.com.leoferolive.nossalista.list.exception;

/**
 * Exceção lançada quando falha ao gerar um código de convite único
 *
 * Isso pode acontecer em casos extremamente raros de colisão
 * após múltiplas tentativas de geração
 */
public class InviteCodeGenerationException extends RuntimeException {

    private static final int DEFAULT_MAX_ATTEMPTS = 10;
    private final int maxAttempts;

    public InviteCodeGenerationException(int maxAttempts) {
        super(String.format(
            "Falha ao gerar código de convite único após %d tentativas. " +
            "Tente novamente ou contate o suporte se o problema persistir.",
            maxAttempts
        ));
        this.maxAttempts = maxAttempts;
    }

    public InviteCodeGenerationException() {
        this(DEFAULT_MAX_ATTEMPTS);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
