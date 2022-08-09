/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import java.util.*;

import static com.exactpro.th2.validator.util.ResourceUtils.getSection;

public class UrlPathConflicts {

    public static void detectUrlPathsConflicts(SchemaValidationContext schemaValidationContext,
                                               Map<String, RepositoryResource> repositoryResources
    ) {

        Map<String, Set<String>> repositoryUrlPaths = getRepositoryUrlPaths(
                schemaValidationContext,
                repositoryResources
        );
        // if no resource or just one resource contains url paths then there can't be conflicts
        if (repositoryUrlPaths.size() < 2) {
            return;
        }

        Set<String> resourceNames = new HashSet<>();

        for (var entry1 : repositoryUrlPaths.entrySet()) {
            resourceNames.add(entry1.getKey());
            final Set<String> urls = Collections.unmodifiableSet(entry1.getValue());

            for (var entry2 : repositoryUrlPaths.entrySet()) {
                // avoid comparing resource with itself and comparing the same resources twice
                if (resourceNames.contains(entry2.getKey())) {
                    continue;
                }

                List<String> duplicated = new ArrayList<>();

                for (String url : entry2.getValue()) {
                    if (urls.contains(url)) {
                        duplicated.add(url);
                    }
                }

                if (!duplicated.isEmpty()) {
                    String message = String.format("Conflict of url paths %s with resource \"%s\"",
                            duplicated, entry2.getKey());
                    schemaValidationContext.setInvalidResource(entry1.getKey());
                    schemaValidationContext.addBoxResourceErrorMessages(
                            new BoxResourceErrorMessage(
                                    entry1.getKey(),
                                    message
                            )
                    );
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> getRepositoryUrlPaths(SchemaValidationContext schemaValidationContext,
                                                                  Map<String, RepositoryResource> resources
    ) {
        Map<String, Set<String>> resToUrlPaths = new HashMap<>();
        for (RepositoryResource resource : resources.values()) {

            String resourceName = resource.getMetadata().getName();
            try {
                var spec = (Map<String, Object>) resource.getSpec();
                Map<String, Object> settings = getSection(spec, "extendedSettings");
                Map<String, Object> service = getSection(settings, "service");
                Map<String, Object> ingress = getSection(service, "ingress");
                if (ingress == null) {
                    continue;
                }

                var urls = (List<String>) ingress.get("urlPaths");
                if (urls == null || urls.isEmpty()) {
                    continue;
                }

                Set<String> urlPaths = new HashSet<>();
                boolean isDuplicated = false;

                for (String url : urls) {
                    if (!urlPaths.add(url)) {
                        isDuplicated = true;
                    }
                }

                if (isDuplicated) {
                    ingress.put("urlPaths", new ArrayList<>(urlPaths));
                }
                resToUrlPaths.put(resourceName, urlPaths);

            } catch (ClassCastException e) {
                String message = String.format("Exception extracting urlPaths property. exception: %s", e.getMessage());
                schemaValidationContext.setInvalidResource(resourceName);
                schemaValidationContext.addBoxResourceErrorMessages(
                        new BoxResourceErrorMessage(
                                resourceName,
                                message
                        )
                );
            }
        }

        return resToUrlPaths;
    }
}
