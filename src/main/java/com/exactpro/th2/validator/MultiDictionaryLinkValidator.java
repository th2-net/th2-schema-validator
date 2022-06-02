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

package com.exactpro.th2.validator;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.errormessages.DictionaryLinkErrorMessage;
import com.exactpro.th2.validator.model.link.MultiDictionaryLink;

public class MultiDictionaryLinkValidator {
    private final SchemaContext schemaContext;

    public MultiDictionaryLinkValidator(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    void validateLink(RepositoryResource linkRes, MultiDictionaryLink link) {
        String linkResName = linkRes.getMetadata().getName();
        SchemaValidationContext schemaValidationContext = schemaContext.getSchemaValidationContext();
        try {
            String boxName = link.getBox();
            RepositoryResource boxResource = schemaContext.getBox(boxName);
            //First check if box is present
            if (boxResource != null) {
                boolean valid = true;
                for (var dictionaryName : link.getDictionaryNames()) {
                    //if box is present validate that required dictionary also exists
                    if (schemaContext.getDictionary(dictionaryName) == null) {
                        valid = false;
                        schemaValidationContext.setInvalidResource(linkResName);
                        schemaValidationContext.addLinkErrorMessage(linkResName, link.errorMessage(
                                String.format("Dictionary '%s' doesn't exist", dictionaryName)));
                        break;
                    }
                }
                if (valid) {
                    schemaValidationContext.addValidMultiDictionaryLink(linkResName, link);
                }
            } else {
                schemaValidationContext.setInvalidResource(linkResName);
                schemaValidationContext.addLinkErrorMessage(linkResName, link.errorMessage(
                        String.format("Resource '%s' doesn't exist", boxName)));
            }
        } catch (Exception e) {
            schemaValidationContext.setInvalidResource(linkResName);
            schemaValidationContext.addLinkErrorMessage(linkResName,
                    new DictionaryLinkErrorMessage(
                            link.getName(),
                            null,
                            null,
                            String.format("Exception: %s", e.getMessage())
                    )
            );
        }
    }
}
