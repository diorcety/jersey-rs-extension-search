/**
 * org.apache.cxf.jaxrs.ext.search.SearchConditionParam.java
 *
 * Copyright (c) 2007-2014 UShareSoft SAS, All rights reserved
 * @author UShareSoft
 */
package org.apache.cxf.jaxrs.ext.search;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.Provider;

@Provider
public class SearchConditionParamProvider implements InjectableProvider<SearchConditionParam, Parameter> {

    private static final class SearchConditionInjectable extends AbstractHttpContextInjectable<SearchCondition> {

        private final String name;
        private final boolean decode;

        SearchConditionInjectable(String name, boolean decode) {
            this.name = name;
            this.decode = decode;
        }

        @Override
        public SearchCondition getValue(HttpContext context) {
            MultivaluedMap<String, String> queryParameters = context.getUriInfo().getQueryParameters(decode);
            String query = queryParameters.getFirst(name);
            if(query == null) {
                return null;
            }
            return null;
        }
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable getInjectable(ComponentContext ic, SearchConditionParam annotation, final Parameter parameter) {
        String parameterName = parameter.getSourceName();
        if (parameterName == null || parameterName.length() == 0) {
            // Invalid URI parameter name
            return null;
        }

        if (!(parameter.getParameterClass().isAssignableFrom(SearchCondition.class))) {
            return new SearchConditionInjectable(parameterName, parameter.isEncoded());
        }
        return null;
    }
}
