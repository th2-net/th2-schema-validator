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

import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ValidationReport {

    private final Map<String, List<LinkErrorMessage>> linkErrorMessages = new HashMap<>();

    private final List<BoxResourceErrorMessage> boxResourceErrorMessages = new ArrayList<>();

    private final List<String> exceptionMessages = new ArrayList<>();

    public <T extends LinkErrorMessage> void addLinkErrorMessage(String linkResName, T linkErrorMessage) {
        this.linkErrorMessages.computeIfAbsent(linkResName, k -> new ArrayList<>()).add(linkErrorMessage);
    }

    public void addBoxResourceErrorMessages(BoxResourceErrorMessage boxResourceErrorMessage) {
        this.boxResourceErrorMessages.add(boxResourceErrorMessage);
    }

    public Map<String, List<LinkErrorMessage>> getLinkErrorMessages() {
        return linkErrorMessages;
    }

    public List<BoxResourceErrorMessage> getBoxResourceErrorMessages() {
        return boxResourceErrorMessages;
    }

    public List<String> getExceptionMessages() {
        return exceptionMessages;
    }

    public void addExceptionMessage(String exceptionMessage) {
        this.exceptionMessages.add(exceptionMessage);
    }
}

