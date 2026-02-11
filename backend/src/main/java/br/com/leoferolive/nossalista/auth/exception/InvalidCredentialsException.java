package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exception lançada quando credenciais de login são inválidas (email não existe OU senha incorreta)
 * Mensagem genérica "Email ou senha inválidos" por segurança (não vaza se email existe)
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email ou senha inválidos");
    }
}
