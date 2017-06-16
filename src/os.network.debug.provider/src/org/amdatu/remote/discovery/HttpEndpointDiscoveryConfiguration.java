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
package org.amdatu.remote.discovery;

import java.net.URL;

/**
 * Interface for accessing discovery configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface HttpEndpointDiscoveryConfiguration {

    /** Default value for the connect timeout for endpoint discovery */
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    /** Default value for the read timeout for endpoint discovery */
    public static final int DEFAULT_READ_TIMEOUT = 5000;

    /**
     * returns the discovery base url
     * 
     * @return the base url
     */
    public URL getBaseUrl();

    /**
     * returns the connect timeout for the endpoint discovery
     * 
     * @return connect timeout in ms
     */
    public int getConnectTimeout();

    /**
     * returns the read timeout for the endpoint discovery
     * 
     * @return read timeout in ms
     */
    public int getReadTimeout();

    /**
     * return the time period in seconds between scheduled polls to the discovery endpoints
     * 
     * @return schedule period in seconds
     */
    public int getSchedule();
}
