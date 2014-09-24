/**
 * org.apache.cxf.jaxrs.ext.search.hibernate.HibernateCriteriaQueryVisitor.java
 *
 * Copyright (c) 2007-2014 UShareSoft SAS, All rights reserved
 * @author UShareSoft
 */
package org.apache.cxf.jaxrs.ext.search.hibernate;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.ext.search.utils.StringUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class HibernateCriteriaVisitor<T> extends AbstractSearchConditionVisitor<T, Criteria> {

    private final Session session;
    private static final String ALIAS_PREFIX = "__alias";
    private Class<T> queryClass;
    private Criteria cq;
    private ClassMetadata root;
    private Map<String, String> aliases = new LinkedHashMap<String, String>();
    private int aliasNum = 0;

    private Stack<List<Criterion>> predStack = new Stack<List<Criterion>>();
    private boolean criteriaFinalized;
    private Set<String> joinProperties;

    public HibernateCriteriaVisitor(Session session, Class<T> queryClass) {
        this(session, queryClass, null, null);
    }

    public HibernateCriteriaVisitor(Session session, Class<T> queryClass,
            Map<String, String> fieldMap) {
        this(session, queryClass, fieldMap, null);
    }

    public HibernateCriteriaVisitor(Session session, Class<T> queryClass,
            Map<String, String> fieldMap,
            List<String> joinProps) {
        super(fieldMap);
        this.session = session;
        this.queryClass = queryClass;
        this.joinProperties = joinProps == null ? null : new HashSet<String>(joinProps);
    }

    protected Criteria createCriteria(Class<?> queryClass) {
        return session.createCriteria(queryClass);
    }

    @Override
    public Criteria getQuery() {
        return getCriteriaQuery();
    }

    public void visit(SearchCondition<T> sc) {
        if (cq == null) {
            cq = createCriteria(queryClass);
            root = getSessionFactory().getClassMetadata(queryClass);
            predStack.push(new ArrayList<Criterion>());
        }
        if (sc.getStatement() != null) {
            predStack.peek().add(buildPredicate(sc.getStatement()));
        } else {
            predStack.push(new ArrayList<Criterion>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            List<Criterion> predsList = predStack.pop();
            if (predsList.size() > 0) {
                Criterion newPred = predsList.get(0);
                for (int i = 1; i < predsList.size(); ++i) {
                    if (sc instanceof OrSearchCondition) {
                        newPred = Restrictions.or(newPred, predsList.get(i));
                    } else {
                        newPred = Restrictions.and(newPred, predsList.get(i));
                    }
                }
                predStack.peek().add(newPred);
            }
        }
    }

    private SessionFactoryImplementor getSessionFactory() {
        return (SessionFactoryImplementor) session.getSessionFactory();
    }

    protected Class<T> getQueryClass() {
        return queryClass;
    }

    public Criteria getCriteriaQuery() {
        if (!criteriaFinalized) {
            List<Criterion> predsList = predStack.pop();
            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                cq.createAlias(entry.getKey(), entry.getValue());
            }
            for (Criterion criterion : predsList) {
                cq.add(criterion);
            }
            criteriaFinalized = true;
        }
        return cq;
    }

    private Criterion buildPredicate(PrimitiveStatement ps) {
        String name = ps.getProperty();
        Object propertyValue = ps.getValue();
        validatePropertyValue(name, propertyValue);

        name = super.getRealPropertyName(name);
        ClassValue cv = getPrimitiveFieldClass(ps,
                name,
                ps.getValue().getClass(),
                ps.getValueType(),
                propertyValue);
        CollectionCheckInfo collInfo = cv.getCollectionCheckInfo();

        /*
         Get the path and property names
         */
        Tuple<Type, String> path[] = getPath(root, name, cv, collInfo);

        Criterion pred = collInfo == null
                ? doBuildPredicate(ps.getCondition(), path, cv.getValue())
                : doBuildCollectionPredicate(ps.getCondition(), path, collInfo);

        return pred;
    }

    /*
     *
     * Path functions
     *
     */

    private Tuple<Type, String>[] getPath(ClassMetadata element, String name, ClassValue cv, CollectionCheckInfo collSize) {
        return getPath(element, StringUtils.split(name, "\\."), cv, collSize);
    }

    private Tuple<Type, String>[] getPath(ClassMetadata classMetadata, String[] name, ClassValue cv, CollectionCheckInfo collSize) {
        Tuple<Type, String>[] path = new Tuple[name.length];
        Object element = classMetadata;
        for (int i = 0; i < name.length; ++i) {
            Type propertyType = getPropertyType(element, name[i]);
            path[i] = new Tuple<Type, String>(propertyType, name[i]);
            if (propertyType.isCollectionType()) {
                CollectionType collectionType = (CollectionType) propertyType;
                String associatedEntityName = collectionType.getRole();
                CollectionPersister collectionPersister = getSessionFactory().getCollectionPersister(associatedEntityName);
                element = collectionPersister.getElementType();
            } else if (propertyType.isAssociationType()) {
                AssociationType associationType = (AssociationType) propertyType;
                String associatedEntityName = associationType.getAssociatedEntityName(getSessionFactory());
                element = getSessionFactory().getClassMetadata(associatedEntityName);
            } else if (propertyType.isComponentType()) {
                element = propertyType;
            }
        }
        return path;
    }

    private Type getPropertyType(Object element, String property) {
        if (element instanceof ClassMetadata) {
            return getPropertyType((ClassMetadata) element, property);
        } else if (element instanceof ComponentType) {
            return getPropertyType((ComponentType) element, property);
        } else if (element instanceof EntityType) {
            return getPropertyType((EntityType) element, property);
        }
        throw new IllegalArgumentException("Not handled: " + element.getClass());
    }

    private Type getPropertyType(ClassMetadata element, String property) {
        return element.getPropertyType(property);
    }

    private Type getPropertyType(EntityType entityType, String property) {
        String associatedEntityName = entityType.getAssociatedEntityName(getSessionFactory());
        ClassMetadata classMetadata = getSessionFactory().getClassMetadata(associatedEntityName);
        return getPropertyType(classMetadata, property);
    }

    private Type getPropertyType(ComponentType componentType, String property) {
        int propertyIndex = componentType.getPropertyIndex(property);
        return componentType.getSubtypes()[propertyIndex];
    }


    /*
     *
     * Create Hibernate expression (using alias) functions
     *
     */

    private synchronized String createAlias() {
        String s = ALIAS_PREFIX + aliasNum;
        aliasNum++;
        return s;
    }

    private String getAlias(String prop) {
        String alias = aliases.get(prop);
        if (alias == null) {
            alias = createAlias();
            aliases.put(prop, alias);
        }
        return alias;
    }


    private String getExpression(Tuple<Type, String> path[], boolean collection) {
        List<String> finalName = new LinkedList<String>();
        for (int i = 0; i < path.length; ++i) {
            Tuple<Type, String> p = path[i];
            Type propertyType = p.getFirst();
            finalName.add(p.getSecond());
            if (propertyType instanceof AssociationType) {
                /*
                 * 1 - Don't alias the last the last property
                 * 2 - Alias the last if the expression is not for a collection (see next part)
                 */
                if ((i < path.length - 1) || !collection) {
                    String pp = StringUtils.join(".", finalName.toArray(new String[finalName.size()]));
                    String alias = getAlias(pp);
                    finalName.clear();
                    finalName.add(alias);
                }

                /*
                 * If the expression is not for a collection
                 * And this is the last property append collection elements item
                 */
                if (i == (path.length - 1) && !collection) {
                    finalName.add(CollectionPropertyNames.COLLECTION_ELEMENTS);
                }
            }
        }
        return StringUtils.join(".", finalName.toArray(new String[finalName.size()]));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Criterion doBuildPredicate(ConditionType ct, Tuple<Type, String> path[], Object value) {
        String exp = getExpression(path, false);
        Criterion pred = null;

        switch (ct) {
            case GREATER_THAN:
                pred = Restrictions.gt(exp, value);
                break;
            case EQUALS:
                if (String.class.isInstance(value)) {
                    String theValue = SearchUtils.toSqlWildcardString(value.toString(), isWildcardStringMatch());
                    if (theValue.contains("%")) {
                        pred = Restrictions.like(exp, theValue);
                    } else {
                        pred = Restrictions.eq(exp, value);
                    }
                } else {
                    pred = Restrictions.eq(exp, value);
                }
                break;
            case NOT_EQUALS:
                if (String.class.isInstance(value)) {
                    String theValue = SearchUtils.toSqlWildcardString(value.toString(), isWildcardStringMatch());
                    if (theValue.contains("%")) {
                        pred = Restrictions.not(Restrictions.like(exp, theValue));
                    } else {
                        pred = Restrictions.ne(exp, value);
                    }
                } else {
                    pred = Restrictions.ne(exp, value);
                }
                break;
            case LESS_THAN:
                pred = Restrictions.lt(exp, value);
                break;
            case LESS_OR_EQUALS:
                pred = Restrictions.le(exp, value);
                break;
            case GREATER_OR_EQUALS:
                pred = Restrictions.ge(exp, value);
                break;
            default:
                break;
        }
        return pred;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Criterion doBuildCollectionPredicate(ConditionType ct, Tuple<Type, String> path[], CollectionCheckInfo collInfo) {
        Criterion pred = null;
        String exp = getExpression(path, true);
        Integer value = Integer.valueOf(collInfo.getCollectionCheckValue().toString());

        switch (ct) {
            case GREATER_THAN:
                pred = Restrictions.sizeGt(exp, value);
                break;
            case EQUALS:
                pred = Restrictions.sizeEq(exp, value);
                break;
            case NOT_EQUALS:
                pred = Restrictions.sizeNe(exp, value);
                break;
            case LESS_THAN:
                pred = Restrictions.sizeLt(exp, value);
                break;
            case LESS_OR_EQUALS:
                pred = Restrictions.sizeLe(exp, value);
                break;
            case GREATER_OR_EQUALS:
                pred = Restrictions.sizeGe(exp, value);
                break;
            default:
                break;
        }
        return pred;
    }

    public class Tuple<X, Y> {

        public final X first;
        public final Y second;

        public Tuple(X first, Y second) {
            this.first = first;
            this.second = second;
        }

        public X getFirst() {
            return first;
        }

        public Y getSecond() {
            return second;
        }
    }
}
