package br.com.leoferolive.nossalista.push;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vapid")
public class VapidConfig {

    private String publicKey;
    private String privateKey;
    private String subject;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isConfigured() {
        return publicKey != null && !publicKey.isBlank()
            && privateKey != null && !privateKey.isBlank();
    }
}
