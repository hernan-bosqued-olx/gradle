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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionRangeSelector;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelector;

import java.util.Map;
import java.util.Set;

public class DefaultIntersectingVersionRangesHandler implements IntersectingVersionRangesHandler {
    private final Map<DynamicMatcherKey, VersionRangeSelector> mergedRangeMatchers = Maps.newHashMap();
    private final Set<DynamicMatcherKey> hasDisjointRanges = Sets.newHashSet();

    private int version;

    public VersionSelector maybeIntersect(String group, String name, VersionSelector current) {
        DynamicMatcherKey key = new DynamicMatcherKey(group, name);
        if (hasDisjointRanges.contains(key)) {
            return current;
        }
        if (current instanceof VersionRangeSelector) {
            VersionRangeSelector versionMatcher = mergedRangeMatchers.get(key);
            if (versionMatcher == null) {
                mergedRangeMatchers.put(key, (VersionRangeSelector) current);
            } else {
                VersionRangeSelector intersection = ((VersionRangeSelector) current).intersect(versionMatcher);
                if (intersection == null) {
                    hasDisjointRanges.add(key);
                    mergedRangeMatchers.remove(key); // free some memory
                    // disjoint intervals, just return 'current' so that conflict resolution can kick in
                    return current;
                }
                if (!intersection.equals(versionMatcher)) {
                    mergedRangeMatchers.put(key, intersection);
                    version++;
                }
                return intersection;
            }
        } else {
            hasDisjointRanges.add(key);
            // free some memory
            mergedRangeMatchers.remove(key);
        }
        return current;
    }

    @Override
    public boolean hasIntersectingRanges(String group, String name) {
        DynamicMatcherKey key = new DynamicMatcherKey(group, name);
        return !hasDisjointRanges.contains(key) && mergedRangeMatchers.containsKey(key);
    }

    @Override
    public int getVersion() {
        return version;
    }

    private static class DynamicMatcherKey {
        private final String group;
        private final String name;
        private final int hashCode;

        private DynamicMatcherKey(String group, String name) {
            this.group = group;
            this.name = name;
            this.hashCode = doHash();
        }

        private int doHash() {
            int result = group.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DynamicMatcherKey that = (DynamicMatcherKey) o;

            if (!group.equals(that.group)) {
                return false;
            }
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "group='" + group + '\'' + ", name='" + name + '\'';
        }
    }
}
