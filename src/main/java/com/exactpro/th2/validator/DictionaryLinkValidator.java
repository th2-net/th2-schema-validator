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
import com.exactpro.th2.validator.errormessages.DictionaryLinkErrorMessage;
import com.exactpro.th2.validator.model.link.DictionaryLink;

public class DictionaryLinkValidator {
    private final SchemaContext schemaContext;

    public DictionaryLinkValidator(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    void validateLink(RepositoryResource linkRes, DictionaryLink link) {
        String linkResName = linkRes.getMetadata().getName();
        SchemaValidationContext schemaValidationContext = schemaContext.getSchemaValidationContext();
        try {
            String boxName = link.getFromBox();
            String dictionaryName = link.getDictionary().getName();
            RepositoryResource boxResource = schemaContext.getBox(boxName);
            //First check if box is present
            if (boxResource != null) {
                //if box is present validate that required dictionary also exists
                if (schemaContext.getDictionary(dictionaryName) != null) {
                    schemaValidationContext.addValidDictionaryLink(linkResName, link);
                    return;
                }
                schemaValidationContext.setInvalidResource(linkResName);
                schemaValidationContext.addLinkErrorMessage(linkResName, link.errorMessage(
                        String.format("Dictionary '%s' doesn't exist", dictionaryName)));
            } else {
                schemaValidationContext.setInvalidResource(linkResName);
                schemaValidationContext.addLinkErrorMessage(linkResName, link.errorMessage(
                        String.format("Resource '%s' doesn't exist", boxName)));
            }
        } catch (Exception e) {
            schemaValidationContext.setInvalidResource(linkResName);
            schemaValidationContext.addLinkErrorMessage(linkResName,
                    new DictionaryLinkErrorMessage(
                            link.getContent(),
                            null,
                            null,
                            String.format("Exception: %s", e.getMessage())
                    )
            );
        }
    }
}
