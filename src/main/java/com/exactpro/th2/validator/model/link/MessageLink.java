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
import com.exactpro.th2.validator.model.pin.LinkToEndpoint;

public final class MessageLink implements IdentifiableLink {
    private final LinkMeta linkMeta;

    private final Endpoint from;

    private final Endpoint to;

    public MessageLink(LinkMeta linkMeta, Endpoint from, Endpoint to) {
        this.linkMeta = linkMeta;
        this.from = from;
        this.to = to;
    }

    public LinkToEndpoint mqLinkToEndpoint() {
        return new LinkToEndpoint(from.getBox(), from.getPin());
    }

    public LinkToEndpoint grpcLinkToEndpoint() {
        return new LinkToEndpoint(to.getBox(), to.getPin());
    }

    @Override
    public String getResourceName() {
        return this.linkMeta.resName();
    }

    @Override
    public String getContent() {
        return this.linkMeta.content();
    }

    @Override
    public LinkErrorMessage errorMessage(String message) {
        return new BoxLinkErrorMessage(
                getContent(),
                message
        );
    }

    public Endpoint getFrom() {
        return this.from;
    }

    @Override
    public String getFromBox() {
        return getFrom().getBox();
    }

    public String getFromPin() {
        return getFrom().getPin();
    }

    public Endpoint getTo() {
        return this.to;
    }

    public String getToBox() {
        return getTo().getBox();
    }

    public String getToPin() {
        return getTo().getPin();
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
