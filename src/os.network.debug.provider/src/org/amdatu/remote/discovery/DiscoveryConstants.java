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

/**
 * Common constants used by all implementations of the discovery service.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface DiscoveryConstants {

    /** Indicates that a service is actually a discovery service, should have a value of "true". */
    String DISCOVERY = "discovery";

    /** Indicates what kind of discovery service is provided. */
    String DISCOVERY_TYPE = "discovery.type";

}
