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
import com.exactpro.th2.infrarepo.settings.RepositorySettingsResource;
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;

import java.util.*;

public class BookNamesValidator {

    private static final String BOOK_NAME = "bookName";

    public static void validate(RepositorySettingsResource settings,
                                Map<String, RepositoryResource> boxesMap,
                                SchemaValidationContext schemaValidationContext) {
        Set<String> checkedBooks = new HashSet<>();
        String defaultBook = settings.getSpec().getBookConfig().getDefaultBook();
        try {
            if (!bookExists(defaultBook)) {
                schemaValidationContext.addBoxResourceErrorMessages(new BoxResourceErrorMessage(
                        settings.getMetadata().getName(),
                        String.format("Specified Default book \"%s\" is not present in database", defaultBook)
                ));
            }
        } catch (Exception e) {
            schemaValidationContext.addExceptionMessage(e.getMessage());
        }
        checkedBooks.add(defaultBook);
        for (var entry : boxesMap.entrySet()) {
            try {
                Map<String, Object> spec = (Map<String, Object>) entry.getValue().getSpec();
                String bookName = (String) spec.get(BOOK_NAME);
                if (bookName == null || checkedBooks.contains(bookName)) {
                    continue;
                }
                if (!bookExists(bookName)) {
                    schemaValidationContext.addBoxResourceErrorMessages(new BoxResourceErrorMessage(
                            entry.getValue().getMetadata().getName(),
                            String.format("Specified book \"%s\" is not present in database", bookName)
                    ));
                }
                checkedBooks.add(bookName);
            } catch (Exception e) {
                schemaValidationContext.addExceptionMessage(e.getMessage());
            }
        }
    }

    private static boolean bookExists(String bookName) {
        return false;
    }
}
