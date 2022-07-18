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

import com.exactpro.th2.validator.model.pin.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Th2Spec {
    private PinSpec pins = new PinSpec();

    public PinSpec getPins() {
        return pins;
    }

    public MqSection getMqPins() {
        return pins.getMq();
    }

    public List<MqSubscriberPin> getMqSubscribers() {
        return getMqPins().getSubscribers();
    }

    public GrpcSection getGrpcPins() {
        return pins.getGrpc();
    }

    public List<GrpcClientPin> getGrpcClientPins() {
        return getGrpcPins().getClient();
    }

    public List<GrpcServerPin> getGrpcServerPins() {
        return getGrpcPins().getServer();
    }

    public MqPin getMqPin(String name) {
        return getMqPins().getPin(name);
    }

    public GrpcClientPin getGrpcClientPin(String name) {
        return getGrpcPins().getClientPin(name);
    }

    public GrpcServerPin getGrpcServerPin(String name) {
        return getGrpcPins().getServerPin(name);
    }

    public Th2Pin getPin(String name) {
        Th2Pin pin = getMqPin(name);
        if (pin != null) {
            return pin;
        }
        pin = getGrpcClientPin(name);
        if (pin != null) {
            return pin;
        }
        pin = getGrpcServerPin(name);
        return pin;
    }
}
