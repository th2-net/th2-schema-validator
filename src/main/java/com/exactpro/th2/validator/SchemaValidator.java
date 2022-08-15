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

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import com.exactpro.th2.validator.model.link.MessageLink;
import com.exactpro.th2.validator.model.pin.*;
import com.exactpro.th2.validator.util.SecretsUtils;
import com.exactpro.th2.validator.util.SourceHashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.*;

import static com.exactpro.th2.validator.UrlPathConflicts.detectUrlPathsConflicts;
import static com.exactpro.th2.validator.enums.ValidationStatus.VALID;
import static com.exactpro.th2.validator.util.ResourceUtils.*;
import static com.exactpro.th2.validator.util.SecretsUtils.extractCustomConfig;
import static com.exactpro.th2.validator.util.SecretsUtils.generateSecretsConfig;

@SuppressWarnings("unchecked")
public class SchemaValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static SchemaValidationContext validate(String schemaName,
                                                   String namespacePrefix,
                                                   Map<String, Map<String, RepositoryResource>> repositoryMap) {
        var schemaValidationContext = new SchemaValidationContext();
        try {
            Map<String, RepositoryResource> boxesMap = collectAllBoxes(repositoryMap);
            Collection<RepositoryResource> boxes = boxesMap.values();

            detectUrlPathsConflicts(schemaValidationContext, boxesMap);
            var pinsValidator = new PinsValidator(boxes, schemaValidationContext);
            pinsValidator.removeDuplicatePins();
            var linksValidator = new LinksValidator(schemaValidationContext);
            linksValidator.validateLinks(schemaName, repositoryMap);
            validateSecrets(schemaName, namespacePrefix, schemaValidationContext, boxes);
        } catch (Exception e) {
            schemaValidationContext.addExceptionMessage(e.getMessage());
            return schemaValidationContext;
        }
        return schemaValidationContext;
    }

    private static void validateSecrets(String schemaName,
                                        String namespacePrefix,
                                        SchemaValidationContext schemaValidationContext,
                                        Collection<RepositoryResource> allBoxes) {
        String namespace = namespacePrefix + schemaName;
        if (SecretsUtils.namespaceNotPresent(namespace)) {
            return;
        }
        Secret secret = SecretsUtils.getCustomSecret(namespace);
        if (secret == null) {
            String errorMessage = String.format("Secret \"secret-custom-config\" is not present in namespace: \"%s\"",
                    namespace);
            schemaValidationContext.addExceptionMessage(errorMessage);
            return;
        }
        Map<String, String> secretData = secret.getData();
        for (var res : allBoxes) {
            Map<String, Object> customConfig = extractCustomConfig(res);
            Set<String> secretsConfig = generateSecretsConfig(customConfig);
            if (!secretsConfig.isEmpty()) {
                for (String secretKey : secretsConfig) {
                    if (secretData == null || !secretData.containsKey(secretKey)) {
                        String resName = res.getMetadata().getName();
                        String errorMessage = String.format("Value \"%s\" from " +
                                "\"secret-custom-config\" is not present in Kubernetes", secretKey);
                        schemaValidationContext.setInvalidResource(resName);
                        schemaValidationContext.addBoxResourceErrorMessages(new BoxResourceErrorMessage(
                                resName,
                                errorMessage
                        ));
                    }
                }
            }
        }
    }

    private static void setValidLinkToSections(
            List<Map<String, Object>> pins, Map<String, List<LinkToEndpoint>> validLinkToMapping) {

        for (var pin : pins) {
            String pinName = (String) pin.get("name");
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
