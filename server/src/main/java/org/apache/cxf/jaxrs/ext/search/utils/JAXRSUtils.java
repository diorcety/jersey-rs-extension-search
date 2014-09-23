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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

public class JAXRSUtils {
    private JAXRSUtils() {
    }

    /**
     * Retrieve map of query parameters from the passed in message
     * @param message
     * @return a Map of query parameters.
     */
    public static MultivaluedMap<String, String> getStructuredParams(String query,
            String sep,
            boolean decode,
            boolean decodePlus) {
        MultivaluedMap<String, String> map =
                new MetadataMap<String, String>(new LinkedHashMap<String, List<String>>());

        getStructuredParams(map, query, sep, decode, decodePlus);

        return map;
    }

    public static void getStructuredParams(MultivaluedMap<String, String> queries,
            String query,
            String sep,
            boolean decode,
            boolean decodePlus) {
        if (!StringUtils.isEmpty(query)) {
            List<String> parts = Arrays.asList(StringUtils.split(query, sep));
            for (String part : parts) {
                int index = part.indexOf('=');
                String name = null;
                String value = null;
                if (index == -1) {
                    name = part;
                    value = "";
                } else {
                    name = part.substring(0, index);
                    value =  index < part.length() ? part.substring(index + 1) : "";
                    if (decode || (decodePlus && value.contains("+"))) {
                        value = (";".equals(sep))
                                ? UrlUtils.pathDecode(value) : UrlUtils.urlDecode(value);
                    }
                }
                queries.add(UrlUtils.urlDecode(name), value);
            }
        }
    }
}
