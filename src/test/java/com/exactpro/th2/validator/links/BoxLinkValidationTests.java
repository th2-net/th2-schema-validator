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

package com.exactpro.th2.validator.links;

import com.exactpro.th2.infrarepo.ResourceType;
import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.SchemaValidationContext;
import com.exactpro.th2.validator.SchemaValidator;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.exactpro.th2.validator.model.Th2Spec;
import com.exactpro.th2.validator.model.pin.LinkToEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.exactpro.th2.validator.links.enums.ValidationStatus.INVALID;
import static com.exactpro.th2.validator.util.ResourceUtils.collectResources;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class BoxLinkValidationTests {
    private static final ObjectMapper mapper = new YAMLMapper();

    private static final String SCHEMA = "schema";

    private static final String PATH = "src/test/resources/linksTest/";

    private static final String EXTENSION = ".yml";

    private static final String ACT = "act-fix";

    private static final String CHECK1 = "check1";

    private static final String CODEC = "codec-fix";

    private static final String RPT = "rpt-data-provider";

    private static final File ACT_FILE = new File(PATH + ACT + EXTENSION);

    private static final File CHECK1_FILE = new File(PATH + CHECK1 + EXTENSION);

    private static final File CODEC_FILE = new File(PATH + CODEC + EXTENSION);

    private static final File RPT_FILE = new File(PATH + RPT + EXTENSION);

    private Map<String, Map<String, RepositoryResource>> initRepositoryMap() throws IOException {
        Map<String, Map<String, RepositoryResource>> repositoryMap = new HashMap<>();
        Map<String, RepositoryResource> nameToBox = Map.of(
                ACT, mapper.readValue(ACT_FILE, RepositoryResource.class),
                CHECK1, mapper.readValue(CHECK1_FILE, RepositoryResource.class),
                CODEC, mapper.readValue(CODEC_FILE, RepositoryResource.class)
        );
        Map<String, RepositoryResource> nameToCore = Map.of(
                RPT, mapper.readValue(RPT_FILE, RepositoryResource.class)
        );
        repositoryMap.put(ResourceType.Th2Box.kind(), nameToBox);
        repositoryMap.put(ResourceType.Th2CoreBox.kind(), nameToCore);

        return repositoryMap;
    }

    /**
     * Tests validation cases of links which:
     * are duplicate,
     * have same endpoints('from' and 'to' boxes),
     * include nonexistent resource,
     * include pin(s) which have the opposite publish/subscribe attribute,
     * or include the same pin types of endpoints (subscriber -> subscriber or client -> client)
     */
    @Test
    void testValidation() throws IOException {
        Map<String, Map<String, RepositoryResource>> repoMap = initRepositoryMap();
        var validationContext = new SchemaValidationContext();
        var validator = new LinksValidator(validationContext, repoMap);
        validator.validateLinks(SCHEMA);

        assertEquals(2, validationContext.getInvalidResources().size());
        assertEquals(INVALID, validationContext.getResource(ACT).getStatus());
        assertEquals(INVALID, validationContext.getResource(RPT).getStatus());

        List<LinkErrorMessage> errors = validationContext.getReport().getLinkErrorMessages();
        Set<String> actualInvalidLinks = collectLinkContents(errors);
        Set<String> expectedInvalidLinks = Set.of(
                linkContent(CODEC, "out_codec_decode", ACT, "from_codec"),
                linkContent(ACT, "to_check1", "fake", "to_act"),
                linkContent(ACT, "to_check1", ACT, "to_check1"),
                linkContent(ACT, "to_check1", CHECK1, "client"),
                linkContent("fake", "fake_pin", RPT, "from_codec"),
                linkContent(CODEC, "out_codec_general_decode", RPT, "from_codec"),
                linkContent(CODEC, "in_codec_encode", RPT, "from_codec"),
                linkContent(RPT, "not_exist", RPT, "from_codec")
        );

        assertEquals(expectedInvalidLinks, actualInvalidLinks);
    }

    @Test
    void testRemoveInvalidLinks() throws IOException {
        Map<String, Map<String, RepositoryResource>> repoMap = initRepositoryMap();
        Map<String, RepositoryResource> boxMap = collectResources(
                repoMap,
                ResourceType.Th2Box.kind(),
                ResourceType.Th2CoreBox.kind(),
                ResourceType.Th2Job.kind()
        );
        var validationContext = new SchemaValidationContext();
        var validator = new LinksValidator(validationContext, repoMap);
        validator.validateLinks(SCHEMA);
        SchemaValidator.removeInvalidLinks(validationContext, boxMap.values());

        var actSpec = mapper.convertValue(boxMap.get(ACT).getSpec(), Th2Spec.class);

        assertNull(actSpec.getMqSubscribers().get(0).getLinkTo());
        List<LinkToEndpoint> expectedActLinkTo = List.of(
                new LinkToEndpoint(CHECK1, "server")
        );

        List<LinkToEndpoint> actualActLinkTo = actSpec.getGrpcClientPins().get(0).getLinkTo();
        assertEquals(expectedActLinkTo, actualActLinkTo);

        var rptSpec = mapper.convertValue(boxMap.get(RPT).getSpec(), Th2Spec.class);

        List<LinkToEndpoint> expectedRptLinkTo = List.of(
                new LinkToEndpoint(CODEC, "out_codec_general_decode")
        );
        List<LinkToEndpoint> actualRptLinkTo = rptSpec.getMqSubscribers().get(0).getLinkTo();
        assertEquals(expectedRptLinkTo, actualRptLinkTo);

    }

    @Test
    void testSpecNullSafety() throws IOException {
        final var noSpecBoxName = "NoSpecBox";
        final var nullLinkToSectionBoxName = "NullLinkToSectionBox";
        final var noSpecBoxFile = new File(
                format("src/test/resources/%s.yml", noSpecBoxName));
        final var nullLinkToFile = new File(
                format("src/test/resources/linksTest/%s.yml", nullLinkToSectionBoxName));
        var noSpecBox = mapper.readValue(noSpecBoxFile, RepositoryResource.class);
        var nullLinkToBox = mapper.readValue(nullLinkToFile, RepositoryResource.class);

        Map<String, Map<String, RepositoryResource>> repoMap = Map.of(
                ResourceType.Th2Box.kind(),
                Map.of(noSpecBoxName, noSpecBox,
                        nullLinkToSectionBoxName, nullLinkToBox)
        );
        Map<String, RepositoryResource> boxMap = collectResources(
                repoMap,
                ResourceType.Th2Box.kind(),
                ResourceType.Th2CoreBox.kind(),
                ResourceType.Th2Job.kind()
        );
        var validationContext = new SchemaValidationContext();
        var validator = new LinksValidator(validationContext, repoMap);
        assertDoesNotThrow(() -> validator.validateLinks(SCHEMA));
        assertDoesNotThrow(() -> SchemaValidator.removeInvalidLinks(validationContext, boxMap.values()));

        assertEquals(Set.of(noSpecBoxName), validationContext.getInvalidResources());
    }

    private Set<String> collectLinkContents(List<LinkErrorMessage> errors) {
        return errors.stream()
                .map(LinkErrorMessage::getLinkContent)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String linkContent(String fromBox, String fromPin, String toBox, String toPin) {
        return format("FROM %s:%s TO %s:%s",
                fromBox, fromPin,
                toBox, toPin);
    }

}
