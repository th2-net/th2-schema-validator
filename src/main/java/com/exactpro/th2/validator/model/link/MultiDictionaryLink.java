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

package com.exactpro.th2.validator.model.link;

import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.exactpro.th2.validator.errormessages.MultiDictionaryLinkErrorMessage;

import java.util.List;
import java.util.stream.Collectors;

public final class MultiDictionaryLink implements IdentifiableLink {

    private String name;

    private String box;

    private List<MultiDictionaryDescription> dictionaries;

    public List<String> getDictionaryNames() {
        return dictionaries.stream().map(MultiDictionaryDescription::getName).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContent() {
        return String.format("%s[%s:%s]",
                this.getClass().getSimpleName(),
                this.box,
                this.dictionaries == null ? "null" : dictionaries.toString());
    }

    @Override
    public LinkErrorMessage errorMessage(String message) {
        return new MultiDictionaryLinkErrorMessage(
                getName(),
                getBox(),
                getDictionaryNames(),
                message
        );
    }

    public String getBox() {
        return box;
    }

    public List<MultiDictionaryDescription> getDictionaries() {
        return dictionaries;
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
