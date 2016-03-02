package io.ucoin.ucoinj.elasticsearch.action;

import io.ucoin.ucoinj.elasticsearch.action.currency.RestCurrencyIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.market.RestMarketRecordIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.registry.RestRegistryRecordIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.security.RestSecurityAuthAction;
import io.ucoin.ucoinj.elasticsearch.action.security.RestSecurityGetChallengeAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

public class RestModule extends AbstractModule implements Module {

    @Override protected void configure() {
        bind(RestCurrencyIndexAction.class).asEagerSingleton();

        bind(RestMarketRecordIndexAction.class).asEagerSingleton();

        bind(RestRegistryRecordIndexAction.class).asEagerSingleton();

        bind(RestSecurityGetChallengeAction.class).asEagerSingleton();
        bind(RestSecurityAuthAction.class).asEagerSingleton();
    }
}