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
import com.exactpro.th2.validator.SchemaContext;
import com.exactpro.th2.validator.SchemaValidationContext;
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;
import com.exactpro.th2.validator.model.BoxesRelation;
import com.exactpro.th2.validator.model.Th2Spec;
import com.exactpro.th2.validator.model.link.Endpoint;
import com.exactpro.th2.validator.model.link.IdentifiableLink;
import com.exactpro.th2.validator.model.link.MessageLink;
import com.exactpro.th2.validator.model.pin.GrpcClientPin;
import com.exactpro.th2.validator.model.pin.LinkToEndpoint;
import com.exactpro.th2.validator.model.pin.MqSubscriberPin;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static com.exactpro.th2.validator.util.ResourceUtils.collectAllBoxes;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;

public final class LinksValidator {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SchemaValidationContext validationContext;

    private final Map<String, Map<String, RepositoryResource>> repositoryMap;

    public LinksValidator(SchemaValidationContext validationContext,
                          Map<String, Map<String, RepositoryResource>> repositoryMap) {
        this.validationContext = validationContext;
        this.repositoryMap = repositoryMap;
    }

    public void validateLinks(String schemaName) {
        Map<String, RepositoryResource> boxesMap = collectAllBoxes(repositoryMap);
        Collection<RepositoryResource> boxes = boxesMap.values();

        BoxesRelation links = arrangeBoxLinks(boxes);
        Map<String, RepositoryResource> dictionaries = repositoryMap.get(ResourceType.Th2Dictionary.kind());

        var schemaContext = new SchemaContext(
                schemaName,
                boxesMap,
                dictionaries,
                validationContext
        );

        var mqLinkValidator = new MqLinkValidator(schemaContext);
        var grpcLinkValidator = new GrpcLinkValidator(schemaContext);
        var dictionaryLinkValidator = new DictionaryLinkValidator(schemaContext);

        removeDuplicateLinks(links);
        removeLinksWithSameEndpoints(links);

        for (MessageLink mqLink : links.getRouterMq()) {
            mqLinkValidator.validateLink(mqLink);
        }

        for (MessageLink grpcLink : links.getRouterGrpc()) {
            grpcLinkValidator.validateLink(grpcLink);
        }

        dictionaryLinkValidator.validateLinks();
    }

    public void removeDuplicatePins() {
        Map<String, RepositoryResource> boxesMap = collectAllBoxes(repositoryMap);
        var pinsValidator = new PinsValidator(validationContext, boxesMap.values());
        pinsValidator.removeDuplicatePins();
    }

    /**
     * concentrating all links dispersed in boxes into one, easily navigable object
     *
     * @param boxes Collection
     * @return BoxesRelation
     */
    private BoxesRelation arrangeBoxLinks(Collection<RepositoryResource> boxes) {
        var boxesRelation = new BoxesRelation();

        for (var box : boxes) {
            final String boxName = box.getMetadata().getName();
            try {
                Th2Spec boxSpec = mapper.convertValue(box.getSpec(), Th2Spec.class);

                List<MqSubscriberPin> subscribers = requireNonNullElse(
                        boxSpec.getMqSubscribers(), emptyList()
                );
                for (var sub : subscribers) {
                    List<LinkToEndpoint> linkTo = requireNonNullElse(sub.getLinkTo(), emptyList());
                    String pinName = sub.getName();
                    linkTo.forEach(startPoint -> boxesRelation.addToMq(new MessageLink(
                            startPoint.mqLinkMetaData(boxName, pinName),
                            new Endpoint(startPoint.getBox(), startPoint.getPin()),
                            new Endpoint(boxName, pinName)
                    )));
                }

                List<GrpcClientPin> clients = requireNonNullElse(
                        boxSpec.getGrpcClientPins(), emptyList()
                );
                for (var client : clients) {
                    List<LinkToEndpoint> linkTo = requireNonNullElse(client.getLinkTo(), emptyList());
                    String pinName = client.getName();
                    linkTo.forEach(destination -> boxesRelation.addToGrpc(new MessageLink(
                            destination.grpcLinkMetaData(boxName, pinName),
                            new Endpoint(boxName, pinName),
                            new Endpoint(destination.getBox(), destination.getPin())
                    )));
                }
            } catch (Exception e) {
                String message = String.format("Exception occurred during the scan of box links: %s",
                        e.getMessage());
                validationContext.setInvalidResource(boxName);
                validationContext.addBoxResourceErrorMessages(new BoxResourceErrorMessage(
                        boxName,
                        message
                ));
            }

        }

        return boxesRelation;
    }

    private <T extends IdentifiableLink> List<T> distinctLinks(List<T> links) {
        Set<String> linkContents = new HashSet<>();
        List<T> distinctLinks = new ArrayList<>();
        for (var link : links) {
            boolean sameContent = !linkContents.add(link.getContent());
            if (sameContent) {
                validationContext.addLinkErrorMessage(link.errorMessage(
                        "Link is the same as other link(s). Ignoring"));
            } else {
                distinctLinks.add(link);
            }
        }

        return distinctLinks;
    }

    private void removeDuplicateLinks(BoxesRelation links) {
        links.setRouterMq(
                distinctLinks(links.getRouterMq()));
        links.setRouterGrpc(
                distinctLinks(links.getRouterGrpc()));
    }

    private List<MessageLink> linksWithDifferingEndpoints(List<MessageLink> links) {
        List<MessageLink> validLinks = new ArrayList<>();
        for (var link : links) {
            if (link.getTo().getBox().equals(link.getFromBox())) {
                validationContext.addLinkErrorMessage(link.errorMessage(
                        "\"from\" box name cannot be the same as \"to\" box name. Ignoring"));
            } else {
                validLinks.add(link);
            }
        }
        return validLinks;
    }

    private void removeLinksWithSameEndpoints(BoxesRelation links) {
        links.setRouterMq(
                linksWithDifferingEndpoints(links.getRouterMq()));
        links.setRouterGrpc(
                linksWithDifferingEndpoints(links.getRouterGrpc()));
    }
}
