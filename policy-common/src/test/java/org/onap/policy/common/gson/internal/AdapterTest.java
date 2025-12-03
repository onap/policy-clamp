/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023, 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.gson.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.JacksonExclusionStrategy;
import org.onap.policy.common.gson.annotation.GsonJsonProperty;
import org.onap.policy.common.gson.internal.Adapter.Factory;
import org.onap.policy.common.gson.internal.DataAdapterFactory.Data;
import org.onap.policy.common.gson.internal.DataAdapterFactory.DerivedData;
import org.springframework.test.util.ReflectionTestUtils;

class AdapterTest {
    private static final String GET_INVALID_NAME = "get$InvalidName";
    private static final String SET_INVALID_NAME = "set$InvalidName";
    private static final String EMPTY_ALIAS = "emptyAlias";
    private static final String GET_VALUE = ".getValue";
    private static final String GET_VALUE_NAME = "getValue";
    private static final String SET_VALUE_NAME = "setValue";
    private static final String VALUE_NAME = "value";
    private static final String MY_NAME = AdapterTest.class.getName();
    private static final String FACTORY_FIELD = "factory";

    private static DataAdapterFactory dataAdapter = new DataAdapterFactory();

    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(dataAdapter)
                    .setExclusionStrategies(new JacksonExclusionStrategy()).create();

    private static Factory saveFactory;

    /*
     * The remaining fields are just used within the tests.
     */

    private String value;

    // empty alias - should use field name
    @GsonJsonProperty("")
    protected String emptyAlias;

    @GsonJsonProperty("name-with-alias")
    protected String nameWithAlias;

    protected String unaliased;

    private List<Data> listField;

    private Data dataField;

    @BeforeAll
    public static void setUpBeforeClass() {
        saveFactory = (Factory) ReflectionTestUtils.getField(Adapter.class, FACTORY_FIELD);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(Adapter.class, FACTORY_FIELD, saveFactory);
    }

    @Test
    void testIsManagedField() {
        assertTrue(Adapter.isManaged(field(VALUE_NAME)));

        // return an invalid field name
        Factory factory = mock(Factory.class);
        when(factory.getName(any(Field.class))).thenReturn("$invalidFieldName");
        ReflectionTestUtils.setField(Adapter.class, FACTORY_FIELD, factory);
        assertFalse(Adapter.isManaged(field(VALUE_NAME)));
    }

    @Test
    void testIsManagedMethod() {
        assertTrue(Adapter.isManaged(mget(GET_VALUE_NAME)));

        // return an invalid method name
        Factory factory = mock(Factory.class);
        ReflectionTestUtils.setField(Adapter.class, FACTORY_FIELD, factory);

        when(factory.getName(any(Method.class))).thenReturn(GET_INVALID_NAME);
        assertFalse(Adapter.isManaged(mget(GET_VALUE_NAME)));

        when(factory.getName(any(Method.class))).thenReturn(SET_INVALID_NAME);
        assertFalse(Adapter.isManaged(mset(SET_VALUE_NAME)));
    }

