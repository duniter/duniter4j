package org.duniter.elasticsearch.user;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

/**
 * Access to configuration options
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class PluginSettings extends org.duniter.elasticsearch.PluginSettings {

    @Inject
    public PluginSettings(Settings settings) {
        super(settings);
    }

    public String getDefaultStringAnalyzer() {
        return settings.get("duniter.string.analyzer", "english");
    }

    public String getKeyringSalt() {
        return settings.get("duniter.keyring.salt");
    }

    public String getKeyringPassword() {
        return settings.get("duniter.keyring.password");
    }

    public String getKeyringPublicKey() {
        return settings.get("duniter.keyring.pub");
    }

    public String getKeyringSecretKey() {
        return settings.get("duniter.keyring.sec");
    }

    public boolean enableDataSync()  {
        return settings.getAsBoolean("duniter.user.sync.enable", false);
    }

    public String getDataSyncHost()  {
        return settings.get("duniter.user.sync.host", "data.duniter.fr");
    }

    public int getDataSyncPort()  {
        return settings.getAsInt("duniter.user.sync.port", 80);
    }

    public String getMailSmtpHost()  {
        return settings.get("duniter.mail.smtp.host", "localhost");
    }

    public int getMailSmtpPort()  {
        return settings.getAsInt("duniter.mail.smtp.port", 25);
    }

    public String getMailSmtpUsername()  {
        return settings.get("duniter.mail.smtp.username");
    }

    public String getMailSmtpPassword()  {
        return settings.get("duniter.mail.smtp.password");
    }

    public String getMailAdmin()  {
        return settings.get("duniter.mail.admin");
    }

    public String getMailFrom()  {
        return settings.get("duniter.mail.from", "no-reply@duniter.fr");
    }

    public String getMailSubjectPrefix()  {
        return settings.get("duniter.mail.subject.prefix", "[Duniter4j ES]");
    }

    protected String getI18nBundleName() {
        return "duniter4j-es-user-i18n";
    }
}
