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

import com.exactpro.th2.validator.enums.ValidationStatus;
import com.exactpro.th2.validator.model.link.MessageLink;

import java.util.ArrayList;
import java.util.List;

public class ResourceValidationContext {

    private ValidationStatus status = ValidationStatus.VALID;

    private final List<MessageLink> validMqLinks = new ArrayList<>();

    private final List<MessageLink> validGrpcLinks = new ArrayList<>();

    public void setInvalid() {
        this.status = ValidationStatus.INVALID;
    }

    public void addValidMqLink(MessageLink link) {
        this.validMqLinks.add(link);
    }

    public void addValidGrpcLink(MessageLink link) {
        this.validGrpcLinks.add(link);
    }

    public List<MessageLink> getValidMqLinks() {
        return validMqLinks;
    }

    public List<MessageLink> getValidGrpcLinks() {
        return validGrpcLinks;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public boolean isInvalid() {
        return status == ValidationStatus.INVALID;
    }
}
