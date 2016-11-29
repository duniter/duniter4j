package org.duniter.core.service;

import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.StringUtils;

import javax.mail.*;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import java.io.Closeable;
import java.util.Properties;

public class MailServiceImpl implements MailService, Closeable {

    private static Session session;
    private static Transport transport;

    public MailServiceImpl() {

    }

    @Override
    public void sendTextEmail(String smtpHost,
                          int smtpPort,
                          String smtpUsername,
                          String smtpPassword,
                          String issuer,
                          String recipients,
                          String subject,
                          String textContent) {
        try{
            ContentType contentType = new ContentType("text/plain");
            contentType.setParameter("charset", "UTF-8");

            sendEmail(smtpHost, smtpPort, smtpUsername, smtpPassword,
                      issuer,
                      recipients,
                      subject,
                      contentType,
                      textContent);
        }
        catch(ParseException e) {
            // Should never occur
            throw new TechnicalException(e);
        }

    }

    @Override
    public void sendEmail(String smtpHost,
                          int smtpPort,
                          String smtpUsername,
                          String smtpPassword,
                          String issuer,
                          String recipients,
                          String subject,
                          ContentType contentType,
                          String content) {

        // check arguments
        if (StringUtils.isBlank(smtpHost) || smtpPort <= 0) {
            throw new TechnicalException("Invalid arguments: 'smtpHost' could not be null or empty, and 'smtpPort' could not be <= 0");
        }
        if (StringUtils.isBlank(issuer)) {
            throw new TechnicalException("Invalid arguments: 'issuer' could not be null or empty");
        }
        if (StringUtils.isBlank(recipients) || StringUtils.isBlank(subject) || StringUtils.isBlank(content) || contentType == null) {
            throw new TechnicalException("Invalid arguments: 'recipients', 'subject', 'contentType' or 'content' could not be null or empty");
        }

        if (!isConnected()) {
            connect(smtpHost, smtpPort, smtpUsername, smtpPassword, issuer);
        }

        // send email to recipients
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(issuer));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(subject);

            message.setContent(content, contentType.toString());
            message.saveChanges();
            transport.sendMessage(message, message.getAllRecipients());

        } catch (MessagingException e) {
            throw new TechnicalException(String.format("Error while sending email to [%s] using smtp server [%s]",
                    recipients,
                    getSmtpServerAsString(smtpHost, smtpPort, smtpUsername)
            ), e);
        } catch (IllegalStateException e) {
            throw new TechnicalException(String.format("Error while sending email to [%s] using smtp server [%s]",
                    recipients,
                    getSmtpServerAsString(smtpHost, smtpPort, smtpUsername)
            ), e);
        }
    }

    public void close() {
        if (isConnected()) {
            try {
                transport.close();
            }
            catch(Exception e) {
                // silent is gold
            }
            transport = null;
            session = null;
        }
    }

    /* -- private methods -- */

    private String getSmtpServerAsString(String smtpHost, int smtpPort, String smtpUsername) {
        StringBuilder buffer = new StringBuilder();
        if (StringUtils.isNotBlank(smtpUsername)) {
            buffer.append(smtpUsername).append("@");
        }
        return buffer.append(smtpHost)
                .append(":")
                .append(smtpPort)
                .toString();

    }

    private void connect(String smtpHost, int smtpPort,
                         String smtpUsername,
                         String smtpPassword,
                         String issuer) {
        Properties props = new Properties();

        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        if (StringUtils.isNotBlank(issuer)) {
            props.put("mail.from", issuer);
        }
        boolean useAuth = false;
        // auto set authentification if smtp user name is provided
        if (StringUtils.isNotBlank(smtpUsername)) {
            props.put("mail.smtp.auth", "true");
            //props.put("mail.smtp.starttls.enable", "true");
            useAuth = true;
        }

        session = Session.getInstance(props);

        try {
            transport = session.getTransport("smtp");
            if (useAuth) {
                transport.connect(smtpUsername, smtpPassword);
            } else {
                transport.connect();
            }
        } catch (NoSuchProviderException e) {
            throw new TechnicalException(e);
        } catch (MessagingException e) {
            throw new TechnicalException(e);
        }
    }

    private boolean isConnected() {
        if ((session == null) || (transport == null)) {
            return false;
        }
        return transport.isConnected();
    }

}