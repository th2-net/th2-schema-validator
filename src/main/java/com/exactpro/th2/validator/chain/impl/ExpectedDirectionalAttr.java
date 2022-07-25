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

package com.exactpro.th2.validator.chain.impl;

import com.exactpro.th2.validator.chain.AbstractValidator;
import com.exactpro.th2.validator.enums.BoxDirection;
import com.exactpro.th2.validator.enums.ValidationResult;
import com.exactpro.th2.validator.model.BoxLinkContext;
import com.exactpro.th2.validator.model.pin.MqPin;

import static com.exactpro.th2.validator.enums.DirectionAttribute.publish;
import static com.exactpro.th2.validator.enums.DirectionAttribute.subscribe;
import static java.lang.String.format;

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
                if (pin.getAttributes().contains(publish.name())) {
                    return ValidationResult.invalid(
                            format("Invalid pin: \"%s\". must not contain attribute: [%s]",
                                    pin.getName(), publish.name())
                    );
                }
                break;

            case from:
                if (pin.getAttributes().contains(subscribe.name())) {
                    return ValidationResult.invalid(
                            format("Invalid pin: \"%s\". must not contain attribute: [%s]",
                                    pin.getName(), subscribe.name())
                    );
                }
                break;
        }
        return super.validate(pin, additional);
    }
}
