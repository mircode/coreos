/*
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
package org.amdatu.remote;

import org.osgi.service.log.LogService;

/**
 * Amdatu Remote compile time constants.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class Constants {

    /**
     * Property key prefix for the log level. Default is {@link LogService#LOG_INFO}.
     */
    public final static String LOGGING_PROP_PRE = "amdatu.remote.logging";

    /**
     * Property key prefix for the console level. Default is {@link LogService#LOG_ERROR} - 1.
     */
    public final static String CONSOLE_PROP_PRE = "amdatu.remote.console";

    /**
     * Manifest header key
     */
    public final static String MANIFEST_REMOTE_SERVICE_HEADER = "Remote-Service";

}
