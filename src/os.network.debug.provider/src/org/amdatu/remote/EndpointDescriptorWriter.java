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

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Generates an XML representation according to {@code http://www.osgi.org/xmlns/rsa/v1.0.0/rsa.xsd}
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EndpointDescriptorWriter {

    private final static SAXParserFactory SAX_PARSERFACTORY = SAXParserFactory.newInstance();

    public void writeDocument(Writer writer, EndpointDescription... endpoints) throws IOException {
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<endpoint-descriptions xmlns=\"http://www.osgi.org/xmlns/rsa/v1.0.0\">\n");
        for (EndpointDescription endpoint : endpoints) {
            appendEndpoint(writer, endpoint);
        }
        writer.append("</endpoint-descriptions>");
    }

    private static void appendEndpoint(Writer writer, EndpointDescription endpoint) throws IOException {
        writer.append("  <endpoint-description>\n");
        for (Entry<String, Object> entry : endpoint.getProperties().entrySet()) {
            appendProperty(writer, entry.getKey(), entry.getValue());
        }
        writer.append("  </endpoint-description>\n");
    }

    private static void appendProperty(Writer writer, String key, Object value) throws IOException {
        if (value.getClass().isArray() || value instanceof List<?> || value instanceof Set<?>) {
            appendMultiValueProperty(writer, key, value);
        }
        else {
            appendSingleValueProperty(writer, key, value);
        }
    }

    private static void appendSingleValueProperty(Writer writer, String key, Object value) throws IOException {
        if (ValueTypes.get(value.getClass()) == null) {
            throw new IllegalStateException("Unsupported type : " + value.getClass());
        }
        if (value instanceof String) {
            String string = (String) value;
            if (string.trim().startsWith("<") && isWellFormedXml(string)) {
                writer.append("    <property name=\"").append(escapeXml(key)).append("\">\n").append("      <xml>")
                    .append((String) value).append("</xml>\n").append("    </property>\n");
            }
            else {
                writer.append("   <property name=\"").append(escapeXml(key)).append("\" value=\"")
                    .append(escapeXml(string)).append("\"/>\n");
            }
        }
        else {
            writer.append("   <property name=\"").append(escapeXml(key))
                .append("\" value-type=\"" + value.getClass().getSimpleName() + "\" value=\"")
                .append(escapeXml(value.toString())).append("\"/>\n");
        }
    }

    private static void appendMultiValueProperty(Writer writer, String key, Object value) throws IOException {

        Class<?> componentType = determineComponentType(value);
        if (ValueTypes.get(componentType) == null) {
            throw new IllegalStateException();
        }
        if (componentType.equals(String.class)) {
            writer.append("   <property name=\"").append(escapeXml(key)).append("\">\n");
        }
        else {
            writer.append("   <property name=\"").append(escapeXml(key)).append("\" value-type=\"")
                .append(componentType.getSimpleName()).append("\">\n");
        }
        if (value.getClass().isArray()) {
            List<Object> objectList = new ArrayList<Object>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                objectList.add(Array.get(value, i));
            }
            writer.append("      <array>").append("\n");
            appendMultiValues(writer, objectList);
            writer.append("      </array>\n");
        }
        else if (value instanceof List<?>) {
            writer.append("      <list>").append("\n");
            appendMultiValues(writer, (List<?>) value);
            writer.append("      </list>\n");
        }
        else if (value instanceof Set<?>) {
            writer.append("      <set>").append("\n");
            appendMultiValues(writer, (Set<?>) value);
            writer.append("      </set>\n");
        }
        writer.append("   </property>\n");
    }

    private static void appendMultiValues(Writer writer, Collection<?> value) throws IOException {
        for (Iterator<?> it = value.iterator(); it.hasNext();) {
            writer.append("         <value>").append(escapeXml(it.next().toString())).append("</value>").append("\n");
        }
    }

    private static Class<?> determineComponentType(Object value) {
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType();
        }
        if (value instanceof Collection<?>) {
            Collection<?> col = ((Collection<?>) value);
            if (col.isEmpty()) {
                return String.class;
            }
            else {
                return col.iterator().next().getClass();
            }
        }
        return value.getClass();
    }

    private static boolean isWellFormedXml(String value) {
        try {
            InputSource source = new InputSource(new StringReader(value));
            SAX_PARSERFACTORY.newSAXParser().parse(source, new DefaultHandler());
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
