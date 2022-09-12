/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.validator.boxes;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.SchemaValidationContext;
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import com.exactpro.th2.validator.util.SecretsUtils;
import io.fabric8.kubernetes.api.model.Secret;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.exactpro.th2.validator.util.SecretsUtils.extractCustomConfig;
import static com.exactpro.th2.validator.util.SecretsUtils.generateSecretsConfig;

class SecretsValidator {
    private final String namespace;

    private final SchemaValidationContext validationContext;

    private final Collection<RepositoryResource> allBoxes;

    SecretsValidator(
            SchemaValidationContext validationContext,
            String namespace,
            Collection<RepositoryResource> allBoxes
    ) {
        this.namespace = namespace;
        this.validationContext = validationContext;
        this.allBoxes = allBoxes;
    }

    void validate() {
        if (SecretsUtils.namespaceNotPresent(namespace)) {
            return;
        }
        Secret secret = SecretsUtils.getCustomSecret(namespace);
        if (secret == null) {
            String errorMessage = String.format("Secret \"secret-custom-config\" is not present in namespace: \"%s\"",
                    namespace);
            validationContext.addExceptionMessage(errorMessage);
            return;
        }
        Map<String, String> secretData = secret.getData();
        for (var res : allBoxes) {
            Map<String, Object> customConfig = extractCustomConfig(res);
            Set<String> secretsConfig = generateSecretsConfig(customConfig);
            if (!secretsConfig.isEmpty()) {
                for (String secretKey : secretsConfig) {
                    if (secretData == null || !secretData.containsKey(secretKey)) {
                        String resName = res.getMetadata().getName();
                        String errorMessage = String.format("Value \"%s\" from " +
                                "\"secret-custom-config\" is not present in Kubernetes", secretKey);
                        validationContext.setInvalidResource(resName);
                        validationContext.addBoxResourceErrorMessages(new BoxResourceErrorMessage(
                                resName,
                                errorMessage
                        ));
                    }
                }
            }
        }
    }
}
