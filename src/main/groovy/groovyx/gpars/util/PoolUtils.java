//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars.util;

/**
 * Provides a couple of utility methods to pools and schedulers.
 *
 * @author Vaclav Pech
 * Date: Oct 31, 2009
 */
@SuppressWarnings({"AccessOfSystemProperties", "UtilityClass"})
public final class PoolUtils {
    private static final String GPARS_POOLSIZE = "gpars.poolsize";

    private PoolUtils() { }

    public static int retrieveDefaultPoolSize() {
        final String poolSizeValue = System.getProperty(GPARS_POOLSIZE);
        try {
            return Integer.parseInt(poolSizeValue);
        } catch (NumberFormatException ignored) {
            return Runtime.getRuntime().availableProcessors() + 1;
        }
    }
}
