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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
public class SearchContextParamProvider implements InjectableProvider<SearchContextParam, Parameter> {

    private static final class SearchContextInjectable extends AbstractHttpContextInjectable<SearchContext> {
        SearchContextInjectable() {
        }

        @Override
        public SearchContext getValue(HttpContext context) {
            return new SearchContextImpl(context);
        }
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable getInjectable(ComponentContext ic, SearchContextParam annotation, final Parameter parameter) {
        if (!(parameter.getParameterClass().isAssignableFrom(SearchContext.class))) {
            return new SearchContextInjectable();
        }
        return null;
    }
}
