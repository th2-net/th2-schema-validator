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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class BookNamesValidator {
    private static final String BOOK_NAME = "bookName";

    private final SchemaValidationContext validationContext;

    private final Map<String, RepositoryResource> boxesMap;

    private final RepositorySettingsResource settings;

    private final String storageServiceBaseUrl;

    public BookNamesValidator(RepositorySettingsResource settings,
                              String storageServiceBaseUrl,
                              SchemaValidationContext validationContext,
                              Map<String, RepositoryResource> boxesMap) {
        this.settings = settings;
        this.validationContext = validationContext;
        this.boxesMap = Collections.unmodifiableMap(boxesMap);
        this.storageServiceBaseUrl = storageServiceBaseUrl;
    }

    public void validate() {
        String keyspace = settings.getSpec().getCradle().getKeyspace();
        try {
            if (!keyspaceExists(keyspace)) {
                validationContext.addExceptionMessage(
                        String.format("Specified Keyspace \"%s\" is not present in database. " +
                                "Can't proceed with books validation", keyspace));
                return;
            }
            checkBooks();
        } catch (Exception e) {
            validationContext.addExceptionMessage(e.getMessage());
        }
    }

    private void checkBooks() throws IOException {
        String keyspace = settings.getSpec().getCradle().getKeyspace();
        for (var entry : mapResourcesAndBooks().entrySet()) {
            String resource = entry.getKey();
            String book = entry.getValue();
            if (!bookExists(keyspace, book)) {
                validationContext.addBookErrorMessages(new BoxResourceErrorMessage(
                        resource,
                        String.format("Specified book \"%s\" is not present in database", book)
                ));
            }
        }
    }

    private boolean keyspaceExists(String keyspace) throws IOException {
        String urlStr = String.format(" http://%s/api/keyspaces/%s", storageServiceBaseUrl, keyspace);
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    private boolean bookExists(String keyspace, String bookName) throws IOException {
        String urlStr = String.format(" http://%s/api/%s/books/%s", storageServiceBaseUrl, keyspace, bookName);
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    private Map<String, String> mapResourcesAndBooks() {
        Map<String, String> resourceToBook = new HashMap<>();
        String defaultBook = settings.getSpec().getBookConfig().getDefaultBook();
        if (defaultBook != null) {
            resourceToBook.put(settings.getMetadata().getName(), defaultBook);
        }
        for (var entry : boxesMap.entrySet()) {
            Map<String, Object> spec = (Map<String, Object>) entry.getValue().getSpec();
            String bookName = (String) spec.get(BOOK_NAME);
            if (bookName == null) {
                continue;
            }
            resourceToBook.put(entry.getValue().getMetadata().getName(), bookName);
        }
        return resourceToBook;
    }
}
