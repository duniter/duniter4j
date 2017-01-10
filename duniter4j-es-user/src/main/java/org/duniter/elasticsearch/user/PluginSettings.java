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


import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

/**
 * Access to configuration options
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class PluginSettings extends AbstractLifecycleComponent<PluginSettings> {

    private org.duniter.elasticsearch.PluginSettings delegate;

    @Inject
    public PluginSettings(Settings settings, org.duniter.elasticsearch.PluginSettings delegate) {
        super(settings);
        this.delegate = delegate;

        // Add i18n bundle name
        delegate.addI18nBundleName(getI18nBundleName());
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doClose() {

    }

    @Override
    protected void doStop() {

    }

    public org.duniter.elasticsearch.PluginSettings getDelegate() {
        return delegate;
    }

    public String getDefaultStringAnalyzer() {
        return delegate.getDefaultStringAnalyzer();
    }

    public boolean reloadIndices() {
        return delegate.reloadIndices();
    }

    public boolean enableDataSync() {
        return delegate.enableDataSync();
    }

    public boolean getMailEnable() {
        return settings.getAsBoolean("duniter.mail.enable", Boolean.TRUE);
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

    /* -- delegate methods -- */

    public String getClusterName() {
        return delegate.getClusterName();
    }

    public String getNodeBmaHost() {
        return delegate.getNodeBmaHost();
    }

    public int getNodeBmaPort() {
        return delegate.getNodeBmaPort();
    }

    public int getIndexBulkSize() {
        return delegate.getIndexBulkSize();
    }

    public boolean enableBlockchainSync() {
        return delegate.enableBlockchainSync();
    }

    public String getKeyringSalt() {
        return delegate.getKeyringSalt();
    }

    public String getKeyringPassword() {
        return delegate.getKeyringPassword();
    }

    public String getKeyringPublicKey() {
        return delegate.getKeyringPublicKey();
    }

    public String getKeyringSecretKey() {
        return delegate.getKeyringSecretKey();
    }

    public void addI18nBundleName(String bundleName) {
        delegate.addI18nBundleName(bundleName);
    }


    /* -- protected methods -- */

    protected String getI18nBundleName() {
        return "duniter4j-es-user-i18n";
    }


}
