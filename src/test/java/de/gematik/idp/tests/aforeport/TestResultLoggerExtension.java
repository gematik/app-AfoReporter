/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.tests.aforeport;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

@Slf4j
public class TestResultLoggerExtension implements TestWatcher {

    @Override
    public void testDisabled(final ExtensionContext extensionContext, final Optional<String> optional) {
        logResult(extensionContext, "DISABLED");
    }

    @Override
    public void testSuccessful(final ExtensionContext extensionContext) {
        logResult(extensionContext, "PASSED");
    }

    @Override
    public void testAborted(final ExtensionContext extensionContext, final Throwable throwable) {
        logResult(extensionContext, "ABORTED");
    }

    @Override
    public void testFailed(final ExtensionContext extensionContext, final Throwable throwable) {
        logResult(extensionContext, "FAILED");
    }

    private void logResult(final ExtensionContext context, final String status) {
        if (log.isDebugEnabled()) {
            context.getTestMethod().ifPresentOrElse(
                m -> log.debug(String.format("%s: %s - %s", m.getName(), context.getDisplayName(), status)),
                () -> log.debug(String.format("%s: %s - %s", "UNKNOWN METHOD", context.getDisplayName(), status))
            );
        }

    }
}
