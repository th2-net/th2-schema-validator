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

import com.exactpro.th2.validator.links.enums.MessageFormatAttribute;
import com.exactpro.th2.validator.model.BoxLinkContext;

import java.util.List;

public final class ExpectedRawMessageAttr extends ExpectedMessageFormatAttr {

    public ExpectedRawMessageAttr(BoxLinkContext context) {
        super(
                context,
                MessageFormatAttribute.raw.getPrefix(),
                //contradictingAttributePrefixes
                List.of(
                        MessageFormatAttribute.parsed.getPrefix(),
                        MessageFormatAttribute.event.getPrefix()
                ),
                //otherMatchingAttributePrefixes
                List.of(MessageFormatAttribute.group.getPrefix())
        );
    }
}
