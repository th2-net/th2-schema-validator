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

import com.exactpro.th2.validator.links.chain.AbstractValidator;
import com.exactpro.th2.validator.links.enums.BoxDirection;
import com.exactpro.th2.validator.links.ValidationResult;
import com.exactpro.th2.validator.links.enums.DirectionAttribute;
import com.exactpro.th2.validator.model.BoxLinkContext;
import com.exactpro.th2.validator.model.pin.MqPin;

public final class ExpectedDirectionalAttr extends AbstractValidator {

    private final BoxDirection boxDirection;

    public ExpectedDirectionalAttr(BoxLinkContext context) {
        this.boxDirection = context.getBoxDirection();
    }

    @Override
    public ValidationResult validate(Object object, Object... additional) {
        if (!(object instanceof MqPin)) {
            throw new IllegalStateException("Expected target of type PinSpec");
        }
        var pin = (MqPin) object;
        switch (boxDirection) {
            case to:
                if (pin.getAttributes().contains(DirectionAttribute.publish.name())) {
                    return ValidationResult.invalid(
                            String.format("Invalid pin: \"%s\". must not contain attribute: [%s]",
                                    pin.getName(), DirectionAttribute.publish.name())
                    );
                }
                break;

            case from:
                if (pin.getAttributes().contains(DirectionAttribute.subscribe.name())) {
                    return ValidationResult.invalid(
                            String.format("Invalid pin: \"%s\". must not contain attribute: [%s]",
                                    pin.getName(), DirectionAttribute.subscribe.name())
                    );
                }
                break;
        }
        return super.validate(pin, additional);
    }
}