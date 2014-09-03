/**
 * org.apache.cxf.jaxrs.ext.search.QueryContextImpl.java
 *
 * Copyright (c) 2007-2014 UShareSoft SAS, All rights reserved
 * @author UShareSoft
 */
package org.apache.cxf.jaxrs.ext.search;

import com.sun.jersey.api.core.HttpContext;

class QueryContextImpl implements QueryContext {

    private SearchContext searchContext;
    private HttpContext context;
    public QueryContextImpl(HttpContext context) {
        this.searchContext = new SearchContextImpl(context);
        this.context = context;
    }

    public String getConvertedExpression(String originalExpression) {
        return getConvertedExpression(originalExpression, SearchBean.class);
    }

    public <T> String getConvertedExpression(String originalExpression, Class<T> beanClass) {
        return getConvertedExpression(originalExpression, beanClass, String.class);

    }

    public <T, E> E getConvertedExpression(String originalExpression,
                                           Class<T> beanClass,
                                           Class<E> queryClass) {
        SearchConditionVisitor<T, E> visitor = getVisitor();
        if (visitor == null) {
            return null;
        }

        SearchCondition<T> cond = searchContext.getCondition(originalExpression, beanClass);
        if (cond == null) {
            return null;
        }
        cond.accept(visitor);
        return queryClass.cast(visitor.getQuery());

    }

    @SuppressWarnings("unchecked")
    private <T, Y> SearchConditionVisitor<T, Y> getVisitor() {
        Object visitor = context.getProperties().get(SearchUtils.SEARCH_VISITOR_PROPERTY);
        if (visitor == null) {
            return null;
        } else {
            //TODO: consider introducing SearchConditionVisitor.getBeanClass &&
            //      SearchConditionVisitor.getQueryClass to avoid such casts
            return (SearchConditionVisitor<T, Y>)visitor;
        }
    }
}
