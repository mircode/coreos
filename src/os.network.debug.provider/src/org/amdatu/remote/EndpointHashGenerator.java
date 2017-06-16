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

import static org.amdatu.remote.ServiceUtil.getStringPlusValue;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * Helper class that generates an deterministic endpoint ID for a map of endpoint properties.<p>
 * 
 * The implementation tries to minimize the spread by sorting and lowercasing keys as well as
 * sorting known properties where it does not harm the semantics.<p>
 * 
 * Example: objectClass=[A,B] is equivalent to objectClass=[B,A]
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EndpointHashGenerator {

    /**
     * Keys of String+ properties that can be sorted without changing the semantics
     * of the endpoint.
     */
    private final static Set<String> SORTABLE_STRING_KEYS = new HashSet<String>();
    static {
        SORTABLE_STRING_KEYS.add(Constants.OBJECTCLASS);
        SORTABLE_STRING_KEYS.add(RemoteConstants.SERVICE_EXPORTED_INTENTS);
        SORTABLE_STRING_KEYS.add(RemoteConstants.SERVICE_EXPORTED_INTERFACES);
    }

    public String hash(Map<String, ?> map) {
        MessageDigest digest = HashUtil.createDigester();
        List<String> keys = getSortedKeys(map);
        for (String key : keys) {
            Object value = getKeyValue(key, map);
            appendObject(digest, key);
            digest.update("=".getBytes());
            appendObject(digest, value);
            digest.update(",".getBytes());
        }
        return HashUtil.toHexString(digest.digest());
    }

    private static List<String> getSortedKeys(Map<String, ?> map) {
        List<String> keys = new ArrayList<String>(map.size());
        for (String key : map.keySet()) {
            keys.add(key.toLowerCase());
        }
        Collections.sort(keys);
        return keys;
    }

    private static Object getKeyValue(String key, Map<String, ?> map) {
        Object value = map.get(key);
        if (value == null) {
            for (Entry<String, ?> entry : map.entrySet()) {
                if (entry.getKey().toLowerCase().equals(key)) {
                    value = entry.getValue();
                }
            }
        }
        if (SORTABLE_STRING_KEYS.contains(key)) {
            List<String> list = Arrays.asList(getStringPlusValue(value));
            Collections.sort(list);
            value = list;
        }
        return value;
    }

    private static void appendObject(MessageDigest digest, Object value) {
        if (value.getClass().isArray()) {
            appendArray(digest, value);
        }
        else if (value instanceof Map<?, ?>) {
            appendMap(digest, (Map<?, ?>) value);
        }
        else if (value instanceof Set<?>) {
            appendSet(digest, (Set<?>) value);
        }
        else if (value instanceof List<?>) {
            appendList(digest, (List<?>) value);
        }
        else {
            digest.update(value.toString().getBytes());
        }
    }

    private static void appendMap(MessageDigest digest, Map<?, ?> map) {
        digest.update("M".getBytes());
        for (Entry<?, ?> entry : map.entrySet()) {
            appendObject(digest, entry.getKey());
            digest.update("=".getBytes());
            appendObject(digest, entry.getValue());
            digest.update(",".getBytes());
        }
    }

    private static void appendArray(MessageDigest digest, Object value) {
        digest.update("A".getBytes());
        for (int i = 0; i < Array.getLength(value); i++) {
            appendObject(digest, Array.get(value, i));
            digest.update(",".getBytes());
        }
    }

    private static void appendSet(MessageDigest digest, Set<?> value) {
        digest.update("S".getBytes());
        for (Object object : value) {
            appendObject(digest, object);
            digest.update(",".getBytes());
        }
    }

    private static void appendList(MessageDigest digest, List<?> value) {
        digest.update("L".getBytes());
        for (Object object : value) {
            appendObject(digest, object);
            digest.update(",".getBytes());
        }
    }
}
