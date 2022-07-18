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

package com.exactpro.th2.validator.model.pin;

import com.exactpro.th2.validator.model.link.LinkMeta;

public final class LinkToEndpoint {
    private String box;

    private String pin;

    private static final String CONTENT_TEMPLATE = "FROM %s:%s TO %s:%s";

    public LinkToEndpoint() {}

    public LinkToEndpoint(String box, String pin) {
        this.box = box;
        this.pin = pin;
    }

    public String getBox() {
        return box;
    }

    public String getPin() {
        return pin;
    }

    public LinkMeta grpcLinkMetaData(String fromBoxName, String fromPinName) {
        return new LinkMeta(fromBoxName, String.format(CONTENT_TEMPLATE,
                fromBoxName, fromPinName,
                box, pin));
    }

    public LinkMeta mqLinkMetaData(String toBoxName, String toPinName) {
        return new LinkMeta(toBoxName, String.format(CONTENT_TEMPLATE,
                box, pin,
                toBoxName, toPinName));
    }
}
