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
import com.exactpro.th2.validator.model.BoxesRelation;
import com.exactpro.th2.validator.model.Th2Spec;
import com.exactpro.th2.validator.model.link.Endpoint;
import com.exactpro.th2.validator.model.link.IdentifiableLink;
import com.exactpro.th2.validator.model.link.MessageLink;
import com.exactpro.th2.infrarepo.ResourceType;
import com.exactpro.th2.validator.model.pin.*;
import com.exactpro.th2.validator.util.SecretsUtils;
import com.exactpro.th2.validator.util.SourceHashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Secret;

import java.util.*;

import static com.exactpro.th2.validator.UrlPathConflicts.detectUrlPathsConflicts;
import static com.exactpro.th2.validator.enums.ValidationStatus.VALID;
import static com.exactpro.th2.validator.util.SecretsUtils.extractCustomConfig;
import static com.exactpro.th2.validator.util.SecretsUtils.generateSecretsConfig;

@SuppressWarnings("unchecked")
public class SchemaValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static SchemaValidationContext validate(String schemaName,
                                                   String namespacePrefix,
                                                   Map<String, Map<String, RepositoryResource>> repositoryMap) {
        SchemaValidationContext schemaValidationContext = new SchemaValidationContext();
        try {
            detectUrlPathsConflicts(schemaValidationContext, collectAllBoxes(repositoryMap));

            validateLinks(schemaName, schemaValidationContext, repositoryMap);
            validateSecrets(schemaName, namespacePrefix, schemaValidationContext, repositoryMap);
        } catch (Exception e) {
            schemaValidationContext.addExceptionMessage(e.getMessage());
            return schemaValidationContext;
        }
        return schemaValidationContext;
    }

    private static void validateSecrets(String schemaName,
                                        String namespacePrefix,
                                        SchemaValidationContext schemaValidationContext,
                                        Map<String, Map<String, RepositoryResource>> repositoryMap) {
        Map<String, RepositoryResource> boxes = repositoryMap.get(ResourceType.Th2Box.kind());
        Map<String, RepositoryResource> coreBoxes = repositoryMap.get(ResourceType.Th2CoreBox.kind());
        String namespace = namespacePrefix + schemaName;
        List<RepositoryResource> allBoxes = new ArrayList<>(boxes.values());
        allBoxes.addAll(coreBoxes.values());
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

    private static <T extends IdentifiableLink> List<T> distinctLinks(
            List<T> links,
            SchemaValidationContext schemaValidationContext) {

        Set<String> linkContents = new HashSet<>();
        List<T> distinctLinks = new ArrayList<>();
        for (var link : links) {
            boolean sameContent = !linkContents.add(link.getContent());
            if (sameContent) {
                schemaValidationContext.addLinkErrorMessage(link.getResourceName(), link.errorMessage(
                        "Link is the same as other link(s). Ignoring"));
            } else {
                distinctLinks.add(link);
            }
        }

        return distinctLinks;
    }

    private static void removeDuplicateLinks(
            BoxesRelation links, SchemaValidationContext schemaValidationContext) {

        links.setRouterMq(
                distinctLinks(links.getRouterMq(), schemaValidationContext));
        links.setRouterGrpc(
                distinctLinks(links.getRouterGrpc(), schemaValidationContext));
    }

    private static List<MessageLink> linksWithDifferingEndpoints(
            List<MessageLink> links, SchemaValidationContext schemaValidationContext) {

        List<MessageLink> validLinks = new ArrayList<>();
        for (var link : links) {
            if (link.getTo().getBox().equals(link.getFromBox())) {
                schemaValidationContext.addLinkErrorMessage(link.getResourceName(), link.errorMessage(
                        "\"from\" box name cannot be the same as \"to\" box name. Ignoring"));
            } else {
                validLinks.add(link);
            }
        }
        return validLinks;
    }

    private static void removeLinksWithSameEndpoints(
            BoxesRelation links, SchemaValidationContext schemaValidationContext) {

        links.setRouterMq(
                linksWithDifferingEndpoints(links.getRouterMq(), schemaValidationContext));
        links.setRouterGrpc(
                linksWithDifferingEndpoints(links.getRouterGrpc(), schemaValidationContext));
    }

    private static void validateLinks(String schemaName,
                                      SchemaValidationContext schemaValidationContext,
                                      Map<String, Map<String, RepositoryResource>> repositoryMap) {

        Map<String, RepositoryResource> boxesMap = collectAllBoxes(repositoryMap);
        Collection<RepositoryResource> boxes = boxesMap.values();

        BoxesRelation links = arrangeBoxLinks(boxes);
        Map<String, RepositoryResource> dictionaries = repositoryMap.get(ResourceType.Th2Dictionary.kind());

        SchemaContext schemaContext = new SchemaContext(
                schemaName,
                boxesMap,
                dictionaries,
                schemaValidationContext
        );

        var mqLinkValidator = new MqLinkValidator(schemaContext);
        var grpcLinkValidator = new GrpcLinkValidator(schemaContext);
        var dictionaryLinkValidator = new DictionaryLinkValidator(schemaContext);

        removeDuplicateLinks(links, schemaValidationContext);
        removeLinksWithSameEndpoints(links, schemaValidationContext);

        for (MessageLink mqLink : links.getRouterMq()) {
            mqLinkValidator.validateLink(mqLink);
        }

        for (MessageLink grpcLink : links.getRouterGrpc()) {
            grpcLinkValidator.validateLink(grpcLink);
        }

        dictionaryLinkValidator.validateLinks();
    }

    private static BoxesRelation arrangeBoxLinks(Collection<RepositoryResource> boxes) {
        var boxesRelation = new BoxesRelation();

        for (var box : boxes) {
            String boxName = box.getMetadata().getName();
            Th2Spec boxSpec = mapper.convertValue(box.getSpec(), Th2Spec.class);

            List<MqSubscriberPin> subscribers = boxSpec.getMqSubscribers();
            for (var sub : subscribers) {
                List<LinkToEndpoint> linkTo = sub.getLinkTo();
                String pinName = sub.getName();
                linkTo.forEach(startPoint -> boxesRelation.addToMq(new MessageLink(
                        startPoint.mqLinkMetaData(boxName, pinName),
                        new Endpoint(startPoint.getBox(), startPoint.getPin()),
                        new Endpoint(boxName, pinName)
                )));
            }

            List<GrpcClientPin> clients = boxSpec.getGrpcClientPins();

            for (var client : clients) {
                List<LinkToEndpoint> linkTo = client.getLinkTo();
                String pinName = client.getName();
                linkTo.forEach(destination -> boxesRelation.addToGrpc(new MessageLink(
                        destination.grpcLinkMetaData(boxName, pinName),
                        new Endpoint(boxName, pinName),
                        new Endpoint(destination.getBox(), destination.getPin())
                )));
            }

        }

        return boxesRelation;
    }

    private static Map<String, RepositoryResource> collectAllBoxes(
            Map<String, Map<String, RepositoryResource>> repositoryMap
    ) {
        Map<String, RepositoryResource> boxes = repositoryMap.get(ResourceType.Th2Box.kind());
        Map<String, RepositoryResource> coreBoxes = repositoryMap.get(ResourceType.Th2CoreBox.kind());
        Map<String, RepositoryResource> allBoxes = new HashMap<>(boxes);
        allBoxes.putAll(coreBoxes);
        return allBoxes;
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

    private static Map<String, Object> getSection(Map<String, Object> parent, String sectionName) {
        return parent != null ? (Map<String, Object>) parent.get(sectionName) : null;
    }

    private static List<Map<String, Object>> getSectionArray(Map<String, Object> parent, String sectionName) {
        return parent != null ? (List<Map<String, Object>>) parent.get(sectionName) : null;
    }
}
