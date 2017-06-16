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

import java.util.HashMap;
import java.util.Map;

/**
 * Unified enumeration of the supported types according to {@code http://www.osgi.org/xmlns/rsa/v1.0.0/rsa.xsd}
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public enum ValueTypes {
    STRING_CLASS(String.class) {
        @Override
        public Object parse(String value) {
            return value;
        }
    },
    LONG_CLASS(Long.class) {
        @Override
        public Object parse(String value) {
            return new Long(Long.parseLong(value));
        }
    },
    LONG_TYPE(Long.TYPE) {
        @Override
        public Object parse(String value) {
            return Long.parseLong(value);
        }
    },
    INTEGER_CLASS(Integer.class) {
        @Override
        public Object parse(String value) {
            return new Integer(Integer.parseInt(value));
        }
    },
    INTEGER_TYPE(Integer.TYPE) {
        @Override
        public Object parse(String value) {
            // TODO Auto-generated method stub
            return null;
        }
    },
    BYTE_CLASS(Byte.class) {
        @Override
        public Object parse(String value) {
            return new Byte(Byte.parseByte(value));
        }
    },
    BYTE_TYPE(Byte.TYPE) {
        @Override
        public Object parse(String value) {
            return Byte.parseByte(value);
        }
    },
    CHAR_CLASS(Character.class) {
        @Override
        public Object parse(String value) {
            if (value.length() == 1) {
                return new Character(value.charAt(0));
            }
            else {
                throw new IllegalArgumentException();
            }
        }
    },
    CHAR_TYPE(Character.TYPE) {
        @Override
        public Object parse(String value) {
            if (value.length() == 1) {
                return value.charAt(0);
            }
            else {
                throw new IllegalArgumentException();
            }
        }
    },
    DOUBLE_CLASS(Double.class) {
        @Override
        public Object parse(String value) {
            return new Double(Double.parseDouble(value));
        }
    },
    DOUBLE_TYPE(Double.TYPE) {
        @Override
        public Object parse(String value) {
            return Double.parseDouble(value);
        }
    },
    FLOAT_CLASS(Float.class) {
        @Override
        public Object parse(String value) {
            return new Float(Float.parseFloat(value));
        }
    },
    FLOAT_TYPE(Float.TYPE) {
        @Override
        public Object parse(String value) {
            return Float.parseFloat(value);
        }
    },
    BOOLEAN_CLASS(Boolean.class) {
        @Override
        public Object parse(String value) {
            return new Boolean(Boolean.parseBoolean(value));
        }
    },
    BOOLEAN_TYPE(Boolean.TYPE) {
        @Override
        public Object parse(String value) {
            return Boolean.parseBoolean(value);
        }
    },
    SHORT_CLASS(Short.class) {
        @Override
        public Object parse(String value) {
            return new Short(Short.parseShort(value));
        }
    },
    SHORT_TYPE(Short.TYPE) {
        @Override
        public Object parse(String value) {
            return Short.parseShort(value);
        }
    };

    private final static Map<String, ValueTypes> NAMES_LOOKUP = new HashMap<String, ValueTypes>();
    private final static Map<Class<?>, ValueTypes> TYPES_LOOKUP = new HashMap<Class<?>, ValueTypes>();

    static {
        for (ValueTypes valueType : values()) {
            NAMES_LOOKUP.put(valueType.getName(), valueType);
            TYPES_LOOKUP.put(valueType.getType(), valueType);
        }
    }

    private final Class<?> m_type;
    private final String m_name;

    public static ValueTypes get(String name) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Name argument must not be empty");
        }
        return NAMES_LOOKUP.get(name);
    }

    public static ValueTypes get(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument must not be null");
        }
        return TYPES_LOOKUP.get(type);
    }

    private ValueTypes(Class<?> clazz) {
        m_type = clazz;
        m_name = clazz.getSimpleName();
    }

    public String getName() {
        return m_name;
    }

    public Class<?> getType() {
        return m_type;
    }

    public abstract Object parse(String value);
}
