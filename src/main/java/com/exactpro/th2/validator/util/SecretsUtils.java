/*
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.validator.util;

import com.exactpro.th2.infrarepo.RepositoryResource;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SecretsUtils {

    public static final String DEFAULT_SECRET_NAME = "secret-custom-config";

    public static Secret getCustomSecret(String namespace) {
        KubernetesClient kubernetesClient = new DefaultKubernetesClient();
        return kubernetesClient.secrets()
                .inNamespace(namespace)
                .withName(DEFAULT_SECRET_NAME).get();
    }

    public static Map<String, Object> extractCustomConfig(RepositoryResource resource) {
        String customConfigAlias = "custom-config";
        Map<String, Object> spec = (Map<String, Object>) resource.getSpec();
        return (Map<String, Object>) spec.get(customConfigAlias);
    }

    public static Set<String> generateSecretsConfig(Map<String, Object> customConfig) {
        Set<String> collector = new HashSet<>();
        CustomLookup customLookup = new CustomLookup(collector);
        StringSubstitutor stringSubstitutor = new StringSubstitutor(
                StringLookupFactory.INSTANCE.interpolatorStringLookup(
                        Map.of("secret_value", customLookup,
                                "secret_path", customLookup
                        ), null, false
                ));
        if (customConfig == null) {
            return Collections.emptySet();
        }
        for (var entry : customConfig.entrySet()) {
            var value = entry.getValue();
            if (value instanceof String) {
                String valueStr = (String) value;
                stringSubstitutor.replace(valueStr);
            } else if (value instanceof Map) {
                collector.addAll(generateSecretsConfig((Map<String, Object>) value));
            }
        }
        return collector;
    }

    static class CustomLookup implements StringLookup {

        private Set<String> collector;

        public CustomLookup(Set<String> collector) {
            this.collector = collector;
        }

        @Override
        public String lookup(String key) {
            collector.add(key);
            return null;
        }
    }
}
