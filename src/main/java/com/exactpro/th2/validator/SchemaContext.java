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

package com.exactpro.th2.validator;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.enums.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class SchemaContext {

    private final String schemaName;

    private final Map<String, RepositoryResource> allBoxes;

    private final Map<String, RepositoryResource> dictionaries;

    private final SchemaValidationContext schemaValidationContext;

    public SchemaContext(String schemaName,
                         Map<String, RepositoryResource> allBoxes,
                         Map<String, RepositoryResource> dictionaries,
                         SchemaValidationContext schemaValidationContext) {
        this.schemaName = schemaName;
        this.allBoxes = allBoxes;
        this.dictionaries = dictionaries;
        this.schemaValidationContext = schemaValidationContext;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public RepositoryResource getBox(String boxName) {
        return allBoxes.get(boxName);
    }

    public Collection<RepositoryResource> getAllBoxes() {
        return allBoxes.values();
    }

    public boolean dictionaryExists(String dictionaryName) {
        return getDictionary(dictionaryName) != null;
    }

    private RepositoryResource getDictionary(String dictionaryName) {
        return dictionaries.get(dictionaryName);
    }

    public SchemaValidationContext getSchemaValidationContext() {
        return schemaValidationContext;
    }

    private static class ResourcesList {

        private final ValidationResult result = ValidationResult.valid();

        private final ArrayList<RepositoryResource> linkedResources = new ArrayList<>();
    }
}
