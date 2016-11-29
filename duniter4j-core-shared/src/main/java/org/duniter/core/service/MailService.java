package org.duniter.core.service;

import org.duniter.core.beans.Bean;
import org.duniter.core.exception.TechnicalException;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

/**
 * Created by blavenie on 28/11/16.
 */
public interface MailService extends Bean {

    void sendTextEmail(String smtpHost,
                       int smtpPort,
                       String smtpUsername,
                       String smtpPassword,
                       String issuer,
                       String recipients,
                       String subject,
                       String textContent);

    void sendEmail(String smtpHost,
                   int smtpPort,
                   String smtpUsername,
                   String smtpPassword,
                   String issuer,
                   String recipients,
                   String subject,
                   ContentType contentType,
                   String content);
}
