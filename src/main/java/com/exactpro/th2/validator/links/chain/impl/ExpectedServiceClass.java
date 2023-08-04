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

package com.exactpro.th2.validator.links.chain.impl;

import com.exactpro.th2.infrarepo.repo.RepositoryResource;
import com.exactpro.th2.validator.links.chain.AbstractValidator;
import com.exactpro.th2.validator.links.enums.SchemaConnectionType;
import com.exactpro.th2.validator.links.ValidationResult;
import com.exactpro.th2.validator.model.*;
import com.exactpro.th2.validator.model.pin.GrpcClientPin;
import com.exactpro.th2.validator.model.pin.GrpcServerPin;

import java.util.Set;

import static com.exactpro.th2.validator.util.MapperUtils.MAPPER;
import static java.lang.String.format;

public final class ExpectedServiceClass extends AbstractValidator {

    private final RepositoryResource linkedResource;

    private final String linkedResourceName;

    private final String linkedPinName;

    private final SchemaConnectionType connectionType;

    public ExpectedServiceClass(BoxLinkContext context) {
        this.linkedResource = context.getLinkedResource();
        this.linkedResourceName = context.getLinkedResourceName();
        this.linkedPinName = context.getLinkedPinName();
        this.connectionType = context.getConnectionType();
    }

    @Override
    public ValidationResult validate(Object object, Object... additional) {

        if (connectionType == SchemaConnectionType.grpc_server) {
            return super.validate(object, additional);
        }
        if (linkedResource == null) {
            return ValidationResult.invalid(format("Linked resource: [%s] does not exist", linkedResourceName));
        }

        Th2Spec linkedResSpec = MAPPER.convertValue(linkedResource.getSpec(), Th2Spec.class);
        GrpcServerPin linkedPin = linkedResSpec.getGrpcServerPin(linkedPinName);

        if (linkedPin == null) {
            return ValidationResult.invalid(
                    format("Linked grpc pin: [%s] does not exist in server section", linkedPinName));
        }

        Set<String> serviceClasses = linkedPin.getServiceClasses();

        if (serviceClasses == null || serviceClasses.isEmpty()) {
            return ValidationResult.invalid(
                    format("Linked resource: [%s] is invalid. linked pin: [%s] does not contain serviceClasses",
                            linkedResourceName, linkedPinName)
            );
        }

        var pin = (GrpcClientPin) object;
        if (serviceClasses.contains(pin.getServiceClass())) {
            return super.validate(object, additional);
        }

        return ValidationResult.invalid(format(
                "Provided serviceClass: [%s] is not supported by the server: [%s]. Supported service classes are: [%s]",
                pin.getServiceClass(), linkedResourceName, serviceClasses.toString()
                )
        );

    }
}
