package org.texttechnologylab.duui.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.texttechnologylab.duui.api.Main;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class DUUIMailClient {

    private static Logger log = LoggerFactory.getLogger(DUUIMailClient.class);

    public static void sendMail(String to, String subject, String body) throws MessagingException {
        long t0 = System.nanoTime();

        String host = Main.config.getSmtpHost();
        String port = Main.config.getSmtpPort();
        String user = Main.config.getSmtpUser();
        String pass = Main.config.getSmtpPassword();
        String from = Main.config.getSmtpFromEmail();

        boolean useDummy = Main.config.getUseSmptpDebug()
                || ("localhost".equalsIgnoreCase(host) && "1025".equals(port));

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session;

        if (useDummy) {
            // ---- Dummy server (MailHog/smtp4dev) ----
            props.put("mail.smtp.auth", "false");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "false");
            session = Session.getInstance(props);
            log.info("SMTP: dummy mode -> host={} port={} from={} to={}", host, port, from, to);
        } else {
            // ---- Real SMTP server (robust) ----
            boolean hasCreds = user != null && !user.isBlank() && pass != null && !pass.isBlank();
            String p = (port == null) ? "" : port.trim();

            props.put("mail.smtp.auth", String.valueOf(hasCreds));

            if ("465".equals(p)) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "false");
                props.put("mail.smtp.ssl.enable", "false");
            }

            if (hasCreds) {
                session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, pass);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            log.info("SMTP: real mode -> host={} port={} auth={} tls(starttls={}, ssl={}) from={} to={}",
                    host, port, hasCreds,
                    props.getProperty("mail.smtp.starttls.enable"),
                    props.getProperty("mail.smtp.ssl.enable"),
                    from, to);

            if (log.isDebugEnabled()) {
                String maskedUser = (user == null || user.isBlank()) ? "<none>" :
                        user.replaceAll("(?<=.).(?=[^@]*?@)", "•"); // mask username part
                log.debug("SMTP details: user={} starttls.required={}",
                        maskedUser, props.getProperty("mail.smtp.starttls.required"));
            }
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            // generate Message-ID before sending so we can log it
            message.saveChanges();

            Transport.send(message);

            String msgId = message.getMessageID();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (msgId != null) {
                log.info("SMTP: sent OK (messageId={}, subject=\"{}\", duration={}ms)", msgId, safeSubject(subject), ms);
            } else {
                log.info("SMTP: sent OK (subject=\"{}\", duration={}ms)", safeSubject(subject), ms);
            }
        } catch (MessagingException ex) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            log.error("SMTP: send FAILED (host={} port={} to={} subject=\"{}\" duration={}ms) -> {}",
                    host, port, to, safeSubject(subject), ms, ex.getMessage());
            throw ex;
        }
    }

    // Helper to keep console output short
    private static String safeSubject(String subject) {
        if (subject == null) return "<null>";
        String s = subject.replaceAll("\\s+", " ").trim();
        return (s.length() > 80) ? s.substring(0, 77) + "..." : s;
    }

}
