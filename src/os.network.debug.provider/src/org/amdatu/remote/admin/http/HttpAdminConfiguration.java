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
package org.amdatu.remote.admin.http;

import java.net.URL;

/**
 * Interface for accessing HTTP Admin configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface HttpAdminConfiguration {

    /**
     * returns the base url for the HTTP admin
     * 
     * @return the base url
     */
    public URL getBaseUrl();
    
    /**
     * returns the connect timeout for the client endpoint
     * 
     * @return connect timeout in ms
     */
    public int getConnectTimeout();

    /**
     * returns the read timeout for the client endpoint
     * 
     * @return read timeout in ms
     */
    public int getReadTimeout();

}
