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

import java.util.ArrayList;
import java.util.List;

public final class MqSection {
    private List<MqSubscriberPin> subscribers = new ArrayList<>();

    private List<MqPublisherPin> publishers = new ArrayList<>();

    public List<MqSubscriberPin> getSubscribers() {
        return subscribers;
    }

    public List<MqPublisherPin> getPublishers() {
        return publishers;
    }

    private <T extends MqPin> T getPin(List<T> pins, String name) {
        return pins.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public MqPin getPin(String name) {
        MqSubscriberPin subscriber = getPin(subscribers, name);
        if (subscriber != null) {
            return subscriber;
        }

        return getPin(publishers, name);
    }
}
