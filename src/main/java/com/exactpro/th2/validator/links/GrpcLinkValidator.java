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

package com.exactpro.th2.validator.links;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.SchemaContext;
import com.exactpro.th2.validator.SchemaValidationContext;
import com.exactpro.th2.validator.links.chain.impl.ExpectedServiceClass;
import com.exactpro.th2.validator.links.chain.impl.PinExist;
import com.exactpro.th2.validator.links.chain.impl.ResourceExists;
import com.exactpro.th2.validator.links.enums.BoxDirection;
import com.exactpro.th2.validator.links.enums.SchemaConnectionType;
import com.exactpro.th2.validator.errormessages.LinkErrorMessage;
import com.exactpro.th2.validator.model.BoxLinkContext;
import com.exactpro.th2.validator.model.link.Endpoint;
import com.exactpro.th2.validator.model.link.MessageLink;

class GrpcLinkValidator extends BoxesLinkValidator {

    GrpcLinkValidator(SchemaContext schemaContext) {
        super(schemaContext);
    }

    @Override
    void validateLink(MessageLink link) {
        Endpoint fromBoxEndpoint = link.getFrom();
        String resName = link.getResourceName();
        Endpoint toBoxEndpoint = link.getTo();

        try {
            RepositoryResource toRes = schemaContext.getBox(toBoxEndpoint.getBox());

            var fromContext = new BoxLinkContext.Builder()
                    .setBoxName(fromBoxEndpoint.getBox())
                    .setBoxPinName(fromBoxEndpoint.getPin())
                    .setBoxDirection(BoxDirection.from)
                    .setConnectionType(SchemaConnectionType.grpc_client)
                    .setLinkedResource(toRes)
                    .setLinkedResourceName(toBoxEndpoint.getBox())
                    .setLinkedPinName(toBoxEndpoint.getPin())
                    .build();

            var toContext = new BoxLinkContext.Builder()
                    .setBoxName(toBoxEndpoint.getBox())
                    .setBoxPinName(toBoxEndpoint.getPin())
                    .setBoxDirection(BoxDirection.to)
                    .setConnectionType(SchemaConnectionType.grpc_server)
                    .build();

            validate(fromContext, toContext, resName, link);
        } catch (Exception e) {
            var schemaValidationContext = schemaContext.getSchemaValidationContext();
            schemaValidationContext.setInvalidResource(resName);
            schemaValidationContext.addLinkErrorMessage(
                    new LinkErrorMessage(
                            link.getContent(),
                            String.format("Exception: %s", e.getMessage())
                    )
            );
        }
    }

    @Override
    void addValidMessageLink(String linkResName, MessageLink link) {
        SchemaValidationContext schemaValidationContext = schemaContext.getSchemaValidationContext();
        schemaValidationContext.addValidGrpcLink(linkResName, link);
    }

    @Override
    ValidationResult validateByContext(RepositoryResource resource,
                                       BoxLinkContext context) {
        var resValidator = new ResourceExists(context);
        var pinExist = new PinExist(context);
        var expectedServiceClass = new ExpectedServiceClass(context);

        resValidator.setNext(pinExist);
        pinExist.setNext(expectedServiceClass);

        return resValidator.validate(resource);
    }
}
