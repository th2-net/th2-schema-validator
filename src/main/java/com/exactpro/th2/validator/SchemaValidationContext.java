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

import com.exactpro.th2.validator.model.BoxesRelation;
import com.exactpro.th2.validator.model.Th2LinkSpec;
import com.exactpro.th2.validator.model.link.DictionaryLink;
import com.exactpro.th2.validator.model.link.MessageLink;

import java.util.HashMap;
import java.util.Map;

import static com.exactpro.th2.validator.enums.ValidationStatus.VALID;

public class SchemaValidationContext {

    private boolean valid = true;

    private Map<String, ResourceValidationContext> resources = new HashMap<>();

    private ValidationReport report = new ValidationReport();

    public boolean isValid() {
        return valid;
    }

    public ValidationReport getReport() {
        return report;
    }

    public void setInvalidResource(String resourceName) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext()).setInvalid();
        this.valid = false;
    }

    public void setException(Exception e) {
        this.report.setException(e);
        this.valid = false;
    }

    public void addValidMqLink(String resourceName, MessageLink link) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext()).addValidMqLink(link);
    }

    public void addValidGrpcLink(String resourceName, MessageLink link) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext()).addValidGrpcLink(link);
    }

    public void addValidDictionaryLink(String resourceName, DictionaryLink dictionaryLink) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext())
                .addValidDictionaryLink(dictionaryLink);
    }

    public void addLinkErrorMessage(String message) {
        report.addLinkErrorMessage(message);
    }

    public void addSecretsErrorMessage(String message) {
        report.addSecretsErrorMessage(message);
    }

    public void removeInvalidLinks(String linkResName, Th2LinkSpec spec) {
        ResourceValidationContext resourceValidationContext = resources.get(linkResName);
        if (resourceValidationContext == null || resourceValidationContext.getStatus().equals(VALID)) {
            return;
        }
        BoxesRelation boxesRelation = spec.getBoxesRelation();
        boxesRelation.setMqLinks(resourceValidationContext.getValidMqLinks());
        boxesRelation.setGrpcLinks(resourceValidationContext.getValidGrpcLinks());
        spec.setDictionariesRelation(resourceValidationContext.getValidDictionaryLinks());
    }
}
