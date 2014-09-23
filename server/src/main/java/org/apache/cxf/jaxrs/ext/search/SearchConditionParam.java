package org.apache.cxf.jaxrs.ext.search;

/**
 * org.apache.cxf.jaxrs.ext.search.SearchConditionParam.java
 *
 * Copyright (c) 2007-2014 UShareSoft SAS, All rights reserved
 *
 * @author UShareSoft
 */
public @interface SearchConditionParam {
    /**
     * Defines the name of the HTTP query parameter whose value will be used
     * to initialize the value of the annotated method argument, class field or
     * bean property. The name is specified in decoded form, any percent encoded
     * literals within the value will not be decoded and will instead be
     * treated as literal text. E.g. if the parameter name is "a b" then the
     * value of the annotation is "a b", <i>not</i> "a+b" or "a%20b".
     */
    String value();
}
