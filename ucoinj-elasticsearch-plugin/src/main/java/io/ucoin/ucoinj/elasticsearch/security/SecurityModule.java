package io.ucoin.ucoinj.elasticsearch.security;

import io.ucoin.ucoinj.elasticsearch.security.challenge.ChallengeMessageStore;
import io.ucoin.ucoinj.elasticsearch.security.token.SecurityTokenStore;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

public class SecurityModule extends AbstractModule implements Module {

    @Override protected void configure() {
        bind(ChallengeMessageStore.class).asEagerSingleton();
        bind(SecurityTokenStore.class).asEagerSingleton();
    }
}