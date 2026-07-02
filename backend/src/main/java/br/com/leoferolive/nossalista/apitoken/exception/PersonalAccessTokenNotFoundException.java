package br.com.leoferolive.nossalista.apitoken.exception;

/**
 * Lançada quando um PAT não é encontrado para o id e usuário informados
 * (inexistente ou pertencente a outro usuário — resposta 404 em ambos os
 * casos, para não revelar a existência do recurso de outra conta).
 */
public class PersonalAccessTokenNotFoundException extends RuntimeException {

    public PersonalAccessTokenNotFoundException(String message) {
        super(message);
    }
}
