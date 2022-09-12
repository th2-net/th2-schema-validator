/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.validator.links;

import com.exactpro.th2.validator.links.enums.ValidationStatus;

public class ValidationResult {

    private final String message;

    private final ValidationStatus validationStatus;

    public static ValidationResult invalid(String message) {
        return new ValidationResult(message, ValidationStatus.INVALID);
    }

    public static ValidationResult valid() {
        return new ValidationResult("", ValidationStatus.VALID);
    }

    private ValidationResult(String message, ValidationStatus validationStatus) {
        this.message = message;
        this.validationStatus = validationStatus;
    }

    public String getMessage() {
        return message;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

}
