package org.duniter.core.service;

/*
 * #%L
 * Duniter4j :: Core Shared
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Joiner;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.model.SmtpConfig;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

public class MailServiceImpl implements MailService, Closeable {

    private SmtpConfig smtpConfig;
    private static Session session;
    private static Transport transport;

    public MailServiceImpl() {

    }

    @Override
    public void setSmtpConfig(SmtpConfig smtpConfig) {
        this.smtpConfig = smtpConfig;
    }

    @Override
    public void sendTextEmail(String subject,
                              String textContent,
                              String... recipients) {
        try{
            ContentType contentType = new ContentType("text/plain");
            contentType.setParameter("charset", "UTF-8");
            sendEmail(subject, contentType.toString(), textContent, recipients);
        }
        catch(ParseException e) {
            // Should never occur
            throw new TechnicalException(e);
        }
    }

    @Override
    public void sendHtmlEmail(String subject,
                              String htmlContent,
                              String... recipients) {
        try{
            ContentType contentType = new ContentType("text/html");
            contentType.setParameter("charset", "UTF-8");

            Multipart content = new MimeMultipart();
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setContent(htmlContent, contentType.toString());
            content.addBodyPart(mbp);

            sendEmail(subject, content.getContentType(), content, recipients);
        }
        catch(MessagingException e) {
            // Should never occur
            throw new TechnicalException(e);
        }

    }

    @Override
    public void sendHtmlEmailWithText(String subject,
                                      String textContent,
                                      String htmlContent,
                                      String... recipients) {
        try{

            Multipart content = new MimeMultipart("alternative");

            // Add text part
            {
                MimeBodyPart mbp = new MimeBodyPart();
                ContentType contentType = new ContentType("text/plain");
                contentType.setParameter("charset", "UTF-8");
                mbp.setContent(textContent, contentType.toString());
                content.addBodyPart(mbp);
            }

            // Add html part
            {
                MimeBodyPart mbp = new MimeBodyPart();
                ContentType contentType = new ContentType("text/html");
                contentType.setParameter("charset", "UTF-8");
                mbp.setContent(htmlContent, contentType.toString());
                content.addBodyPart(mbp);
            }

            sendEmail(subject, content.getContentType(), content, recipients);
        }
        catch(MessagingException e) {
            // Should never occur
            throw new TechnicalException(e);
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

    public void sendEmail(String subject,
                          String contentType,
                          Object content,
                          String... recipients) {

        if (CollectionUtils.isEmpty(recipients) || StringUtils.isBlank(subject) || content == null || contentType == null) {
            throw new TechnicalException("Invalid arguments: 'recipients', 'subject', 'contentType' or 'content' could not be null or empty");
        }

        if (!isConnected()) {
            connect(smtpConfig);
        }

        // send email to recipients
        try {
            Message message = new MimeMessage(session);

            message.setFrom(getSenderAddress(smtpConfig));

            Address[] recipientsAddresses = Arrays.asList(recipients).stream().map(recipient -> {
                try {
                    return new InternetAddress(recipient);
                }
                catch (AddressException e) {
                    throw new TechnicalException(String.format("Error while sending email. Bad recipient address [%s]", recipient), e);
                }
            }).collect(Collectors.toList()).toArray(new InternetAddress[recipients.length]);

            message.setRecipients(Message.RecipientType.TO, recipientsAddresses);
            message.setSubject(subject);

            message.setContent(content, contentType);
            message.setSentDate(new java.util.Date());
            message.saveChanges();
            transport.sendMessage(message, message.getAllRecipients());

        }  catch (MessagingException e) {
            throw new TechnicalException(String.format("Error while sending email to [%s] using smtp server [%s]: %s",
                    Joiner.on(',').join(recipients), getSmtpServerAsString(),
                    e.getMessage()
            ), e);
        }
    }

    private String getSmtpServerAsString() {
        if (smtpConfig == null) return "";
        return getSmtpServerAsString(smtpConfig.getSmtpHost(), smtpConfig.getSmtpPort(), smtpConfig.getSmtpUsername());
    }


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

    private InternetAddress getSenderAddress(SmtpConfig smtpConfig) {
        Preconditions.checkNotNull(smtpConfig);
        try {
            if (StringUtils.isNotBlank(smtpConfig.getSenderAddress())) {
                return new InternetAddress(smtpConfig.getSenderAddress(), smtpConfig.getSenderName());
            }
            return new InternetAddress(smtpConfig.getSenderAddress());
        }
        catch(UnsupportedEncodingException | AddressException e) {
            throw new TechnicalException(e);
        }
    }

    private void connect(String smtpHost, int smtpPort,
                         String smtpUsername,
                         String smtpPassword,
                         String issuer,
                         boolean useSsl,
                         boolean starttls) {
        // check arguments
        if (StringUtils.isBlank(smtpHost) || smtpPort <= 0) {
            throw new TechnicalException("Invalid arguments: 'smtpHost' could not be null or empty, and 'smtpPort' could not be <= 0");
        }
        if (StringUtils.isBlank(issuer)) {
            throw new TechnicalException("Invalid arguments: 'issuer' could not be null or empty");
        }

        this.smtpConfig = new SmtpConfig();
        smtpConfig.setSmtpHost(smtpHost);
        smtpConfig.setSmtpPort(smtpPort);
        smtpConfig.setSmtpUsername(smtpUsername);
        smtpConfig.setSmtpPassword(smtpPassword);
        smtpConfig.setSenderAddress(issuer);
        smtpConfig.setUseSsl(useSsl);
        smtpConfig.setStartTLS(starttls);
        connect(this.smtpConfig);
    }

    private void connect(SmtpConfig config) {

        // Workaround, to avoid error on content type
        // http://stackoverflow.com/questions/21856211/javax-activation-unsupporteddatatypeexception-no-object-dch-for-mime-type-multi
        Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );

        configureJavaMailMimeTypes();

        Properties props = new Properties();

        // check arguments
        if (StringUtils.isBlank(config.getSmtpHost()) || config.getSmtpPort() <= 0) {
            throw new TechnicalException("Invalid arguments: 'smtpHost' could not be null or empty, and 'smtpPort' could not be <= 0");
        }
        if (StringUtils.isBlank(config.getSenderAddress())) {
            throw new TechnicalException("Invalid arguments: 'senderAddress' could not be null or empty");
        }

        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());
        if (StringUtils.isNotBlank(config.getSenderAddress())) {
            props.put("mail.from", config.getSenderAddress());
            if (StringUtils.isNotBlank(config.getSenderName())) {
                props.put("mail.from.alias", config.getSenderName());
            }
        }
        if (config.isUseSsl()) {
            props.put("mail.smtp.socketFactory.port", config.getSmtpPort());
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        if (config.isStartTLS()) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        boolean useAuth = false;
        // auto set authentification if smtp user name is provided
        if (StringUtils.isNotBlank(config.getSmtpUsername())) {
            props.put("mail.smtp.auth", true);
            useAuth = true;
        }

        session = Session.getInstance(props);

        try {
            transport = session.getTransport("smtp");
            if (useAuth) {
                transport.connect(config.getSmtpUsername(), config.getSmtpPassword());
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


    /**
     * Workaround to define javax.mail MIME types to classes WITHOUT using classpath file.
     * See http://stackoverflow.com/questions/21856211/javax-activation-unsupporteddatatypeexception-no-object-dch-for-mime-type-multi
     */
    protected void configureJavaMailMimeTypes() {

        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");
    }
}