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

import java.util.ArrayList;
import java.util.List;

public class ValidationReport {

    private List<String> linkErrorMessages = new ArrayList<>();

    private List<String> secretsErrorMessages = new ArrayList<>();

    private Exception exception;

    private String commitRef;

    public void addLinkErrorMessage(String message) {
        this.linkErrorMessages.add(message);
    }

    public void addSecretsErrorMessage(String message) {
        this.secretsErrorMessages.add(message);
    }

    public List<String> getLinkErrorMessages() {
        return linkErrorMessages;
    }

    public List<String> getSecretsErrorMessages() {
        return secretsErrorMessages;
    }

    public Exception getException() {
        return exception;
    }

    public String getCommitRef() {
        return commitRef;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void setCommitRef(String commitRef) {
        this.commitRef = commitRef;
    }
}

