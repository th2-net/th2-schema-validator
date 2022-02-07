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

package com.exactpro.th2.validator.model;

import com.exactpro.th2.infrarepo.RepositoryResource;
import com.exactpro.th2.validator.enums.BoxDirection;
import com.exactpro.th2.validator.enums.SchemaConnectionType;

public final class BoxLinkContext {

    private String boxName;

    private String boxPinName;

    private BoxDirection boxDirection;

    private SchemaConnectionType connectionType;

    private RepositoryResource linkedResource;

    private String linkedResourceName;

    private String linkedPinName;

    public String getBoxName() {
        return boxName;
    }

    public String getBoxPinName() {
        return boxPinName;
    }

    public BoxDirection getBoxDirection() {
        return boxDirection;
    }

    public SchemaConnectionType getConnectionType() {
        return connectionType;
    }

    public RepositoryResource getLinkedResource() {
        return linkedResource;
    }

    public String getLinkedResourceName() {
        return linkedResourceName;
    }

    public String getLinkedPinName() {
        return linkedPinName;
    }

    public static class Builder {

        private String boxName;

        private String boxPinName;

        private BoxDirection boxDirection;

        private SchemaConnectionType connectionType;

        private RepositoryResource linkedResource;

        private String linkedResourceName;

        private String linkedPinName;

        public Builder setBoxName(String boxName) {
            this.boxName = boxName;
            return this;
        }

        public Builder setBoxPinName(String boxPinName) {
            this.boxPinName = boxPinName;
            return this;
        }

        public Builder setBoxDirection(BoxDirection boxDirection) {
            this.boxDirection = boxDirection;
            return this;
        }

        public Builder setConnectionType(SchemaConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }

        public Builder setLinkedResource(RepositoryResource linkedResource) {
            this.linkedResource = linkedResource;
            return this;
        }

        public Builder setLinkedResourceName(String linkedResourceName) {
            this.linkedResourceName = linkedResourceName;
            return this;
        }

        public Builder setLinkedPinName(String linkedPinName) {
            this.linkedPinName = linkedPinName;
            return this;
        }

        public BoxLinkContext build() {

            BoxLinkContext boxLinkContext = new BoxLinkContext();
            boxLinkContext.boxName = boxName;
            boxLinkContext.boxPinName = boxPinName;
            boxLinkContext.boxDirection = boxDirection;
            boxLinkContext.connectionType = connectionType;
            boxLinkContext.linkedResource = linkedResource;
            boxLinkContext.linkedPinName = linkedPinName;
            boxLinkContext.linkedResourceName = linkedResourceName;
            return boxLinkContext;
        }
    }
}
