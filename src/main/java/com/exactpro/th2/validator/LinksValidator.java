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
import com.exactpro.th2.infrarepo.repo.RepositoryResource;
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

class LinksValidator {
    private static final ObjectMapper mapper = new ObjectMapper();

    static void validateLinks(String schemaName,
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

    /**
     * concentrating all links dispersed in boxes into one, easily navigable object
     * @param boxes Collection
     * @return BoxesRelation
     */
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
}