    @Test
    void testAdapterField_Converter() {
        Adapter adapter = new Adapter(gson, field("dataField"));

        // first, write something of type Data
        dataAdapter.reset();
        dataField = new Data(300);
        JsonElement tree = adapter.toJsonTree(dataField);
        assertEquals("{'id':300}".replace('\'', '"'), tree.toString());

        // now try a subclass
        dataAdapter.reset();
        dataField = new DerivedData(300, "three");
        tree = adapter.toJsonTree(dataField);
        assertEquals("{'id':300,'text':'three'}".replace('\'', '"'), tree.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAdapterField_Converter_List() {
        listField = DataAdapterFactory.makeList();

        Adapter adapter = new Adapter(gson, field("listField"));

        dataAdapter.reset();
        JsonElement tree = adapter.toJsonTree(listField);
        assertTrue(dataAdapter.isDataWritten());
        assertEquals(DataAdapterFactory.ENCODED_LIST, tree.toString());

        // encode it twice so it uses the cached converter
        dataAdapter.reset();
        tree = adapter.toJsonTree(listField);
        assertTrue(dataAdapter.isDataWritten());
        assertEquals(DataAdapterFactory.ENCODED_LIST, tree.toString());

        dataAdapter.reset();
        List<Data> lst2 = (List<Data>) adapter.fromJsonTree(tree);
        assertTrue(dataAdapter.isDataRead());

        assertEquals(listField.toString(), lst2.toString());

        // decode it twice so it uses the cached converter
        dataAdapter.reset();
        lst2 = (List<Data>) adapter.fromJsonTree(tree);
        assertTrue(dataAdapter.isDataRead());

        assertEquals(listField.toString(), lst2.toString());
    }

    @Test
    void testAdapterMethod_Converter() throws Exception {
        listField = DataAdapterFactory.makeList();

        Method getter = mget("getMyList");

        Adapter aget = new Adapter(gson, getter, getter.getReturnType());

        dataAdapter.reset();
        JsonElement tree = aget.toJsonTree(listField);
        assertTrue(dataAdapter.isDataWritten());
        assertEquals(DataAdapterFactory.ENCODED_LIST, tree.toString());

        Method setter = AdapterTest.class.getDeclaredMethod("setMyList", List.class);
        Adapter aset = new Adapter(gson, setter, setter.getGenericParameterTypes()[0]);

        dataAdapter.reset();
        @SuppressWarnings("unchecked")
        List<Data> lst2 = (List<Data>) aset.fromJsonTree(tree);
        assertTrue(dataAdapter.isDataRead());

        assertEquals(listField.toString(), lst2.toString());
    }

    @Test
    void testGetPropName_testGetFullName_testMakeError() {
        // test field
        Adapter adapter = new Adapter(gson, field(VALUE_NAME));

        assertEquals(VALUE_NAME, adapter.getPropName());
        assertEquals(MY_NAME + ".value", adapter.getFullName());


        // test getter
        adapter = new Adapter(gson, mget(GET_VALUE_NAME), String.class);

        assertEquals(VALUE_NAME, adapter.getPropName());
        assertEquals(MY_NAME + GET_VALUE, adapter.getFullName());

        assertEquals("hello: " + MY_NAME + GET_VALUE, adapter.makeError("hello: "));


        // test setter
        adapter = new Adapter(gson, mset(SET_VALUE_NAME), String.class);

        assertEquals(VALUE_NAME, adapter.getPropName());
        assertEquals(MY_NAME + ".setValue", adapter.getFullName());
    }

    @Test
    void testToJsonTree() {
        Adapter adapter = new Adapter(gson, field(VALUE_NAME));

        JsonElement tree = adapter.toJsonTree("hello");
        assertTrue(tree.isJsonPrimitive());
        assertEquals("hello", tree.getAsString());
    }

    @Test
    void testFromJsonTree() {
        Adapter adapter = new Adapter(gson, field(VALUE_NAME));

        assertEquals("world", adapter.fromJsonTree(new JsonPrimitive("world")));
    }

    @Test
    void testDetmPropName() {
        assertEquals(EMPTY_ALIAS, Adapter.detmPropName(field(EMPTY_ALIAS)));
        assertEquals("name-with-alias", Adapter.detmPropName(field("nameWithAlias")));
        assertEquals("unaliased", Adapter.detmPropName(field("unaliased")));

        // return an invalid field name
        Factory factory = mock(Factory.class);
        when(factory.getName(any(Field.class))).thenReturn("$invalidFieldName");
        ReflectionTestUtils.setField(Adapter.class, FACTORY_FIELD, factory);
        assertEquals(null, Adapter.detmPropName(field(VALUE_NAME)));
    }

    @Test
    void testDetmGetterPropName() {
        assertEquals(EMPTY_ALIAS, Adapter.detmGetterPropName(mget("getEmptyAlias")));
        assertEquals("get-with-alias", Adapter.detmGetterPropName(mget("getWithAlias")));
        assertEquals("plain", Adapter.detmGetterPropName(mget("getPlain")));
        assertEquals("primBool", Adapter.detmGetterPropName(mget("isPrimBool")));
        assertEquals("boxedBool", Adapter.detmGetterPropName(mget("isBoxedBool")));
        assertEquals(null, Adapter.detmGetterPropName(mget("isString")));
        assertEquals(null, Adapter.detmGetterPropName(mget("noGet")));
        assertEquals(null, Adapter.detmGetterPropName(mget("get")));

        // return an invalid method name
        Factory factory = mock(Factory.class);
        ReflectionTestUtils.setField(Adapter.class, FACTORY_FIELD, factory);

        when(factory.getName(any(Method.class))).thenReturn(GET_INVALID_NAME);
        assertEquals(null, Adapter.detmGetterPropName(mget(GET_VALUE_NAME)));
    }

    @Test
    void testDetmSetterPropName() {
        assertEquals(EMPTY_ALIAS, Adapter.detmSetterPropName(mset("setEmptyAlias")));
        assertEquals("set-with-alias", Adapter.detmSetterPropName(mset("setWithAlias")));
        assertEquals("plain", Adapter.detmSetterPropName(mset("setPlain")));
        assertEquals(null, Adapter.detmSetterPropName(mset("noSet")));
        assertEquals(null, Adapter.detmSetterPropName(mset("set")));

        // return an invalid method name
        Factory factory = mock(Factory.class);
        ReflectionTestUtils.setField(Adapter.class, FACTORY_FIELD, factory);

        when(factory.getName(any(Method.class))).thenReturn(SET_INVALID_NAME);
        assertEquals(null, Adapter.detmSetterPropName(mset(SET_VALUE_NAME)));
    }

    @Test
    void testGetQualifiedNameField() throws Exception {
        assertEquals(MY_NAME + ".value", Adapter.getQualifiedName(AdapterTest.class.getDeclaredField(VALUE_NAME)));
    }

    @Test
    void testGetQualifiedNameMethod() {
        assertEquals(MY_NAME + GET_VALUE, Adapter.getQualifiedName(mget(GET_VALUE_NAME)));
    }

    /**
     * Gets a field from this class, by name.
     *
     * @param name name of the field to get
     * @return the field
     */
    private Field field(String name) {
        try {
            return AdapterTest.class.getDeclaredField(name);

        } catch (SecurityException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a "getter" method from this class, by name.
     *
     * @param name name of the method to get
     * @return the method
     */
    private Method mget(String name) {
        try {
            return AdapterTest.class.getDeclaredMethod(name);

        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a "setter" method from this class, by name.
     *
     * @param name name of the method to get
     * @return the method
     */
    private Method mset(String name) {
        try {
            return AdapterTest.class.getDeclaredMethod(name, String.class);

        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * The remaining methods are just used within the tests.
     */

    protected String getValue() {
        return value;
    }

    // empty alias - should use method name
    @GsonJsonProperty("")
    protected String getEmptyAlias() {
        return "";
    }

    @GsonJsonProperty("get-with-alias")
    protected String getWithAlias() {
        return "";
    }

    // no alias, begins with "get"
    protected String getPlain() {
        return "";
    }

    // begins with "is", returns primitive boolean
    protected boolean isPrimBool() {
        return true;
    }

    // begins with "is", returns boxed Boolean
    protected Boolean isBoxedBool() {
        return true;
    }

    // begins with "is", but doesn't return a boolean
    protected String isString() {
        return "";
    }

    // doesn't begin with "get"
    protected String noGet() {
        return "";
    }

    // nothing after "get"
    protected String get() {
        return "";
    }


    protected void setValue(String text) {
        // do nothing
    }

    // empty alias - should use method name
    @GsonJsonProperty("")
    protected void setEmptyAlias(String text) {
        // do nothing
    }

    @GsonJsonProperty("set-with-alias")
    protected void setWithAlias(String text) {
        // do nothing
    }

    // no alias, begins with "set"
    protected void setPlain(String text) {
        // do nothing
    }

    // doesn't begin with "set"
    protected void noSet(String text) {
        // do nothing
    }

    // nothing after "get"
    protected void set(String text) {
        // do nothing
    }

    // returns a list
    protected List<Data> getMyList() {
        return listField;
    }

    // accepts a list
    protected void setMyList(List<Data> newList) {
        listField = newList;
    }
}
