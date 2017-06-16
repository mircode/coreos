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

import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

/**
 * Generic OSGi service utilities.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ServiceUtil {

    /**
     * Return the framework UUID associated with the provided Bundle Context. If
     * no framework UUID is set it will be assigned.
     * 
     * @param bundleContext the context
     * @return the UUID
     */
    public static String getFrameworkUUID(BundleContext bundleContext) {
        String uuid = bundleContext.getProperty("org.osgi.framework.uuid");
        if (uuid != null) {
            return uuid;
        }
        synchronized ("org.osgi.framework.uuid") {
            uuid = bundleContext.getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }

    /**
     * Returns String[] for a String+ service property value. The value must be of
     * type String, String[] or Collection&gt;String&lt;.
     * 
     * @param value an object of a valid type, can be {@code null}
     * @return a String[] containing the String+ entries
     * @throws IllegalArgumentException if the value type is invalid
     */
    public static String[] getStringPlusValue(Object value) {
        if (value == null) {
            return new String[] {};
        }
        if (value instanceof String) {
            return new String[] { (String) value };
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) value;
            Iterator<?> iter = col.iterator();
            while (iter.hasNext()) {
                if (!(iter.next() instanceof String)) {
                    throw new IllegalArgumentException("Not a valid String+ property value: " + value);
                }
            }
            return col.toArray(new String[col.size()]);
        }
        throw new IllegalArgumentException("Not a valid String+ property value: " + value);
    }

    public static String getServletAlias(URL url) {
        String alias = url.getPath();
        if (alias.endsWith("/")) {
            alias = alias.substring(0, alias.length() - 1);
        }
        return alias;
    }

    public static String getConfigStringValue(BundleContext context, String key, Dictionary<String, ?> properties,
        String defaultValue) throws ConfigurationException {

        String value = null;
        if (properties != null && properties.get(key) != null) {
            value = properties.get(key).toString();
        }
        if (context != null && value == null) {
            value = context.getProperty(key);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static int getConfigIntValue(BundleContext context, String key, Dictionary<String, ?> properties,
        int defaultValue) throws ConfigurationException {

        String value = null;
        if (properties != null && properties.get(key) != null) {
            value = properties.get(key).toString();
        }
        if (context != null && value == null) {
            value = context.getProperty(key);
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            throw new ConfigurationException(key, "not an integer", e);
        }
    }
}
