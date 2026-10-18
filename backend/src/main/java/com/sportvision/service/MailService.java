package com.sportvision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String smtpHost;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${spring.mail.host:}") String smtpHost,
                       @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.smtpHost = smtpHost;
        this.from = from;
    }

    @Async
    public void sendPasswordReset(String to, String firstName, String link, long minutes) {
        String body = "Bonjour " + firstName + ",\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe Sport Vision.\n"
                + "Cliquez sur le lien ci-dessous (valable " + minutes + " minutes, usage unique) :\n\n"
                + link + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet e-mail.\n\n"
                + "L'équipe Sport Vision";

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || smtpHost == null || smtpHost.isBlank()) {
            // Mode développement sans SMTP configuré
            log.warn("SMTP non configuré (SMTP_HOST). Lien de réinitialisation pour {} : {}", to, link);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Sport Vision - Réinitialisation de votre mot de passe");
            message.setText(body);
            sender.send(message);
        } catch (Exception e) {
            // Ne jamais révéler à l'appelant si l'envoi a réussi (anti-énumération de comptes)
            log.error("Échec d'envoi de l'e-mail de réinitialisation à {}", to, e);
        }
    }
}
