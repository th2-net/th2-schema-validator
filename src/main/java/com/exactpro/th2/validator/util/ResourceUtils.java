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

package com.exactpro.th2.validator.util;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ResourceUtils {
    private ResourceUtils() {
    }

    public static Map<String, Object> getSection(Map<String, Object> parent, String sectionName) {
        return parent != null ? (Map<String, Object>) parent.get(sectionName) : null;
    }

    public static List<Map<String, Object>> getSectionArray(Map<String, Object> parent, String sectionName) {
        return parent != null ? (List<Map<String, Object>>) parent.get(sectionName) : null;
    }

    public static Map<String, RepositoryResource> collectResources(
            Map<String, Map<String, RepositoryResource>> repositoryMap,
            String... kinds
    ) {
        Map<String, RepositoryResource> result = new HashMap<>();
        for (String kind : kinds) {
            result.putAll(repositoryMap.getOrDefault(kind, Map.of()));
        }
        return result;
    }
}
