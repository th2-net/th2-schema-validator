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

package com.exactpro.th2.validator.model;

import com.exactpro.th2.validator.model.link.DictionaryLink;
import com.exactpro.th2.validator.model.link.MultiDictionaryLink;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Th2LinkSpec {

    @JsonProperty("boxes-relation")
    private BoxesRelation boxesRelation = new BoxesRelation();

    @JsonProperty("dictionaries-relation")
    private List<DictionaryLink> dictionariesRelation = new ArrayList<>();

    @JsonProperty("multi-dictionaries-relation")
    private List<MultiDictionaryLink> multiDictionaryRelation = new ArrayList<>();

    public BoxesRelation getBoxesRelation() {
        return this.boxesRelation;
    }

    public List<DictionaryLink> getDictionariesRelation() {
        return this.dictionariesRelation;
    }

    public void setDictionariesRelation(List<DictionaryLink> dictionariesRelation) {
        if (dictionariesRelation != null) {
            this.dictionariesRelation = dictionariesRelation;
        }
    }

    public List<MultiDictionaryLink> getMultiDictionaryRelation() {
        return this.multiDictionaryRelation;
    }

    public void setMultiDictionaryRelation(List<MultiDictionaryLink> multiDictionaryRelation) {
        this.multiDictionaryRelation = multiDictionaryRelation;
    }

    public void setBoxesRelation(BoxesRelation boxesRelation) {
        if (boxesRelation != null) {
            this.boxesRelation = boxesRelation;
        }
    }

    @Override
    public boolean equals(final Object o) {
        throw new AssertionError("method not defined");
    }

    @Override
    public int hashCode() {
        throw new AssertionError("method not defined");
    }

}
