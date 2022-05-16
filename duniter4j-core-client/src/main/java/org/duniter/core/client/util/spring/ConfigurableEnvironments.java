package org.duniter.core.client.util.spring;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class ConfigurableEnvironments {

    protected ConfigurableEnvironments() {
        // Helper class
    }

    public static Properties readProperties(@NonNull ConfigurableEnvironment env, Properties defaultOptions) {
        boolean debug = log.isDebugEnabled();
        List<MapPropertySource> sources = env.getPropertySources().stream()
            .filter(source -> source instanceof MapPropertySource)
            .map(source -> (MapPropertySource)source).collect(Collectors.toList());
        final Properties target = new Properties(defaultOptions);

        if (debug) log.debug("-- Reading environment properties... ---\n");
        for (MapPropertySource source: Lists.reverse(sources)) {

            if (debug) log.debug("Processing source {} ...", source.getName());

            // Cascade properties (keep original order)
            for (String key: source.getPropertyNames()) {
                Object value = source.getProperty(key);
                if (value != null) {
                    if (debug) {
                        if (target.containsKey(key)) log.debug(" {}={} /!\\ Overriding previous value", key, value);
                        else log.debug(" {}={}", key, value);
                    }
                    target.setProperty(key, value.toString());
                }
            }
        }

        // DEBUG
        if (debug) {
            log.debug("-- Environment properties - final summary ---\n");
            target.keySet()
                .stream()
                .map(Object::toString)
                .sorted()
                .forEach(key -> {
                    Object value = target.getProperty(key);
                    log.debug(" {}={}", key, value);
                });
        }

        return target;
    }
}
