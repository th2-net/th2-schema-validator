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

import com.exactpro.th2.infrarepo.ResourceType;
import com.exactpro.th2.infrarepo.repo.GenericResource;
import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.exactpro.th2.validator.model.link.DictionaryLink;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryLinkValidatorTests {
    private static final String DICTIONARY_TYPE = ResourceType.Th2Dictionary.kind();

    private static final String BOX_TYPE = ResourceType.Th2Box.kind();

    private static final String API = "v2";

    private static final String CUSTOM_CONFIG = "customConfig";

    private static final String SCHEMA = "schema";

    private static final Map<String, RepositoryResource> dictionaries = Map.of(
            "d1", new RepositoryResource(API, DICTIONARY_TYPE, new GenericResource.Metadata("d1"), null),
            "d2", new RepositoryResource(API, DICTIONARY_TYPE, new GenericResource.Metadata("d2"), null),
            "d3", new RepositoryResource(API, DICTIONARY_TYPE, new GenericResource.Metadata("d3"), "d3Spec")
    );

    private static final String LINKS_FILE = "src/test/resources/sampleDictionaryLinks.yml";

    @Test
    void testValidationOfFlatStructure() {
        var schemaValidationContext = new SchemaValidationContext();

        Map<String, RepositoryResource> boxes = new HashMap<>();

        Map<String, Map<String, String>> box1Spec = new HashMap<>();
        Map<String, Map<String, String>> box2Spec = new HashMap<>();

        box1Spec.computeIfAbsent(CUSTOM_CONFIG, customConfig -> new HashMap<>())
                .put("validLink1", "${dictionary_link:d1}");

        box1Spec.get(CUSTOM_CONFIG).put("validLink2", "${dictionary_link:d2}");
        box1Spec.get(CUSTOM_CONFIG).put("invalidLink", "${dictionary_link:null}");

        box2Spec.computeIfAbsent(CUSTOM_CONFIG, customConfig -> new HashMap<>())
                        .put("invalidLink", "${dictionary_link:d9}");

        boxes.put("box1", new RepositoryResource(API, BOX_TYPE, new GenericResource.Metadata("box1"), box1Spec));
        boxes.put("box2", new RepositoryResource(API, BOX_TYPE, new GenericResource.Metadata("box2"), box2Spec));

        var schemaContext = new SchemaContext(
                SCHEMA,
                boxes,
                dictionaries,
                schemaValidationContext
        );

        var validator = new DictionaryLinkValidator(schemaContext);
        validator.validateLinks();
        assertTrue(schemaValidationContext.getResource("box1").isInvalid());
        assertTrue(schemaValidationContext.getResource("box1").isInvalid());

        Map<String, List<LinkErrorMessage>> errors = schemaValidationContext.getReport().getLinkErrorMessages();
        List<LinkErrorMessage> box1Errors = errors.get("box1");

        assertNotNull(box1Errors);
        assertEquals(1, box1Errors.size());
        assertEquals(new DictionaryLink("box1", "null").getContent(), box1Errors.get(0).getLinkContent());

        List<LinkErrorMessage> box2Errors = errors.get("box2");

        assertEquals(new DictionaryLink("box2", "d9").getContent(), box2Errors.get(0).getLinkContent());
    }

    @Test
    void testValidationOfNestedStructure() throws IOException {
        var schemaValidationContext = new SchemaValidationContext();
        YAMLMapper mapper = new YAMLMapper();

        Object box1Spec = mapper.readValue(new File(LINKS_FILE), Object.class);

        Map<String, RepositoryResource> boxes = new HashMap<>();
        boxes.put("box1", new RepositoryResource(API, BOX_TYPE, new GenericResource.Metadata("box1"), box1Spec));

        var schemaContext = new SchemaContext(
                SCHEMA,
                boxes,
                dictionaries,
                schemaValidationContext
        );

        var validator = new DictionaryLinkValidator(schemaContext);
        validator.validateLinks();
        Map<String, List<LinkErrorMessage>> errors = schemaValidationContext.getReport().getLinkErrorMessages();
        List<LinkErrorMessage> box1Errors = errors.get("box1");

        assertNotNull(box1Errors);

        var box1ErrorsContents = box1Errors.stream()
                .map(LinkErrorMessage::getLinkContent)
                .collect(Collectors.toUnmodifiableSet());

        assertEquals(2, box1ErrorsContents.size());
        assertTrue(box1ErrorsContents.contains(new DictionaryLink("box1", "invalid1").getContent()));
        assertTrue(box1ErrorsContents.contains(new DictionaryLink("box1", "invalid2").getContent()));
    }
}
