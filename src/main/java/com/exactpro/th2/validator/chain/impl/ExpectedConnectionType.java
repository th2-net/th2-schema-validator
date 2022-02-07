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
import com.exactpro.th2.validator.enums.SchemaConnectionType;
import com.exactpro.th2.validator.enums.ValidationResult;
import com.exactpro.th2.validator.model.BoxLinkContext;
import com.exactpro.th2.validator.model.PinSpec;

public final class ExpectedConnectionType extends AbstractValidator {

    private final SchemaConnectionType connectionType;

    public ExpectedConnectionType(BoxLinkContext context) {
        this.connectionType = context.getConnectionType();
    }

    @Override
    public ValidationResult validate(Object object, Object... additional) {
        if (!(object instanceof PinSpec)) {
            throw new IllegalStateException("Expected target of type PinSpec");
        }
        var pin = (PinSpec) object;
        var actualConnectionType = pin.getConnectionType();
        if (actualConnectionType.equals(connectionType)) {
            return super.validate(pin, additional);
        }
        return ValidationResult.invalid(
                String.format("Invalid pin: \"%s\" expected pin type: [%s] received [%s]",
                        pin.getName(), connectionType, actualConnectionType)
        );
    }

}
