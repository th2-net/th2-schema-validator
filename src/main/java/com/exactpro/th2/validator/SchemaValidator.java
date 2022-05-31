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
import com.exactpro.th2.validator.model.Th2LinkSpec;
import com.exactpro.th2.validator.model.link.DictionaryLink;
import com.exactpro.th2.validator.model.link.IdentifiableLink;
import com.exactpro.th2.validator.model.link.MessageLink;
import com.exactpro.th2.infrarepo.ResourceType;
import com.exactpro.th2.validator.model.link.MultiDictionaryLink;
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
            List<T> links, SchemaValidationContext schemaValidationContext) {
        Set<String> linkNames = new HashSet<>();
        Set<String> linkContents = new HashSet<>();
        List<T> distinctLinks = new ArrayList<>();
        for (var link : links) {
            boolean sameName = !linkNames.add(link.getName());
            boolean sameContent = !linkContents.add(link.getContent());
            if (sameName && sameContent) {
                schemaValidationContext.addLinkErrorMessage(link.getName(), link.errorMessage(
                        "Link has the same name and same content as the other link(s). Removing"));

            } else if (sameName) {
                schemaValidationContext.addLinkErrorMessage(link.getName(), link.errorMessage(
                        "Link has the same name as the other link(s) but different content"
                ));
                distinctLinks.add(link);
            } else if (sameContent) {
                schemaValidationContext.addLinkErrorMessage(link.getName(), link.errorMessage(
                        "Link has the same content as other link(s) but different name. Removing"));
            } else {
                distinctLinks.add(link);
            }
        }

        return distinctLinks;
    }

    private static void removeDuplicateLinks(Th2LinkSpec spec, SchemaValidationContext schemaValidationContext) {
        spec.getBoxesRelation().setRouterMq(
                distinctLinks(spec.getBoxesRelation().getRouterMq(), schemaValidationContext));
        spec.getBoxesRelation().setRouterGrpc(
                distinctLinks(spec.getBoxesRelation().getRouterGrpc(), schemaValidationContext));
        spec.setDictionariesRelation(
                distinctLinks(spec.getDictionariesRelation(), schemaValidationContext));
        spec.setMultiDictionaryRelation(
                distinctLinks(spec.getMultiDictionaryRelation(), schemaValidationContext));
    }

    private static List<MessageLink> linksWithDifferingEndpoints(
            List<MessageLink> links, SchemaValidationContext schemaValidationContext) {
        List<MessageLink> validLinks = new ArrayList<>();
        for (var link : links) {
            if (link.getTo().getBox().equals(link.getFrom().getBox())) {
                String message = "\"from\" box name cannot be the same as \"to\" box name";
                schemaValidationContext.addLinkErrorMessage(link.getName(),
                        link.errorMessage(message));
            } else {
                validLinks.add(link);
            }
        }
        return validLinks;
    }

    private static void removeLinksWithSameEndpoints(
            Th2LinkSpec spec, SchemaValidationContext schemaValidationContext) {
        BoxesRelation boxesRelation = spec.getBoxesRelation();
        boxesRelation.setRouterMq(
                linksWithDifferingEndpoints(boxesRelation.getRouterMq(), schemaValidationContext));
        boxesRelation.setRouterGrpc(
                linksWithDifferingEndpoints(boxesRelation.getRouterGrpc(), schemaValidationContext));
    }

    private static void validateLinks(String schemaName,
                                      SchemaValidationContext schemaValidationContext,
                                      Map<String, Map<String, RepositoryResource>> repositoryMap) {
        Collection<RepositoryResource> links = repositoryMap.get(ResourceType.Th2Link.kind()).values();
        Map<String, RepositoryResource> dictionaries = repositoryMap.get(ResourceType.Th2Dictionary.kind());

        SchemaContext schemaContext = new SchemaContext(
                schemaName,
                collectAllBoxes(repositoryMap),
                dictionaries,
                schemaValidationContext
        );

        var mqLinkValidator = new MqLinkValidator(schemaContext);
        var grpcLinkValidator = new GrpcLinkValidator(schemaContext);
        var dictionaryLinkValidator = new DictionaryLinkValidator(schemaContext);
        var multiDictionaryLinkValidator = new MultiDictionaryLinkValidator(schemaContext);


        for (RepositoryResource linkRes : links) {
            Th2LinkSpec spec = mapper.convertValue(linkRes.getSpec(), Th2LinkSpec.class);
            removeDuplicateLinks(spec, schemaValidationContext);
            removeLinksWithSameEndpoints(spec, schemaValidationContext);

            for (MessageLink mqLink : spec.getBoxesRelation().getRouterMq()) {
                mqLinkValidator.validateLink(linkRes, mqLink);
            }
            for (MessageLink grpcLink : spec.getBoxesRelation().getRouterGrpc()) {
                grpcLinkValidator.validateLink(linkRes, grpcLink);
            }
            for (DictionaryLink dictionaryLink : spec.getDictionariesRelation()) {
                dictionaryLinkValidator.validateLink(linkRes, dictionaryLink);
            }
            for (MultiDictionaryLink multiDictionaryLink : spec.getMultiDictionaryRelation()) {
                multiDictionaryLinkValidator.validateLink(linkRes, multiDictionaryLink);
            }
        }
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

    public static void removeInvalidLinks(SchemaValidationContext validationContext,
                                          Map<String, RepositoryResource> linkResources)
            throws JsonProcessingException {
        for (var entry : linkResources.entrySet()) {
            RepositoryResource resource = entry.getValue();
            String linkResName = entry.getKey();
            Th2LinkSpec spec = mapper.convertValue(resource.getSpec(), Th2LinkSpec.class);
            ResourceValidationContext resourceValidationContext = validationContext.getResource(linkResName);
            if (resourceValidationContext == null || resourceValidationContext.getStatus().equals(VALID)) {
                continue;
            }
            BoxesRelation boxesRelation = spec.getBoxesRelation();
            boxesRelation.setRouterMq(resourceValidationContext.getValidMqLinks());
            boxesRelation.setRouterGrpc(resourceValidationContext.getValidGrpcLinks());
            spec.setDictionariesRelation(resourceValidationContext.getValidDictionaryLinks());
            spec.setMultiDictionaryRelation(resourceValidationContext.getValidMultiDictionaryLinks());
            resource.setSpec(spec);

            String specStr = mapper.writeValueAsString(spec);
            resource.setSourceHash(SourceHashUtil.digest(specStr));
        }
    }
}
