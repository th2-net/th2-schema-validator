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

import com.exactpro.th2.validator.model.link.DictionaryLink;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.exactpro.th2.validator.util.ResourceUtils.getSection;

@SuppressWarnings("unchecked")
class DictionaryLinkValidator {
    private final SchemaContext schemaContext;

    private static final String LINK_START = "${dictionary_link:";

    private static final String LINK_END = "}";

    public DictionaryLinkValidator(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    void validateLinks() {
        SchemaValidationContext schemaValidationContext = schemaContext.getSchemaValidationContext();

        for (var box : schemaContext.getAllBoxes()) {
            var spec = (Map<String, Object>) box.getSpec();
            Map<String, Object> customConfig = getSection(spec, "customConfig");
            if (customConfig == null) {
                continue;
            }

            final String boxName = box.getMetadata().getName();
            final Set<String> dictionaryNames = collectDictionaryNames(customConfig);

            for (var dictionaryName : dictionaryNames) {
                if (!schemaContext.dictionaryExists(dictionaryName)) {
                    var link = new DictionaryLink(boxName, dictionaryName);
                    schemaValidationContext.setInvalidResource(boxName);
                    schemaValidationContext.addLinkErrorMessage(boxName, link.errorMessage(
                            String.format("Dictionary '%s' doesn't exist", dictionaryName)));
                }
            }
        }
    }

    private Set<String> collectDictionaryNames(Map<String, Object> node) {
        Set<String> dictionaryNames = new HashSet<>();
        for (Object value : node.values()) {
            if (value instanceof String) {
                String valueStr = (String) value;
                if (isLink(valueStr)) {
                    var dictionaryName = valueStr.substring(LINK_START.length(), valueStr.length() - 1);
                    dictionaryNames.add(dictionaryName);
                }
            } else if (value instanceof Map) {
                dictionaryNames.addAll(collectDictionaryNames((Map<String, Object>) value));
            }
        }
        return dictionaryNames;
    }

    private boolean isLink(String value) {
        return value.startsWith(LINK_START) && value.endsWith(LINK_END);
    }
}
