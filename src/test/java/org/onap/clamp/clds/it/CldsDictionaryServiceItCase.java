/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.onap.clamp.clds.model.CldsDictionary;
import org.onap.clamp.clds.model.CldsDictionaryItem;
import org.onap.clamp.clds.service.CldsDictionaryService;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test CLDS Dictionary Service APIs.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CldsDictionaryServiceItCase {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsDictionaryServiceItCase.class);
    @Autowired
    private CldsDictionaryService cldsDictionaryService;
    private Authentication authentication;
    private CldsDictionary cldsDictionary;
    private CldsDictionaryItem cldsDictionaryItem;
    private List<GrantedAuthority> authList = new LinkedList<GrantedAuthority>();
    private LoggingUtils util;

    /**
     * Setup the variable before the tests execution.
     *
     * @throws IOException
     *         In case of issues when opening the files
     */
    @Before
    public void setupBefore() throws IOException {
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-cl|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-template|dev|update"));
        authList.add(new SimpleGrantedAuthority("permission-type-filter-vf|dev|*"));
        authList.add(new SimpleGrantedAuthority("permission-type-tosca|dev|read"));
        authList.add(new SimpleGrantedAuthority("permission-type-tosca|dev|update"));
        authentication = new UsernamePasswordAuthenticationToken(new User("admin", "", authList), "", authList);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        util = Mockito.mock(LoggingUtils.class);
        Mockito.doNothing().when(util).entering(Matchers.any(HttpServletRequest.class), Matchers.any(String.class));
        cldsDictionaryService.setLoggingUtil(util);

        cldsDictionaryService.setSecurityContext(securityContext);

        cldsDictionary = new CldsDictionary();

        cldsDictionary.setDictionaryName("TestDictionary");
        ResponseEntity entity = cldsDictionaryService.createOrUpdateDictionary("TestDictionary", cldsDictionary);
        cldsDictionary = (CldsDictionary) entity.getBody();

        cldsDictionaryItem = new CldsDictionaryItem();
        cldsDictionaryItem.setDictElementShortName("TestDictionaryItemShortName");
        cldsDictionaryItem.setDictElementName("TestDictionaryItemName");
        cldsDictionaryItem.setDictElementType("string");
        cldsDictionaryItem.setDictionaryId(cldsDictionary.getDictionaryId());
        cldsDictionaryItem.setDictElementDesc("TestDictionaryItemDesc");
        cldsDictionaryService.createOrUpdateDictionaryElements("TestDictionary", cldsDictionaryItem);

        logger.info("Initial Clds Dictionary uploaded in DB:" + cldsDictionaryItem);
    }

    @Test
    public void testCreateOrUpdateDictionary() throws Exception {
        ResponseEntity<CldsDictionary> responseEntity = cldsDictionaryService.createOrUpdateDictionary("TestDictionary",
            cldsDictionary);
        CldsDictionary dictionary = responseEntity.getBody();
        assertNotNull(dictionary);
        logger.info("CLDS Dictionary is:" + dictionary);
        assertEquals("TestDictionary", dictionary.getDictionaryName());
    }

    @Test
    public void testCreateOrUpdateDictionaryElements() throws Exception {
        cldsDictionaryItem = new CldsDictionaryItem();
        cldsDictionaryItem.setDictElementShortName("TestDictionaryItemShortName1");
        cldsDictionaryItem.setDictElementName("TestDictionaryItemName1");
        cldsDictionaryItem.setDictElementType("string");
        cldsDictionaryItem.setDictionaryId(cldsDictionary.getDictionaryId());
        cldsDictionaryItem.setDictElementDesc("TestDictionaryItemDesc1");

        ResponseEntity<CldsDictionaryItem> responseEntity = cldsDictionaryService
            .createOrUpdateDictionaryElements("TestDictionary", cldsDictionaryItem);
        CldsDictionaryItem dictionaryItem = responseEntity.getBody();
        assertNotNull(dictionaryItem);
        logger.info("CLDS Dictionary Item is:" + dictionaryItem);
        assertEquals("TestDictionaryItemName1", dictionaryItem.getDictElementName());
    }

    @Test
    public void testGetAllDictionaryNames() throws Exception {
        ResponseEntity<List<CldsDictionary>> responseEntity = cldsDictionaryService.getAllDictionaryNames();
        List<CldsDictionary> dictionaries = responseEntity.getBody();
        assertNotNull(dictionaries);
        logger.info("CLDS Dictionary List is:" + dictionaries);
    }

    @Test
    public void testGetDictionaryElementsByName() throws Exception {
        ResponseEntity<List<CldsDictionaryItem>> responseEntity = cldsDictionaryService
            .getDictionaryElementsByName("TestDictionary");
        List<CldsDictionaryItem> dictionaryItems = responseEntity.getBody();
        assertNotNull(dictionaryItems);
        logger.info("CLDS Dictionary Item LIst is:" + dictionaryItems);
    }
}
