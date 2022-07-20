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

package com.exactpro.th2.validator.model.link;

import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DictionaryLink implements IdentifiableLink {

    private final String resName;

    private final String dictionary;

    public DictionaryLink(String resName, String dictionary) {
        this.resName = resName;
        this.dictionary = dictionary;
    }

    @Override
    public String getContent() {
        return String.format("[box:%s : dictionary:%s]",
                this.getResourceName(),
                this.dictionary);
    }

    @Override
    public String getResourceName() {
        return resName;
    }

    @Override
    public LinkErrorMessage errorMessage(String message) {
        return new LinkErrorMessage(
                getContent(),
                message
        );
    }

    public String getDictionary() {
        return this.dictionary;
    }

    @Override
    public boolean equals(Object o) {
        throw new AssertionError("method not defined");
    }

    @Override
    public int hashCode() {
        throw new AssertionError("method not defined");
    }
}
