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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of util methods for the Http Admin Remote Sercvice Admin implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpAdminUtil {

    /**
     * Generate the method signature used to uniquely identity a method during remote
     * invocation.
     * 
     * @param method the method
     * @return the signature
     */
    public static String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append("(");
        for (Class<?> parameterType : method.getParameterTypes()) {
            appendTypeSignature(sb, parameterType);
        }
        sb.append(")");
        appendTypeSignature(sb, method.getReturnType());
        return sb.toString();
    }

    private static final Map<Class<?>, String> TYPESCODES = new HashMap<Class<?>, String>();
    static {
        TYPESCODES.put(Void.TYPE, "V");
        TYPESCODES.put(Boolean.TYPE, "Z");
        TYPESCODES.put(Character.TYPE, "C");
        TYPESCODES.put(Short.TYPE, "S");
        TYPESCODES.put(Integer.TYPE, "I");
        TYPESCODES.put(Long.TYPE, "J");
        TYPESCODES.put(Float.TYPE, "F");
        TYPESCODES.put(Double.TYPE, "D");
    }

    private static void appendTypeSignature(StringBuilder buffer, Class<?> clazz) {
        if (clazz.isArray()) {
            buffer.append("[");
            appendTypeSignature(buffer, clazz.getComponentType());
        }
        else if (clazz.isPrimitive()) {
            buffer.append(TYPESCODES.get(clazz));
        }
        else {
            buffer.append("L").append(clazz.getName().replaceAll("\\.", "/")).append(";");
        }
    }

    private HttpAdminUtil() {
    }
}
