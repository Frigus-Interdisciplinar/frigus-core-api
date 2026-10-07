package com.frigus.coreapi.service;

import com.frigus.coreapi.client.BrevoEmailClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final BrevoEmailClient brevo;
    private final String frontendUrl;

    public EmailService(
            BrevoEmailClient brevo,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.brevo = brevo;
        this.frontendUrl = frontendUrl;
    }

    public void sendLink(String to, String subject, String path, String token) {
        String link = frontendUrl.replaceAll("/$", "") + path + "?token=" + token;
        String body = "Acesse o link para continuar: "
                + link
                + "\nSe você não solicitou esta ação, ignore este e-mail.";
        brevo.send(to, subject, body);
    }

    public void sendRecoveryCode(String to, String code) {
        String subject = "Código para redefinir sua senha Frigus";
        String body = "Seu código de recuperação é: "
                + code
                + "\nEle é válido por 10 minutos. "
                + "Se você não solicitou a redefinição da senha, ignore este e-mail.";
        brevo.send(to, subject, body);
    }
}
