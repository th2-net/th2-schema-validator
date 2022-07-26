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

package com.exactpro.th2.validator.model.pin;

import java.util.ArrayList;
import java.util.List;

public final class GrpcSection {
    private List<GrpcClientPin> client = new ArrayList<>();

    private List<GrpcServerPin> server = new ArrayList<>();

    public List<GrpcClientPin> getClient() {
        return client;
    }

    public List<GrpcServerPin> getServer() {
        return server;
    }

    private <T extends Th2Pin> T getPin(List<T> pins, String name) {
        return pins.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public GrpcClientPin getClientPin(String name) {
        return getPin(getClient(), name);
    }

    public GrpcServerPin getServerPin(String name) {
        return getPin(getServer(), name);
    }
}
