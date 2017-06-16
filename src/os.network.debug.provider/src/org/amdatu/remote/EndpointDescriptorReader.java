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

import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses an XML representation according to {@code http://www.osgi.org/xmlns/rsa/v1.0.0/rsa.xsd}
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EndpointDescriptorReader {

    private final static SAXParserFactory SAX_PARSERFACTORY = SAXParserFactory.newInstance();

    public List<EndpointDescription> parseDocument(Reader reader) {
        EndpointDescriptorParserHandler handler = new EndpointDescriptorParserHandler();
        InputSource source = null;
        try {
            source = new InputSource(reader);
            SAX_PARSERFACTORY.newSAXParser().parse(source, handler);
            return handler.getEndpointDescriptions();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static class EndpointDescriptorParserHandler extends DefaultHandler {

        private final List<EndpointDescription> m_endpointDesciptions = new ArrayList<EndpointDescription>();
        private final Map<String, Object> m_endpointProperties = new HashMap<String, Object>();
        private final StringBuilder m_valueBuffer = new StringBuilder();

        private boolean m_inProperty = false;
        private boolean m_inArray = false;
        private boolean m_inList = false;
        private boolean m_inSet = false;
        private boolean m_inXml = false;
        private boolean m_inValue = false;
        private String m_propertyName;
        private String m_propertyValue;
        private ValueTypes m_propertyType;
        private final List<Object> m_propertyValues = new ArrayList<Object>();

        public List<EndpointDescription> getEndpointDescriptions() {
            return m_endpointDesciptions;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            if (m_inXml) {
                m_valueBuffer.append("<" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    m_valueBuffer.append(" ").append(attributes.getQName(i)).append("=\"")
                        .append(attributes.getValue(i)).append("\"");
                }
                m_valueBuffer.append(">");
                return;
            }

            if (qName.equals("property")) {
                m_inProperty = true;
                m_propertyName = attributes.getValue(uri, "name");
                m_propertyType = getValueType(attributes.getValue(uri, "value-type"));
                m_propertyValue = attributes.getValue(uri, "value");
                m_propertyValues.clear();
                return;
            }

            m_valueBuffer.setLength(0);
            m_inArray |= m_inProperty && qName.equals("array");
            m_inList |= m_inProperty && qName.equals("list");
            m_inSet |= m_inProperty && qName.equals("set");
            m_inXml |= m_inProperty && qName.equals("xml");
            m_inValue |= m_inProperty && qName.equals("value");
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            if (m_inXml) {
                if (!qName.equals("xml")) {
                    m_valueBuffer.append("</" + qName + ">");
                }
                else {
                    m_inXml = false;
                }
                return;
            }

            if (qName.equals("endpoint-description")) {
                m_endpointDesciptions.add(new EndpointDescription(m_endpointProperties));
                m_endpointProperties.clear();
                return;
            }

            if (qName.equals("property")) {
                m_inProperty = false;
                if (m_inArray) {
                    m_endpointProperties.put(m_propertyName, getPropertyValuesArray());
                }
                else if (m_inList) {
                    m_endpointProperties.put(m_propertyName, getPropertyValuesList());
                }
                else if (m_inSet) {
                    m_endpointProperties.put(m_propertyName, getPropertyValuesSet());
                }
                else if (m_propertyValue != null) {
                    m_endpointProperties.put(m_propertyName, m_propertyType.parse(m_propertyValue));
                }
                else {
                    m_endpointProperties.put(m_propertyName, m_valueBuffer.toString());
                }
                m_inArray = false;
                m_inList = false;
                m_inSet = false;
                m_inXml = false;
                return;
            }

            if (qName.equals("value")) {
                m_propertyValues.add(m_propertyType.parse(m_valueBuffer.toString()));
                m_inValue = false;
                return;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (m_inValue || m_inXml) {
                m_valueBuffer.append(ch, start, length);
            }
        }

        private ValueTypes getValueType(String name) {
            if (name == null || "".equals(name)) {
                return ValueTypes.STRING_CLASS;
            }
            ValueTypes type = ValueTypes.get(name);
            return type == null ? ValueTypes.STRING_CLASS : type;
        }

        private Object getPropertyValuesArray() {
            // using reflection because component type may be primitive
            Object valuesArray = Array.newInstance(m_propertyType.getType(), m_propertyValues.size());
            for (int i = 0; i < m_propertyValues.size(); i++) {
                Array.set(valuesArray, i, m_propertyValues.get(i));
            }
            return valuesArray;
        }

        private List<?> getPropertyValuesList() {
            return new ArrayList<Object>(m_propertyValues);
        }

        private Set<Object> getPropertyValuesSet() {
            return new HashSet<Object>(m_propertyValues);
        }
    }
}
