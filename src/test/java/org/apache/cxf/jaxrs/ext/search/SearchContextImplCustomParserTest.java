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
package org.apache.cxf.jaxrs.ext.search;

import com.sun.jersey.api.core.HttpContext;

import org.junit.Assert;
import org.junit.Test;

import static org.easymock.EasyMock.createStrictMock;

public class SearchContextImplCustomParserTest extends Assert {

    @Test
    public void testQuery() {
        HttpContext httpContext = SearchContextImplTest.createHttpContext("$customfilter=color is red");
        httpContext.getProperties().put(SearchContextImpl.CUSTOM_SEARCH_QUERY_PARAM_NAME, "$customfilter");
        httpContext.getProperties().put(SearchContextImpl.CUSTOM_SEARCH_PARSER_PROPERTY, new CustomParser());
        SearchCondition<Color> sc = new SearchContextImpl(httpContext).getCondition(Color.class);
        
        assertTrue(sc.isMet(new Color("red")));
        assertFalse(sc.isMet(new Color("blue")));
    }
    
    private static class CustomParser implements SearchConditionParser<Color> {

        @Override
        public SearchCondition<Color> parse(String searchExpression) throws SearchParseException {
            if (!searchExpression.startsWith("color is ")) {
                throw new SearchParseException();
            }
            String value = searchExpression.substring(9);
            SearchCondition<Color> color = new PrimitiveSearchCondition<Color>("color", 
                                               value,
                                               ConditionType.EQUALS,
                                               new Color(value));
            
            return color;
        }
        
    }

    private static class Color {
        private String color;
        public Color(String color) {
            this.color = color;
        }

        @SuppressWarnings("unused")
        public String getColor() {
            return color;
        }
    }
}
