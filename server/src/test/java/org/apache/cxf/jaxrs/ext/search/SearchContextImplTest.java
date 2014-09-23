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

import com.sun.jersey.api.core.ExtendedUriInfo;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.uri.UriComponent;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class SearchContextImplTest extends Assert {

    static HttpContext createHttpContext(String query) {
        URI uri = URI.create("http://dummy/pppp?" + UriComponent.encode(query, UriComponent.Type.QUERY));
        MultivaluedMap<String, String> stringStringMultivaluedMap = UriComponent.decodeQuery(uri, true);
        ExtendedUriInfo extendedUriInfo = createMock(ExtendedUriInfo.class);
        expect(extendedUriInfo.getQueryParameters(true)).andReturn(stringStringMultivaluedMap).anyTimes();
        expect(extendedUriInfo.getRequestUri()).andReturn(uri).anyTimes();
        replay(extendedUriInfo);

        HttpContext strictMock = createMock(HttpContext.class);
        expect(strictMock.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        expect(strictMock.getUriInfo()).andReturn(extendedUriInfo).anyTimes();
        replay(strictMock);
        return strictMock;
    }

    @Test
    public void testPlainQuery1() {
        HttpContext httpContext = createHttpContext("a=b");
        httpContext.getProperties().put("search.use.plain.queries", true);
        String exp = new SearchContextImpl(httpContext).getSearchExpression();
        assertEquals("a==b", exp);
    }
    
    @Test
    public void testWrongQueryNoException() {
        HttpContext httpContext = createHttpContext("_s=ab");
        httpContext.getProperties().put("search.block.search.exception", true);
        assertNull(new SearchContextImpl(httpContext).getCondition(Book.class));
    }
    
    @Test(expected = SearchParseException.class)
    public void testWrongQueryException() {
        HttpContext httpContext = createHttpContext("_s=ab");
        new SearchContextImpl(httpContext).getCondition(Book.class);
    }
    
    @Test
    public void testPlainQuery2() {
        HttpContext httpContext = createHttpContext("a=b&a=b1");
        httpContext.getProperties().put("search.use.plain.queries", true);
        String exp = new SearchContextImpl(httpContext).getSearchExpression();
        assertEquals("(a==b,a==b1)", exp);
    }
    
    @Test
    public void testPlainQuery3() {
        HttpContext httpContext = createHttpContext("a=b&c=d");
        httpContext.getProperties().put("search.use.plain.queries", true);
        String exp = new SearchContextImpl(httpContext).getSearchExpression();
        assertEquals("(a==b;c==d)", exp);
    }
    
    @Test
    public void testPlainQuery4() {
        HttpContext httpContext = createHttpContext("a=b&a=b2&c=d&f=g");
        httpContext.getProperties().put("search.use.plain.queries", true);
        String exp = new SearchContextImpl(httpContext).getSearchExpression();
        assertEquals("((a==b,a==b2);c==d;f==g)", exp);
    }
    
    @Test
    public void testPlainQuery5() {
        HttpContext httpContext = createHttpContext("aFrom=1&aTill=3");
        httpContext.getProperties().put("search.use.plain.queries", true);
        String exp = new SearchContextImpl(httpContext).getSearchExpression();
        assertEquals("(a=ge=1;a=le=3)", exp);
    }
    
    
    
    @Test
    public void testFiqlSearchCondition() {
        doTestFiqlSearchCondition(
            SearchContextImpl.SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
    }
    
    @Test
    public void testFiqlSearchConditionCustomQueryName() {
        HttpContext httpContext = createHttpContext("thequery" + "=" + "name==CXF%20Rocks;id=gt=123");
        httpContext.getProperties().put(SearchContextImpl.CUSTOM_SEARCH_QUERY_PARAM_NAME, "thequery");
        doTestFiqlSearchCondition(httpContext);
    }
    
    @Test
    public void testFiqlSearchBean() {
        doTestFiqlSearchBean(
            SearchContextImpl.SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConditionType() {
        SearchContext context = new SearchContextImpl(createHttpContext(""));
        context.getCondition(String.class);
    }
    @Test
    public void testFiqlSearchConditionWithShortQuery() {
        doTestFiqlSearchCondition(
            SearchContextImpl.SHORT_SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
    }
    
    @Test
    public void testFiqlSearchConditionWithNonFiqlQuery() {
        doTestFiqlSearchCondition(
            "_s=name==CXF%20Rocks;id=gt=123&a=b");
        doTestFiqlSearchCondition(
            "a=b&_s=name==CXF%20Rocks;id=gt=123");
        doTestFiqlSearchCondition(
            "a=b&_s=name==CXF%20Rocks;id=gt=123&c=d");
    }
    
    private void doTestFiqlSearchCondition(String queryString) {
        doTestFiqlSearchCondition(createHttpContext(queryString));
    }
    
    private void doTestFiqlSearchCondition(HttpContext m) {
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<Book> sc = context.getCondition(Book.class);
        assertNotNull(sc);
        
        List<Book> books = new ArrayList<Book>();
        books.add(new Book("CXF is cool", 125L));
        books.add(new Book("CXF Rocks", 125L));
        
        List<Book> found = sc.findAll(books);
        assertEquals(1, found.size());
        assertEquals(new Book("CXF Rocks", 125L), found.get(0));
    }
    
    private void doTestFiqlSearchBean(String queryString) {
        HttpContext httpContext = createHttpContext(queryString);
        SearchContext context = new SearchContextImpl(httpContext);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);
        
        List<SearchBean> beans = new ArrayList<SearchBean>();
        SearchBean sb1 = new SearchBean();
        sb1.set("name", "CXF is cool");
        beans.add(sb1);
        SearchBean sb2 = new SearchBean();
        sb2.set("name", "CXF Rocks");
        sb2.set("id", "124");
        beans.add(sb2);
        
        List<SearchBean> found = sc.findAll(beans);
        assertEquals(1, found.size());
        assertEquals(sb2, found.get(0));
        
        assertTrue(sc instanceof AndSearchCondition);
        assertNull(sc.getStatement());
        List<SearchCondition<SearchBean>> scs = sc.getSearchConditions();
        assertEquals(2, scs.size());
        SearchCondition<SearchBean> sc1 = scs.get(0);
        assertEquals("name", sc1.getStatement().getProperty());
        SearchCondition<SearchBean> sc2 = scs.get(1);
        assertEquals("id", sc2.getStatement().getProperty());
        
        assertTrue("123".equals(sc1.getStatement().getValue())
                   && "CXF Rocks".equals(sc2.getStatement().getValue())
                   || "123".equals(sc2.getStatement().getValue())
                   && "CXF Rocks".equals(sc1.getStatement().getValue()));
        
    }
    
    @Test
    public void testPrimitiveStatementSearchBean() {
        HttpContext httpContext = createHttpContext("_s=name==CXF");
        SearchContext context = new SearchContextImpl(httpContext);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);
        
        PrimitiveStatement ps = sc.getStatement();
        assertNotNull(ps);
        
        assertEquals("name", ps.getProperty());
        assertEquals("CXF", ps.getValue());
        assertEquals(ConditionType.EQUALS, ps.getCondition());
        assertEquals(String.class, ps.getValueType());
    }
    
    @Test
    public void testPrimitiveStatementSearchBeanComlexName() {
        HttpContext httpContext = createHttpContext("_s=complex.name==CXF");
        SearchContext context = new SearchContextImpl(httpContext);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);
        
        PrimitiveStatement ps = sc.getStatement();
        assertNotNull(ps);
        
        assertEquals("complex.name", ps.getProperty());
        assertEquals("CXF", ps.getValue());
        assertEquals(ConditionType.EQUALS, ps.getCondition());
        assertEquals(String.class, ps.getValueType());
    }
    
    @Test
    public void testSingleEquals() {
        HttpContext httpContext = createHttpContext("_s=name=CXF");
        httpContext.getProperties().put("fiql.support.single.equals.operator", "true");
        SearchContext context = new SearchContextImpl(httpContext);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);
        
        PrimitiveStatement ps = sc.getStatement();
        assertNotNull(ps);
        
        assertEquals("name", ps.getProperty());
        assertEquals("CXF", ps.getValue());
        assertEquals(ConditionType.EQUALS, ps.getCondition());
        assertEquals(String.class, ps.getValueType());
    }
    
    @Test
    public void testIsMetCompositeObject() throws Exception {
        SearchCondition<TheBook> filter = 
            new FiqlParser<TheBook>(TheBook.class,
                null,                          
                Collections.singletonMap("address", "address.street")).parse("address==Street1");
        
        TheBook b = new TheBook();
        b.setAddress(new TheOwnerAddress("Street1"));
        assertTrue(filter.isMet(b));
        
        b.setAddress(new TheOwnerAddress("Street2"));
        assertFalse(filter.isMet(b));
    }
    @Test
    public void testIsMetCompositeInterface() throws Exception {
        SearchCondition<TheBook> filter = 
            new FiqlParser<TheBook>(TheBook.class,
                null,                          
                Collections.singletonMap("address", "addressInterface.street"))
                    .parse("address==Street1");
        
        TheBook b = new TheBook();
        b.setAddress(new TheOwnerAddress("Street1"));
        assertTrue(filter.isMet(b));
        
        b.setAddress(new TheOwnerAddress("Street2"));
        assertFalse(filter.isMet(b));
    }
        
    public static class TheBook {
        private TheOwnerAddressInterface address;

        public TheOwnerAddress getAddress() {
            return (TheOwnerAddress)address;
        }

        public void setAddress(TheOwnerAddress a) {
            this.address = a;
        }
        
        public TheOwnerAddressInterface getAddressInterface() {
            return address;
        }

        public void setAddressInterface(TheOwnerAddressInterface a) {
            this.address = a;
        }
    }
    public interface TheOwnerAddressInterface {
        String getStreet();
        void setStreet(String street);
    }
    public static class TheOwnerAddress implements TheOwnerAddressInterface {
        private String street;

        public TheOwnerAddress() {
            
        }
        public TheOwnerAddress(String s) {
            this.street = s;
        }
        
        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }
    }
}
