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

import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.exactpro.th2.validator.model.link.MessageLink;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SchemaValidationContext {

    private boolean valid = true;

    private final Map<String, ResourceValidationContext> resources = new HashMap<>();

    private final ValidationReport report = new ValidationReport();

    public boolean isValid() {
        return valid;
    }

    public ValidationReport getReport() {
        return report;
    }

    public Set<String> getInvalidResources() {
        return resources.keySet().stream()
                .filter(name -> resources.get(name).isInvalid())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void setInvalidResource(String resourceName) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext()).setInvalid();
        this.valid = false;
    }

    public void addExceptionMessage(String exceptionMessage) {
        this.report.addExceptionMessage(exceptionMessage);
        this.valid = false;
    }

    public void addValidMqLink(String resourceName, MessageLink link) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext()).addValidMqLink(link);
    }

    public void addValidGrpcLink(String resourceName, MessageLink link) {
        this.resources.computeIfAbsent(resourceName, k -> new ResourceValidationContext()).addValidGrpcLink(link);
    }

    public void addLinkErrorMessage(LinkErrorMessage linkErrorMessage) {
        this.valid = false;
        report.addLinkErrorMessage(linkErrorMessage);
    }

    public void addBoxResourceErrorMessages(BoxResourceErrorMessage boxResourceErrorMessage) {
        this.valid = false;
        report.addBoxResourceErrorMessages(boxResourceErrorMessage);
    }

    public void addBookErrorMessages(BoxResourceErrorMessage bookErrorMessage) {
        this.valid = false;
        report.addBookErrorMessages(bookErrorMessage);
    }

    public ResourceValidationContext getResource(String resName) {
        return resources.get(resName);
    }
}
