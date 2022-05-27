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

import com.exactpro.th2.validator.model.link.MessageLink;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public final class BoxesRelation {

    @JsonProperty("routerMq")
    private List<MessageLink> mqLinks = new ArrayList<>();

    @JsonProperty("routerGrpc")
    private List<MessageLink> grpcLinks = new ArrayList<>();

    public List<MessageLink> getRouterMq() {
        return this.mqLinks;
    }

    public List<MessageLink> getRouterGrpc() {
        return this.grpcLinks;
    }

    public void setMqLinks(List<MessageLink> mqLinks) {
        if (mqLinks != null) {
            this.mqLinks = mqLinks;
        }
    }

    public void setGrpcLinks(List<MessageLink> grpcLinks) {
        if (grpcLinks != null) {
            this.grpcLinks = grpcLinks;
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
