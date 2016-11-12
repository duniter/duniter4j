package org.duniter.elasticsearch.action.security;

import org.duniter.elasticsearch.PluginSettings;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestRequest;

import java.util.*;

/**
 * Created by blavenie on 11/10/16.
 */
public class RestSecurityController extends AbstractLifecycleComponent<RestSecurityController> {

    private static final ESLogger log = ESLoggerFactory.getLogger("security");

    private boolean enable;

    private Map<RestRequest.Method, Set<String>> allowRulesByMethod;

    @Inject
    public RestSecurityController(Settings settings, PluginSettings pluginSettings) {
        super(settings);
        this.enable = pluginSettings.enableSecurity();
        this.allowRulesByMethod = new HashMap<>();
    }

    public RestSecurityController allowIndexType(RestRequest.Method method, String index, String type) {
        return allow(method, String.format("/%s/%s(/.*)?", index, type));
    }

    public RestSecurityController allow(RestRequest.Method method, String regexPath) {
        Set<String> allowRules = allowRulesByMethod.get(method);
        if (allowRules == null) {
            allowRules = new TreeSet<>();
            allowRulesByMethod.put(method, allowRules);
        }
        allowRules.add(regexPath);
        return this;
    }

    public boolean isAllow(RestRequest request) {
        if (!this.enable) return true;
        RestRequest.Method method = request.method();
        if (log.isTraceEnabled()) {
            log.trace(String.format("Checking rules for %s request [%s]...", method, request.path()));
        }

        Set<String> allowRules = allowRulesByMethod.get(request.method());
        String path = request.path();
        if (allowRules != null) {
            for (String allowRule : allowRules) {
                if (path.matches(allowRule)) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Find matching rule [%s] for %s request [%s]: allow", allowRule, method, path));
                    }
                    return true;
                }
            }
        }

        log.trace(String.format("No matching rules for %s request [%s]: reject", method, path));
        return false;
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }

    /* -- Internal method -- */

}
