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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generic hashing  utilities.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HashUtil {

    /** The message digest algorithm used to fingerprint files. */
    private static final String DIGEST_ALG = "MD5";

    /** The hexadecimal alphabet. */
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    /**
     * Creates a new message digest instance.
     * 
     * @return a new {@link MessageDigest} instance, never <code>null</code>.
     * @throws RuntimeException in case the desired message digest algorithm is not
     *         supported by the platform.
     */
    public static MessageDigest createDigester() {
        try {
            return MessageDigest.getInstance(DIGEST_ALG);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("No such algorithm: " + DIGEST_ALG);
        }
    }

    /**
     * Creates a hexadecimal representation of all given bytes an concatenates these
     * hex-values to a single string.
     * 
     * @param bytes the byte values to convert, cannot be <code>null</code>.
     * @return a hex-string of the given bytes, never <code>null</code>.
     */
    public static String toHexString(byte[] bytes) {
        // based on <http://stackoverflow.com/a/9855338/229140>
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_DIGITS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String hash(String msg) {
        try {
            return toHexString(createDigester().digest(msg.getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported?!");
        }
    }

    public static String hash(Method m) {
        return hash(m.toGenericString());
    }
}
