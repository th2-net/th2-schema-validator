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

package com.exactpro.th2.validator;

import com.exactpro.th2.infrarepo.ResourceType;
import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.infrarepo.settings.RepositorySettingsResource;
import com.exactpro.th2.validator.boxes.BoxesValidator;
import com.exactpro.th2.validator.links.LinksValidator;
import com.exactpro.th2.validator.model.link.MessageLink;
import com.exactpro.th2.validator.model.pin.*;
import com.exactpro.th2.validator.util.SourceHashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static com.exactpro.th2.validator.links.enums.ValidationStatus.VALID;
import static com.exactpro.th2.validator.util.ResourceUtils.*;

@SuppressWarnings("unchecked")
public class SchemaValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static SchemaValidationContext validate(String schemaName,
                                                   String namespacePrefix,
                                                   String storageServiceBaseUrl,
                                                   RepositorySettingsResource settingsResource,
                                                   Map<String, Map<String, RepositoryResource>> repositoryMap) {
        var schemaValidationContext = new SchemaValidationContext();
        try {
            Map<String, RepositoryResource> boxesMap = collectResources(
                    repositoryMap,
                    ResourceType.Th2Box.kind(),
                    ResourceType.Th2CoreBox.kind(),
                    ResourceType.Th2Job.kind(),
                    ResourceType.Th2Mstore.kind(),
                    ResourceType.Th2Estore.kind()
            );
            String namespace = namespacePrefix + schemaName;

            var boxesValidator = new BoxesValidator(schemaValidationContext, boxesMap);
            boxesValidator.detectUrlPathsConflicts();
            boxesValidator.validateSecrets(namespace);

            var linksValidator = new LinksValidator(schemaValidationContext, repositoryMap);
            linksValidator.removeDuplicatePins();
            linksValidator.validateLinks(schemaName);

            var booksValidator = new BookNamesValidator(
                    settingsResource,
                    storageServiceBaseUrl,
                    schemaValidationContext,
                    boxesMap
            );
            booksValidator.validate();
        } catch (Exception e) {
            schemaValidationContext.addExceptionMessage(e.getMessage());
        }
        return schemaValidationContext;
    }

    private static void setValidLinkToSections(
            List<Map<String, Object>> pins, Map<String, List<LinkToEndpoint>> validLinkToMapping) {

        for (var pin : pins) {
            var pinName = (String) pin.get("name");
            if (validLinkToMapping.containsKey(pinName)) {
                pin.put("linkTo", validLinkToMapping.get(pinName));
            }
        }
    }

    public static void removeInvalidLinks(SchemaValidationContext validationContext,
                                          Collection<RepositoryResource> boxes) throws JsonProcessingException {

        for (var box : boxes) {
            String resName = box.getMetadata().getName();
            ResourceValidationContext resValidationContext = validationContext.getResource(resName);
            if (resValidationContext == null || resValidationContext.getStatus().equals(VALID)) {
                continue;
            }
            var spec = (Map<String, Object>) box.getSpec();
            if (spec == null) {
                continue;
            }

            Map<String, Object> pinSpec = getSection(spec, "pins");
            Map<String, Object> mq = getSection(pinSpec, "mq");
            List<Map<String, Object>> subscribersSection = getSectionArray(mq, "subscribers");

            if (subscribersSection != null) {
                Set<String> linkFulSubs = getLinkFulPinNames(subscribersSection);
                List<MessageLink> validMqLinks = resValidationContext.getValidMqLinks();
                Map<String, List<LinkToEndpoint>> validMqSubsLinkTo = new HashMap<>();
                validMqLinks.forEach(msgLink ->
                        validMqSubsLinkTo.computeIfAbsent(msgLink.getToPin(),
                                sub -> new ArrayList<>()).add(msgLink.mqLinkToEndpoint()));
                /* we have to remove LinkTos from all pins which have no valid links */
                linkFulSubs.forEach(sub -> validMqSubsLinkTo.putIfAbsent(sub, null));
                setValidLinkToSections(subscribersSection, validMqSubsLinkTo);
            }

            Map<String, Object> grpc = getSection(pinSpec, "grpc");
            List<Map<String, Object>> clientSection = getSectionArray(grpc, "client");

            if (clientSection != null) {
                Set<String> linkFulClients = getLinkFulPinNames(clientSection);
                List<MessageLink> validGrpcLinks = resValidationContext.getValidGrpcLinks();
                Map<String, List<LinkToEndpoint>> validGrpcClientsLinkTo = new HashMap<>();
                validGrpcLinks.forEach(msgLink ->
                        validGrpcClientsLinkTo.computeIfAbsent(msgLink.getFromPin(),
                                client -> new ArrayList<>()).add(msgLink.grpcLinkToEndpoint()));

                linkFulClients.forEach(client -> validGrpcClientsLinkTo.putIfAbsent(client, null));
                setValidLinkToSections(clientSection, validGrpcClientsLinkTo);
            }

            String specStr = mapper.writeValueAsString(spec);
            box.setSourceHash(SourceHashUtil.digest(specStr));
        }
    }

    private static Set<String> getLinkFulPinNames(List<Map<String, Object>> pins) {
        Set<String> linkFulSubs = new HashSet<>();
        for (var pin : pins) {
            List<Map<String, Object>> linkTo = getSectionArray(pin, "linkTo");
            if (linkTo != null) {
                linkFulSubs.add((String) pin.get("name"));
            }
        }
        return linkFulSubs;
    }
}
