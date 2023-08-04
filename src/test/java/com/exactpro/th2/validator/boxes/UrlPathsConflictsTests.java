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

package com.exactpro.th2.validator.boxes;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.SchemaValidationContext;
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlPathsConflictsTests {
    private static final String PATH = "src/test/resources/urlPathsTest/";

    private static final ObjectMapper mapper = new YAMLMapper();

    private Map<String, RepositoryResource> boxMap() throws IOException {
        RepositoryResource box1 = getBox("box1.yml");
        RepositoryResource box2 = getBox("box2.yml");
        RepositoryResource box3 = getBox("box3.yml");
        return Map.of(
                "box1", box1,
                "box2", box2,
                "box3", box3
        );
    }

    private RepositoryResource getBox(String name) throws IOException {
        return mapper.readValue(new File(PATH + name), RepositoryResource.class);
    }

    @Test
    void testUrlPathConflictsDetection() throws IOException {
        var validationContext = new SchemaValidationContext();
        var boxMap = boxMap();
        new BoxesValidator(validationContext, boxMap).detectUrlPathsConflicts();
        String errorMsgs = validationContext.getReport().getBoxResourceErrorMessages().stream()
                .map(BoxResourceErrorMessage::toPrintableMessage)
                .collect(Collectors.joining("\n"));

        /* 3 as box1->box2, box2->box3, box1->box3 all relations have URL1 conflict */
        assertEquals(2, StringUtils.countMatches(errorMsgs, "Contains duplicated url paths"));
        assertEquals(3, StringUtils.countMatches(errorMsgs, "URL1"));
        assertEquals(1, StringUtils.countMatches(errorMsgs, "URL2"));
        assertEquals(1, StringUtils.countMatches(errorMsgs, "URL4"));
    }

    @Test
    void testNullSafety() throws IOException {
        var validationContext = new SchemaValidationContext();
        var boxMap = Map.of("noIngress", getBox("BoxWithNoIngress.yml"));
        assertDoesNotThrow(() -> new BoxesValidator(validationContext, boxMap).detectUrlPathsConflicts());
    }
}
