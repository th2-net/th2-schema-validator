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

import com.exactpro.th2.validator.model.Th2LinkSpec;
import com.exactpro.th2.validator.model.link.DictionaryLink;
import com.exactpro.th2.validator.model.link.MessageLink;
import com.exactpro.th2.infrarepo.RepositoryResource;
import com.exactpro.th2.infrarepo.ResourceType;
import com.exactpro.th2.validator.util.SecretsUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.*;

import static com.exactpro.th2.validator.util.SecretsUtils.extractCustomConfig;
import static com.exactpro.th2.validator.util.SecretsUtils.generateSecretsConfig;

public class SchemaValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static SchemaValidationContext validate(String schemaName,
                                                   Map<String, Map<String, RepositoryResource>> repositoryMap) {
        SchemaValidationContext schemaValidationContext = new SchemaValidationContext();

        try {
            validateLinks(schemaName, schemaValidationContext, repositoryMap);
            validateSecrets(schemaName, schemaValidationContext, repositoryMap);
        } catch (Exception e) {
            schemaValidationContext.setException(e);
            return schemaValidationContext;
        }
        return schemaValidationContext;
    }

    private static void validateSecrets(String schemaName,
                                        SchemaValidationContext schemaValidationContext,
                                        Map<String, Map<String, RepositoryResource>> repositoryMap) {
        Map<String, RepositoryResource> boxes = repositoryMap.get(ResourceType.Th2Box.kind());
        Map<String, RepositoryResource> coreBoxes = repositoryMap.get(ResourceType.Th2CoreBox.kind());
        List<RepositoryResource> allBoxes = new ArrayList<>(boxes.values());
        allBoxes.addAll(coreBoxes.values());
        Secret secret = SecretsUtils.getCustomSecret(schemaName);
        Map<String, String> secretData = secret.getData();
        for (var res : allBoxes) {
            Map<String, Object> customConfig = extractCustomConfig(res);
            Set<String> secretsConfig = generateSecretsConfig(customConfig);
            if (!secretsConfig.isEmpty()) {
                for (String secretKey : secretsConfig) {
                    if (secretData == null || !secretData.containsKey(secretKey)) {
                        String resName = res.getMetadata().getName();
                        String errorMessage = String.format("Resource \"%s\" is invalid, value \"%s\" from " +
                                "\"secret-custom-config\" is not present in Kubernetes", resName, secretKey);
                        schemaValidationContext.setInvalidResource(resName);
                        schemaValidationContext.addSecretsErrorMessage(errorMessage);
                    }
                }
            }
        }
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

        MqLinkValidator mqLinkValidator = new MqLinkValidator(schemaContext);
        GrpcLinkValidator grpcLinkValidator = new GrpcLinkValidator(schemaContext);
        DictionaryLinkValidator dictionaryLinkValidator = new DictionaryLinkValidator(schemaContext);

        for (RepositoryResource linkRes : links) {
            Th2LinkSpec spec = mapper.convertValue(linkRes.getSpec(), Th2LinkSpec.class);
            for (MessageLink mqLink : spec.getBoxesRelation().getRouterMq()) {
                mqLinkValidator.validateLink(linkRes, mqLink);
            }
            for (MessageLink grpcLink : spec.getBoxesRelation().getRouterGrpc()) {
                grpcLinkValidator.validateLink(linkRes, grpcLink);
            }
            for (DictionaryLink dictionaryLink : spec.getDictionariesRelation()) {
                dictionaryLinkValidator.validateLink(linkRes, dictionaryLink);
            }
//            schemaValidationContext.removeInvalidLinks(linkRes.getMetadata().getName(), spec);
//            linkRes.setSpec(spec);
//            try {
//                String specStr = mapper.writeValueAsString(spec);
//                linkRes.setSourceHash(SourceHashUtil.digest(specStr));
//            } catch (JsonProcessingException e) {
//                logger.error("Couldn't update source hash for \"{}\"", annotationFor(linkRes, schemaName));
//            }
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
}
