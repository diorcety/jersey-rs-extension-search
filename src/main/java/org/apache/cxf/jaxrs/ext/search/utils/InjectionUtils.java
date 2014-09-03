/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.ext.search.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;

public final class InjectionUtils {
    
    private InjectionUtils() {
        
    }

    public static boolean isSupportedCollectionOrArray(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || type.isArray();
    }

    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type)
                || String.class == type;
    }

    public static Class<?> getActualType(Type genericType) {

        return getActualType(genericType, 0);
    }

    public static Class<?> getActualType(Type genericType, int pos) {

        if (genericType == null) {
            return null;
        }
        if (genericType == Object.class) {
            return (Class<?>)genericType;
        }
        if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            if (genericType instanceof TypeVariable) {
                genericType = getType(((TypeVariable<?>)genericType).getBounds(), pos);
            } else if (genericType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType)genericType;
                Type[] bounds = wildcardType.getLowerBounds();
                if (bounds.length == 0) {
                    bounds = wildcardType.getUpperBounds();
                }
                genericType = getType(bounds, pos);
            } else if (genericType instanceof GenericArrayType) {
                genericType = ((GenericArrayType)genericType).getGenericComponentType();
            }
            Class<?> cls = null;
            if (!(genericType instanceof ParameterizedType)) {
                cls = (Class<?>)genericType;
            } else {
                cls = (Class<?>)((ParameterizedType)genericType).getRawType();
            }
            return cls.isArray() ? cls.getComponentType() : cls;

        }
        ParameterizedType paramType = (ParameterizedType)genericType;
        Type t = getType(paramType.getActualTypeArguments(), pos);
        return t instanceof Class ? (Class<?>)t : getActualType(t, 0);
    }

    public static Type getType(Type[] types, int pos) {
        if (pos >= types.length) {
            throw new RuntimeException("No type can be found at position " + pos);
        }
        return types[pos];
    }

    public static <T> Object convertStringToPrimitive(String value, Class<?> cls) {
        if (String.class == cls) {
            return value;
        }
        if (cls.isPrimitive()) {
            return PrimitiveUtils.read(value, cls);
        } else if (cls.isEnum()) {
            try {
                Method m  = cls.getMethod("valueOf", new Class[]{String.class});
                return m.invoke(null, value.toUpperCase());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                Constructor<?> c = cls.getConstructor(new Class<?>[]{String.class});
                return c.newInstance(new Object[]{value});
            } catch (Throwable ex) {
                // try valueOf
            }
            try {
                Method m = cls.getMethod("valueOf", new Class[]{String.class});
                return cls.cast(m.invoke(null, value));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
