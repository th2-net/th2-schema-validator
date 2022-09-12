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

import java.util.Collections;
import java.util.Map;

public final class BoxesValidator {
    private final SchemaValidationContext validationContext;

    private final Map<String, RepositoryResource> boxesMap;

    public BoxesValidator(SchemaValidationContext validationContext,
                          Map<String, RepositoryResource> boxesMap) {
        this.validationContext = validationContext;
        this.boxesMap = Collections.unmodifiableMap(boxesMap);
    }

    public void detectUrlPathsConflicts() {
        var urlPathsValidator = new UrlPathsValidator(validationContext, boxesMap);
        urlPathsValidator.detectUrlPathsConflicts();
    }

    public void validateSecrets(String namespace) {
        var secretsValidator = new SecretsValidator(validationContext, namespace, boxesMap.values());
        secretsValidator.validate();
    }
}
