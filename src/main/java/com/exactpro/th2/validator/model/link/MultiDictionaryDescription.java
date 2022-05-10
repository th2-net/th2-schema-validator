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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class MultiDictionaryDescription {

    private final List<SingleDictionaryDescription> dicts = new ArrayList<>();

    public List<String> dictNames() {
        return dicts.stream().map(dict -> dict.getName()).collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o) {
        throw new AssertionError("method not defined");
    }

    @Override
    public int hashCode() {
        throw new AssertionError("method not defined");
    }

    public static final class SingleDictionaryDescription {
        private String name;

        private String alias;

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }
    }

}
