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

package com.exactpro.th2.validator.errormessages;

public class LinkErrorMessage implements PrintableMessage {

    private final String linkContent;

    private final String message;

    public LinkErrorMessage(String linkContent, String message) {
        this.linkContent = linkContent;
        this.message = message;
    }

    public String getLinkContent() {
        return linkContent;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toPrintableMessage() {
        return String.format("Link: %s is invalid. %s",
                getLinkContent(), getMessage());
    }
}
