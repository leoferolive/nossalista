package br.com.leoferolive.nossalista.email.service;

/**
 * Serviço de envio de e-mails transacionais.
 */
public interface EmailService {

    /**
     * Envia e-mail de reset de senha.
     *
     * @param toEmail    destinatário
     * @param userName   nome do usuário
     * @param resetToken token de reset
     */
    void sendPasswordReset(String toEmail, String userName, String resetToken);

    /**
     * Envia e-mail de verificação de cadastro.
     *
     * @param toEmail           destinatário
     * @param userName          nome do usuário
     * @param verificationToken token de verificação
     */
    void sendEmailVerification(String toEmail, String userName, String verificationToken);

    /**
     * Envia e-mail de login por magic link.
     *
     * @param toEmail    destinatário
     * @param userName   nome do usuário
     * @param loginToken token de login
     */
    void sendMagicLink(String toEmail, String userName, String loginToken);
}
