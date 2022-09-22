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

package com.exactpro.th2.validator.links.chain.impl;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.links.chain.AbstractValidator;
import com.exactpro.th2.validator.links.ValidationResult;
import com.exactpro.th2.validator.model.BoxLinkContext;
import com.exactpro.th2.validator.model.pin.MqPin;
import com.exactpro.th2.validator.model.Th2Spec;
import com.exactpro.th2.validator.model.pin.MqPublisherPin;
import com.exactpro.th2.validator.model.pin.MqSubscriberPin;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ExpectedMessageFormatAttr extends AbstractValidator {

    private final RepositoryResource linkedResource;

    private final String linkedPinName;

    private final String linkedResourceName;

    private String mainAttributePrefix;

    private List<String> otherMatchingAttributePrefixes;

    private List<String> contradictingAttributePrefixes;

    public ExpectedMessageFormatAttr(
            BoxLinkContext context,
            String mainAttributePrefix,
            List<String> contradictingAttributePrefixes,
            List<String> otherMatchingAttributePrefixes
    ) {
        this.linkedResource = context.getLinkedResource();
        this.linkedResourceName = context.getLinkedResourceName();
        this.linkedPinName = context.getLinkedPinName();

        this.mainAttributePrefix = mainAttributePrefix;

        this.otherMatchingAttributePrefixes = otherMatchingAttributePrefixes;
        this.contradictingAttributePrefixes = contradictingAttributePrefixes;
    }

    @Override
    public ValidationResult validate(Object object, Object... additional) {
        if (!(object instanceof MqPin)) {
            throw new IllegalStateException("Expected target of type PinSpec");
        }

        if (object instanceof MqPublisherPin) {
            return super.validate(object, additional);
        }

        var pin = (MqSubscriberPin) object;

        List<String> filteredAttributes = mainPrefixAttributes(pin);

        if (filteredAttributes.isEmpty()) {
            return super.validate(pin, additional);
        }

        ValidationResult resultForSubPin = checkForPinAttributes(pin, filteredAttributes, additional);
        if (resultForSubPin.isInvalid()) {
            return resultForSubPin;
        }

        if (linkedResource == null) {
            return ValidationResult.invalid(format("Linked resource: [%s] does not exist", linkedResourceName));
        }

        String exactAttribute = filteredAttributes.get(0);

        ObjectMapper mapper = new ObjectMapper();
        Th2Spec linkedResSpec = mapper.convertValue(linkedResource.getSpec(), Th2Spec.class);
        MqPin linkedPin = linkedResSpec.getMqPin(linkedPinName);

        List<String> attributesForLinkedPin = mainPrefixAttributes(linkedPin);

        if (filteredAttributes.isEmpty()) {
            return super.validate(pin, additional);
        }

        ValidationResult linkedPinMainAttributes = checkForPinAttributes(linkedPin, attributesForLinkedPin, additional);
        if (linkedPinMainAttributes.isInvalid()) {
            return linkedPinMainAttributes;
        }

        ValidationResult linkedPinOtherAttributeMatch = linkedPinAttributeMatch(
                linkedPin, exactAttribute, otherMatchingAttributePrefixes
        );
        if (linkedPinOtherAttributeMatch.isInvalid()) {
            return linkedPinOtherAttributeMatch;
        }
        return super.validate(pin, additional);
    }

    private ValidationResult checkForPinAttributes(MqPin pin, List<String> filteredAttributes, Object... additional) {
        ValidationResult duplicationResult = checkForDuplication(pin, filteredAttributes, additional);
        if (duplicationResult.isInvalid()) {
            return duplicationResult;
        }

        var contradictingAttributesResult = checkContradictingAttributes(pin, contradictingAttributePrefixes);
        if (contradictingAttributesResult.isInvalid()) {
            return contradictingAttributesResult;
        }

        return ValidationResult.valid();
    }

    private ValidationResult checkForDuplication(MqPin pin, List<String> filteredAttributes, Object... additional) {
        String pinName = pin.getName();

        if (filteredAttributes.size() > 1) {
            // error. more than 1 attribute with the same prefix.
            return ValidationResult.invalid(
                    format("Invalid pin: \"%s\". detected multiple attributes with prefix: [%s]",
                            pinName, mainAttributePrefix)
            );
        }

        return ValidationResult.valid();
    }

    private List<String> mainPrefixAttributes(MqPin pin) {
        return pin.getAttributes()
                .stream()
                .filter(attribute -> attribute.startsWith(mainAttributePrefix))
                .collect(Collectors.toUnmodifiableList());
    }

    protected ValidationResult checkContradictingAttributes(MqPin pin, List<String> excludedAttributePrefixes) {
        for (String excludedPrefix : excludedAttributePrefixes) {
            var contradictingAttributes = pin.getAttributes()
                    .stream()
                    .filter(attribute -> attribute.startsWith(excludedPrefix)).collect(Collectors.toList());
            if (contradictingAttributes.size() > 0) {
                return ValidationResult.invalid(format("Invalid pin: \"%s\". [%s] contradicts with: [%s]",
                        pin.getName(), contradictingAttributes, mainAttributePrefix)
                );
            }
        }
        return ValidationResult.valid();
    }

    protected ValidationResult linkedPinAttributeMatch(MqPin linkedPin,
                                                       String exactAttribute,
                                                       List<String> otherMatchingAttributePrefixes) {
        if (linkedPin == null) {
            return ValidationResult.invalid(format("Linked pin: [%s] on resource: [%s] does not exist",
                    linkedPinName, linkedResourceName));
        }

        List<String> otherMatchingAttributes = new ArrayList<>();
        for (String matchingPrefix : otherMatchingAttributePrefixes) {
            var attrFilter = linkedPin.getAttributes()
                    .stream()
                    .filter(attr -> attr.startsWith(matchingPrefix)).collect(Collectors.toList());
            otherMatchingAttributes.addAll(attrFilter);
        }
        if (linkedPin.getAttributes().contains(exactAttribute) || otherMatchingAttributes.size() > 0) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid(format("linked pin: [%s] on resource: [%s] does not contain [%s] attribute",
                linkedPinName, linkedResourceName, exactAttribute)
        );

    }
}
