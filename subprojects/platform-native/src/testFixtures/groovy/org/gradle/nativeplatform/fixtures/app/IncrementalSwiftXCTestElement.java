/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativeplatform.fixtures.app;

import java.util.List;
import java.util.regex.Pattern;

public abstract class IncrementalSwiftXCTestElement extends IncrementalSwiftElement implements XCTestElement {
    public Pattern getExpectedSummaryOutputPattern() {
        return XCTestSourceElement.toExpectedSummaryOutputPattern(getTestCount(), getFailureCount());
    }

    public Pattern getExpectedAlternateSummaryOutputPattern() {
        return XCTestSourceElement.toExpectedSummaryOutputPattern(getAlternateTestCount(), getAlternateFailureCount());
    }

    public abstract List<XCTestSourceFileElement> getTestSuites();
    public abstract List<XCTestSourceFileElement> getAlternateTestSuites();

    @Override
    public String getSourceSetName() {
        return "test";
    }

    @Override
    public int getFailureCount() {
        int result = 0;
        for (XCTestSourceFileElement element : getTestSuites()) {
            result += element.getFailureCount();
        }
        return result;
    }

    @Override
    public int getPassCount() {
        int result = 0;
        for (XCTestSourceFileElement element : getTestSuites()) {
            result += element.getPassCount();
        }
        return result;
    }

    @Override
    public int getTestCount() {
        int result = 0;
        for (XCTestSourceFileElement element : getTestSuites()) {
            result += element.getTestCount();
        }
        return result;
    }

    public int getAlternateTestCount() {
        int result = 0;
        for (XCTestSourceFileElement element : getAlternateTestSuites()) {
            result += element.getTestCount();
        }
        return result;
    }

    public int getAlternateFailureCount() {
        int result = 0;
        for (XCTestSourceFileElement element : getAlternateTestSuites()) {
            result += element.getFailureCount();
        }
        return result;
    }

    public int getAlternatePassCount() {
        int result = 0;
        for (XCTestSourceFileElement element : getAlternateTestSuites()) {
            result += element.getPassCount();
        }
        return result;
    }
}
