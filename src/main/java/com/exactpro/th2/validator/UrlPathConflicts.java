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

import com.exactpro.th2.infrarepo.RepositoryResource;
import com.exactpro.th2.validator.errormessages.BoxResourceErrorMessage;

import java.util.*;

public class UrlPathConflicts {

    public static void detectUrlPathsConflicts(SchemaValidationContext schemaValidationContext,
                                               Map<String, RepositoryResource> repositoryResources
    ) {

        // get map of resource label and url paths pairs
        Map<String, Set<String>> repositoryUrlPaths = getRepositoryUrlPaths(
                schemaValidationContext,
                repositoryResources
        );
        // if no resource or just one resource contains url paths then there can't be conflicts
        if (repositoryUrlPaths.size() < 2) {
            return;
        }

        Set<String> entries = new HashSet<>();

        // detect conflicts, if any
        for (var entry1 : repositoryUrlPaths.entrySet()) {
            entries.add(entry1.getKey());

            for (var entry2 : repositoryUrlPaths.entrySet()) {
                // avoid comparing resource with itself and comparing the same resources twice
                if (entries.contains(entry2.getKey())) {
                    continue;
                }

                List<String> duplicated = new ArrayList<>();
                Set<String> checker = new HashSet<>(entry1.getValue());
                // use the set 'checker' to detect url duplications between resources
                for (String url : entry2.getValue()) {
                    if (checker.contains(url)) {
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

    private static Map<String, Set<String>> getRepositoryUrlPaths(SchemaValidationContext schemaValidationContext,
                                                                  Map<String, RepositoryResource> resources
    ) {
        Map<String, Set<String>> map = new HashMap<>();
        for (RepositoryResource resource : resources.values()) {

            String resourceName = resource.getMetadata().getName();
            try {
                var spec = (Map<String, Object>) resource.getSpec();
                if (spec == null) {
                    continue;
                }

                var settings = (Map<String, Object>) spec.get("extended-settings");
                if (settings == null) {
                    continue;
                }

                var service = (Map<String, Object>) settings.get("service");
                if (service == null) {
                    continue;
                }

                var ingress = (Map<String, Object>) service.get("ingress");
                if (ingress == null) {
                    continue;
                }

                List<String> urls = (List<String>) ingress.get("urlPaths");
                if (urls == null || urls.isEmpty()) {
                    continue;
                }

                Set<String> urlPaths = new HashSet<>();
                Set<String> duplicated = new HashSet<>();
                // use the set 'urlPaths' to detect url duplication in a resource
                for (String url : urls) {
                    if (!urlPaths.add(url)) {
                        duplicated.add(url);
                    }
                }

                if (!duplicated.isEmpty()) {
                    // put fixed urlPaths property in the resource
                    ingress.put("urlPaths", new ArrayList<>(urlPaths));
                }
                // collect resources that contain url paths
                map.put(resourceName, urlPaths);

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

        return map;
    }
}
