package br.com.leoferolive.nossalista.member.exception;

public class UserNotFoundForInviteException extends RuntimeException {

    public UserNotFoundForInviteException(String message) {
        super(message);
    }
}
