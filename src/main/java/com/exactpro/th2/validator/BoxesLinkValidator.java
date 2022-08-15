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

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.enums.ValidationResult;
import com.exactpro.th2.validator.model.BoxLinkContext;
import com.exactpro.th2.validator.model.link.MessageLink;

import static com.exactpro.th2.validator.enums.ValidationStatus.VALID;
import static java.lang.String.format;

abstract class BoxesLinkValidator {
    protected SchemaContext schemaContext;

    abstract void validateLink(MessageLink link);

    abstract ValidationResult validateByContext(RepositoryResource resource, BoxLinkContext context);

    abstract void addValidMessageLink(String linkResName, MessageLink link);

    BoxesLinkValidator(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    protected void validate(BoxLinkContext fromContext, BoxLinkContext toContext,
                            String resName, MessageLink link) {
        RepositoryResource fromRes = schemaContext.getBox(fromContext.getBoxName());
        RepositoryResource toRes = schemaContext.getBox(toContext.getBoxName());
        SchemaValidationContext schemaValidationContext = schemaContext.getSchemaValidationContext();

        ValidationResult fromResValidationResult = validateByContext(fromRes, fromContext);
        ValidationResult toResValidationResult = validateByContext(toRes, toContext);

        if (fromResValidationResult.getValidationStatus().equals(VALID)
                && toResValidationResult.getValidationStatus().equals(VALID)) {
            addValidMessageLink(resName, link);
            return;
        }
        String fromBoxName = link.getFromBox();
        String toBoxName = link.getToBox();
        //check if "from" resource is valid
        if (!fromResValidationResult.getValidationStatus().equals(VALID)) {
            String message = format("\"%s\" %s. link will be ignored.",
                    fromBoxName, fromResValidationResult.getMessage());
            //Mark "th2link" resource as invalid, since it contains invalid link
            schemaValidationContext.setInvalidResource(resName);
            schemaValidationContext.addLinkErrorMessage(resName, link.errorMessage(message));
        }
        if (!toResValidationResult.getValidationStatus().equals(VALID)) {
            String message = format("\"%s\" %s. link will be ignored.",
                    toBoxName, toResValidationResult.getMessage());
            //Mark "th2link" resource as invalid, since it contains invalid link
            schemaValidationContext.setInvalidResource(resName);
            schemaValidationContext.addLinkErrorMessage(resName, link.errorMessage(message));
        }
    }
}
