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

import com.exactpro.th2.validator.errormessages.BoxLinkErrorMessage;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class MessageLink implements IdentifiableLink {

    private String name;

    private Endpoint from;

    private Endpoint to;

    public String getName() {
        return this.name;
    }

    @Override
    public String getContent() {
        return String.format("%s[%s:%s-%s:%s]", this.getClass().getSimpleName(),
                from.getBox(), from.getPin(),
                to.getBox(), to.getPin());
    }

    @Override
    public LinkErrorMessage errorMessage(String message) {
        return new BoxLinkErrorMessage(
                getName(),
                from.getBox(),
                to.getBox(),
                message
        );
    }

    public Endpoint getFrom() {
        return this.from;
    }

    public Endpoint getTo() {
        return this.to;
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
