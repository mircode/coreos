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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Generic {@link EndpointDescription} utilities.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EndpointUtil {

    private final static EndpointDescriptorReader m_reader = new EndpointDescriptorReader();
    private final static EndpointDescriptorWriter m_writer = new EndpointDescriptorWriter();
    private final static EndpointHashGenerator m_hasher = new EndpointHashGenerator();

    private EndpointUtil() {
    }

    public static List<EndpointDescription> readEndpoints(Reader reader) throws IOException {
        return m_reader.parseDocument(reader);
    }

    public static void writeEndpoints(Writer writer, EndpointDescription... endpoints) throws IOException {
        m_writer.writeDocument(writer, endpoints);
    }

    public static String computeHash(EndpointDescription endpoint) {
        return m_hasher.hash(endpoint.getProperties());
    }
}
