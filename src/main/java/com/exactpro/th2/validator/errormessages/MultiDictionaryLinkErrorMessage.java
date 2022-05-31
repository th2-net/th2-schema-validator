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

package com.exactpro.th2.validator.errormessages;

import java.util.List;

public class MultiDictionaryLinkErrorMessage extends LinkErrorMessage {
    private final String box;

    private final List<String> dictionaryNames;

    public MultiDictionaryLinkErrorMessage(String linkName, String box, List<String> dictionaryNames, String message) {
        super(linkName, message);
        this.box = box;
        this.dictionaryNames = dictionaryNames;
    }

    @Override
    public String toPrintableMessage() {
        return String.format("link: \"%s\" [box: %s] - [dictionaries: %s] is invalid. %s",
                getLinkName(), box, dictionaryNames, getMessage());
    }
}
