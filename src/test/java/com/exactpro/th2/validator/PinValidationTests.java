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

package com.exactpro.th2.validator;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.exactpro.th2.validator.util.ResourceUtils.getSection;
import static com.exactpro.th2.validator.util.ResourceUtils.getSectionArray;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class PinValidationTests {
    private static final YAMLMapper mapper = new YAMLMapper();

    private static final String PATH = "src/test/resources/";

    @Test
    void testDuplicatePinsValidation() throws IOException {
        final File duplicatePinsBoxFile = new File(PATH + "DuplicatePinsBox.yml");
        var box = mapper.readValue(duplicatePinsBoxFile, RepositoryResource.class);
        PinsValidator.removeDuplicatePins(List.of(box));

        var spec = (Map<String, Object>) box.getSpec();
        Map<String, Object> pins = getSection(spec, "pins");
        Map<String, Object> mq = getSection(pins, "mq");
        Map<String, Object> grpc = getSection(pins, "grpc");
        List<Map<String, Object>> subscribers = getSectionArray(mq, "subscribers");
        List<Map<String, Object>> publishers = getSectionArray(mq, "publishers");
        List<Map<String, Object>> client = getSectionArray(grpc, "client");
        List<Map<String, Object>> server = getSectionArray(grpc, "server");

        assertEquals(2, subscribers.size());
        assertEquals(Set.of("sub_pin", "unique_sub"), collectPinNames(subscribers));

        assertEquals(2, publishers.size());
        assertEquals(Set.of("pub_pin", "unique_pub"), collectPinNames(publishers));

        assertEquals(1, client.size());
        assertEquals(Set.of("unique_client"), collectPinNames(client));

        assertEquals(3, server.size());
        assertEquals(Set.of("server_pin", "unique_server", "unique_server_1"), collectPinNames(server));
    }

    @Test
    void testDuplicatePinsValidationNullSafety() throws IOException {
        final var noSpecBoxFile = new File(PATH + "NoSpecBox.yml");
        var noSpecBox = mapper.readValue(noSpecBoxFile, RepositoryResource.class);

        final var emptySubsectionsBoxFile = new File(PATH + "BoxWithEmptySubsections.yml");
        var emptySubsectionsBox = mapper.readValue(emptySubsectionsBoxFile, RepositoryResource.class);

        final var emptyMqPinsBoxFile = new File(PATH + "BoxWithEmptySection.yml");
        var emptyMqPinsBox = mapper.readValue(emptyMqPinsBoxFile, RepositoryResource.class);

        assertDoesNotThrow(() -> PinsValidator.removeDuplicatePins(List.of(
                noSpecBox, emptySubsectionsBox, emptyMqPinsBox)));
    }

    private Set<String> collectPinNames(List<Map<String, Object>> pins) {
        return pins.stream()
                .map(pin -> (String) pin.get("name"))
                .collect(Collectors.toSet());
    }
}
