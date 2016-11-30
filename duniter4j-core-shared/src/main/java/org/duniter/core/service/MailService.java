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
