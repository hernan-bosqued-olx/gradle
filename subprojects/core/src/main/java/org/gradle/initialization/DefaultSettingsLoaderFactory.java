/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.initialization;

import org.gradle.composite.internal.IncludedBuildFactory;
import org.gradle.initialization.buildsrc.BuildSourceBuilder;
import org.gradle.internal.composite.CompositeBuildSettingsLoader;
import org.gradle.internal.composite.CompositeContextBuilder;

public class DefaultSettingsLoaderFactory implements SettingsLoaderFactory {
    private final ISettingsFinder settingsFinder;
    private final SettingsProcessor settingsProcessor;
    private final BuildSourceBuilder buildSourceBuilder;
    private final CompositeContextBuilder compositeContextBuilder;
    private final IncludedBuildFactory includedBuildFactory;

    public DefaultSettingsLoaderFactory(ISettingsFinder settingsFinder, SettingsProcessor settingsProcessor, BuildSourceBuilder buildSourceBuilder,
                                        CompositeContextBuilder compositeContextBuilder, IncludedBuildFactory includedBuildFactory) {
        this.settingsFinder = settingsFinder;
        this.settingsProcessor = settingsProcessor;
        this.buildSourceBuilder = buildSourceBuilder;
        this.compositeContextBuilder = compositeContextBuilder;
        this.includedBuildFactory = includedBuildFactory;
    }

    @Override
    public SettingsLoader forTopLevelBuild() {
        return notifyingSettingsLoader(compositeBuildSettingsLoader());
    }

    @Override
    public SettingsLoader forNestedBuild() {
        return notifyingSettingsLoader(defaultSettingsLoader());
    }

    private SettingsLoader compositeBuildSettingsLoader() {
        return new CompositeBuildSettingsLoader(
            defaultSettingsLoader(),
            compositeContextBuilder,
            includedBuildFactory
        );
    }

    private SettingsLoader defaultSettingsLoader() {
        return new DefaultSettingsLoader(
            settingsFinder,
            settingsProcessor,
            buildSourceBuilder
        );
    }


    private SettingsLoader notifyingSettingsLoader(SettingsLoader settingsLoader) {
        return new NotifyingSettingsLoader(settingsLoader);
    }

}
