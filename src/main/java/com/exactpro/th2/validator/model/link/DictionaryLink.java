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

import com.exactpro.th2.validator.errormessages.DictionaryLinkErrorMessage;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DictionaryLink implements IdentifiableLink {

    private LinkMeta linkMeta;

    private String fromBox;

    private DictionaryDescription dictionary;

    @Override
    public String getContent() {
        return String.format("%s[%s:%s]", /* may be removed */
                this.getClass().getSimpleName(),
                this.fromBox,
                this.dictionary == null ? "null" : this.dictionary.getName());
    }

    @Override
    public String getResourceName() {
        return linkMeta.resName();
    }

    @Override
    public LinkErrorMessage errorMessage(String message) {
        return new DictionaryLinkErrorMessage(
                getContent(),
                getFromBox(),
                dictionary.getName(),
                message
        );
    }

    @Override
    public String getFromBox() {
        return this.fromBox;
    }

    public DictionaryDescription getDictionary() {
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
