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
package org.apache.cxf.jaxrs.ext.search.hibernate;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.persistence.Book;
import org.apache.cxf.jaxrs.ext.search.persistence.BookReview;
import org.apache.cxf.jaxrs.ext.search.persistence.Library;
import org.apache.cxf.jaxrs.ext.search.persistence.Name;
import org.apache.cxf.jaxrs.ext.search.persistence.OwnerAddress;
import org.apache.cxf.jaxrs.ext.search.persistence.OwnerInfo;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public abstract class AbstractHibernateVisitorTest extends Assert {

    private EntityManagerFactory emFactory;

    private EntityManager em;

    private Connection connection;
    private Session session;

    protected abstract SearchConditionParser<Book> getParser();
    protected abstract SearchConditionParser<Book> getParser(Map<String, String> visitorProps,
            Map<String, String> parserBinProps);
    
    @Before
    public void setUp() throws Exception {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:mem:books-jpa", "sa", "");
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during HSQL database init.");
        }
        try {
            emFactory = Persistence.createEntityManagerFactory("testUnitHibernate");
            em = emFactory.createEntityManager();
            session = (Session) (em.getDelegate());
         
            em.getTransaction().begin();
            
            Library lib = new Library();
            lib.setId(1);
            lib.setAddress("town");
            em.persist(lib);
            assertTrue(em.contains(lib));
            
            BookReview br1 = new BookReview();
            br1.setId(1);
            br1.setReview(BookReview.Review.BAD);
            br1.getAuthors().add("Ted");
            em.persist(br1);

            Book b1 = new Book();

            br1.setBook(b1);
            b1.getReviews().add(br1);


            b1.setId(9);
            b1.setBookTitle("num9");
            b1.setAddress(new OwnerAddress("Street1"));
            OwnerInfo info1 = new OwnerInfo();
            info1.setName(new Name("Fred"));
            info1.setDateOfBirth(parseDate("2000-01-01"));
            b1.setOwnerInfo(info1);
            b1.setLibrary(lib);
            b1.getAuthors().add("John");
            em.persist(b1);
            assertTrue(em.contains(b1));

            BookReview br2 = new BookReview();
            br2.setId(2);
            br2.setReview(BookReview.Review.GOOD);
            br2.getAuthors().add("Ted");
            em.persist(br2);

            Book b2 = new Book();
            b2.getReviews().add(br2);
            br2.setBook(b2);

            b2.setId(10);
            b2.setBookTitle("num10");
            b2.setAddress(new OwnerAddress("Street2"));
            OwnerInfo info2 = new OwnerInfo();
            info2.setName(new Name("Barry"));
            info2.setDateOfBirth(parseDate("2001-01-01"));
            b2.setOwnerInfo(info2);
            b2.setLibrary(lib);
            b2.getAuthors().add("John");
            em.persist(b2);
            assertTrue(em.contains(b2));

            BookReview br3 = new BookReview();
            br3.setId(3);
            br3.setReview(BookReview.Review.GOOD);
            br3.getAuthors().add("Ted");
            em.persist(br3);

            Book b3 = new Book();
            b3.getReviews().add(br3);
            br3.setBook(b3);
            b3.setId(11);
            b3.setBookTitle("num11");
            b3.setAddress(new OwnerAddress("Street3"));
            b3.getAuthors().add("Barry");
            OwnerInfo info3 = new OwnerInfo();
            info3.setName(new Name("Bill"));
            info3.setDateOfBirth(parseDate("2002-01-01"));
            b3.setOwnerInfo(info3);
            b3.setLibrary(lib);
            em.persist(b3);
            assertTrue(em.contains(b3));
            
            em.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during JPA EntityManager creation.");
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (em != null) {
                em.close();
            }
            if (emFactory != null) {
                emFactory.close();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();    
        } finally {    
            try {
                connection.createStatement().execute("SHUTDOWN");
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
    
    protected List<Book> queryBooks(String expression) throws Exception {
        return queryBooks(expression, null, null, null);
    }
    
    protected List<Book> queryBooks(String expression, 
                                  Map<String, String> visitorProps) throws Exception {
        return queryBooks(expression, visitorProps, null, null);
    }
    
    protected List<Book> queryBooks(String expression, 
                                  Map<String, String> visitorProps,
                                  Map<String, String> parserBinProps) throws Exception {
        return queryBooks(expression, visitorProps, parserBinProps, null);
    }
    
    protected List<Book> queryBooks(String expression, 
                                  Map<String, String> visitorProps,
                                  Map<String, String> parserBinProps,
                                  List<String> joinProps) throws Exception {
        SearchCondition<Book> filter = getParser(visitorProps, parserBinProps)
            .parse(expression);
        SearchConditionVisitor<Book, Criteria> jpa =
            new HibernateCriteriaVisitor<Book>(session, Book.class, visitorProps, joinProps);
        filter.accept(jpa);
        Criteria query = jpa.getQuery();
        return query.list();
    }
    
    protected long criteriaQueryBooksCount(String expression) throws Exception {
        SearchCondition<Book> filter = getParser().parse(expression);
        SearchConditionVisitor<Book, Criteria> jpa =
            new HibernateCriteriaVisitor<Book>(session, Book.class);
        filter.accept(jpa);
        Criteria query = jpa.getQuery();
        query.setProjection(Projections.rowCount());
        return (Long) query.uniqueResult();
    }

    protected Date parseDate(String value) throws Exception {
        return new SimpleDateFormat(SearchUtils.DEFAULT_DATE_FORMAT).parse(value);
    }
    
    public static class BookInfo {
        private int id;
        private String title;

        public BookInfo() {
            
        }
        
        public BookInfo(Integer id, String title) {
            this.id = id;
            this.title = title;
        }
        
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
