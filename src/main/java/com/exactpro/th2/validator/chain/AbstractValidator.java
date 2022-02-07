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

package com.exactpro.th2.validator.chain;

import com.exactpro.th2.validator.enums.ValidationResult;

import java.util.Objects;

public abstract class AbstractValidator implements Validator<Object, ValidationResult, Object> {

    private Validator<Object, ValidationResult, Object> next;

    @Override
    public ValidationResult validate(Object object, Object... additional) {
        if (Objects.nonNull(next)) {
            return next.validate(object);
        }
        return ValidationResult.valid();
    }

    @Override
    public void setNext(Validator<Object, ValidationResult, Object> validator) {
        this.next = validator;
    }

}
