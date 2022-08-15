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
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.exactpro.th2.validator.util.ResourceUtils.getSection;
import static com.exactpro.th2.validator.util.ResourceUtils.getSectionArray;

@SuppressWarnings("unchecked")
class PinsValidator {
    private static final Logger logger = LoggerFactory.getLogger(SchemaValidator.class);

    private final Collection<RepositoryResource> boxes;

    private final SchemaValidationContext validationContext;

    PinsValidator(Collection<RepositoryResource> boxes, SchemaValidationContext validationContext) {
        this.boxes = Collections.unmodifiableCollection(boxes);
        this.validationContext = validationContext;
    }

    void removeDuplicatePins() {
        for (var box : boxes) {
            final String boxName = box.getMetadata().getName();
            try {
                var spec = (Map<String, Object>) box.getSpec();
                Map<String, Object> pinSpec = getSection(spec, "pins");
                Map<String, Object> mq = getSection(pinSpec, "mq");
                Map<String, Object> grpc = getSection(pinSpec, "grpc");

                putUniquePinsInSection(mq, "subscribers", boxName);

                putUniquePinsInSection(mq, "publishers", boxName);

                putUniquePinsInSection(grpc, "client", boxName);

                putUniquePinsInSection(grpc, "server", boxName);
            } catch (Exception e) {
                String message = String.format("Exception occurred during the removal of duplicate pins: %s",
                        e.getMessage());
                validationContext.setInvalidResource(boxName);
                validationContext.addBoxResourceErrorMessages(new BoxResourceErrorMessage(
                        boxName,
                        message
                ));
            }
        }
    }

    private void putUniquePinsInSection(
            Map<String, Object> section, String subSectionName, String boxName) {

        if (section == null) {
            return;
        }

        List<Map<String, Object>> uniquePins = uniquePinsInArraySection(
                getSectionArray(section, subSectionName),
                boxName
        );

        section.put(subSectionName, uniquePins);
    }

    private List<Map<String, Object>> uniquePinsInArraySection(
            List<Map<String, Object>> arraySection, String boxName) {

        if (arraySection == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> uniquePins = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Map<String, Object> pin : arraySection) {
            String pinName = (String) pin.get("name");
            if (!names.add(pinName)) {
                logger.warn("Detected duplicated pin: \"{}\" in \"{}\". will be ignored",
                        pinName, boxName);
            } else {
                uniquePins.add(pin);
            }
        }
        return uniquePins;
    }
}
