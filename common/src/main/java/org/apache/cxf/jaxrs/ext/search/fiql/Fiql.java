/**
 * org.apache.cxf.jaxrs.ext.search.fiql.Fiql.java
 *
 * Copyright (c) 2007-2014 UShareSoft SAS, All rights reserved
 * @author UShareSoft
 */
package org.apache.cxf.jaxrs.ext.search.fiql;

import org.apache.cxf.jaxrs.ext.search.ConditionType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Fiql {

    public static final String OR = ",";
    public static final String AND = ";";

    public static final String GT = "=gt=";
    public static final String GE = "=ge=";
    public static final String LT = "=lt=";
    public static final String LE = "=le=";
    public static final String EQ = "==";
    public static final String NEQ = "!=";

    public static final Map<ConditionType, String> CONDITION_MAP;

    public static final String SUPPORT_SINGLE_EQUALS = "fiql.support.single.equals.operator";
    public static final String EXTENSION_COUNT = "count";
    public static final String EXTENSION_COUNT_OPEN = EXTENSION_COUNT + "(";

    public static final Map<String, ConditionType> OPERATORS_MAP;
    public static final Pattern COMPARATORS_PATTERN;
    public static final Pattern COMPARATORS_PATTERN_SINGLE_EQUALS;

    static {
        // operatorsMap
        OPERATORS_MAP = new HashMap<String, ConditionType>();
        OPERATORS_MAP.put(GT, ConditionType.GREATER_THAN);
        OPERATORS_MAP.put(GE, ConditionType.GREATER_OR_EQUALS);
        OPERATORS_MAP.put(LT, ConditionType.LESS_THAN);
        OPERATORS_MAP.put(LE, ConditionType.LESS_OR_EQUALS);
        OPERATORS_MAP.put(EQ, ConditionType.EQUALS);
        OPERATORS_MAP.put(NEQ, ConditionType.NOT_EQUALS);

        CONDITION_MAP = new HashMap<ConditionType, String>();
        CONDITION_MAP.put(ConditionType.GREATER_THAN, GT);
        CONDITION_MAP.put(ConditionType.GREATER_OR_EQUALS, GE);
        CONDITION_MAP.put(ConditionType.LESS_THAN, LT);
        CONDITION_MAP.put(ConditionType.LESS_OR_EQUALS, LE);
        CONDITION_MAP.put(ConditionType.EQUALS, EQ);
        CONDITION_MAP.put(ConditionType.NOT_EQUALS, NEQ);


        // pattern
        String comparators = GT + "|" + GE + "|" + LT + "|" + LE + "|" + EQ + "|" + NEQ;
        String s1 = "[\\p{ASCII}]+(" + comparators + ")";
        COMPARATORS_PATTERN = Pattern.compile(s1);

        String s2 = "[\\p{ASCII}]+(" + comparators + "|" + "=" + ")";
        COMPARATORS_PATTERN_SINGLE_EQUALS = Pattern.compile(s2);
    }
}
