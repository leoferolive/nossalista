package br.com.leoferolive.nossalista.apitoken.exception;

/**
 * Lançada quando o usuário tenta criar um novo PAT já tendo atingido o
 * limite máximo de tokens ativos permitidos por conta.
 */
public class PersonalAccessTokenLimitExceededException extends RuntimeException {

    private final int maxTokens;

    public PersonalAccessTokenLimitExceededException(int maxTokens) {
        super("Limite de " + maxTokens
            + " tokens ativos atingido. Revogue um token existente antes de criar outro.");
        this.maxTokens = maxTokens;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}
