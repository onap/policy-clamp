/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ws.rs.BadRequestException;

import org.junit.Test;

/**
 * Test org.onap.clamp.ClampDesigner.model.Model
 */
public class CldsModelTest {

    @Test
    public void testCreateUsingControlName() {
        utilCreateUsingControlName("abc-", "7c42aceb-2350-11e6-8131-fa163ea8d2da");
        utilCreateUsingControlName("", "7c42aceb-2350-11e6-8131-fa163ea8d2da");
    }

    @Test(expected = BadRequestException.class)
    public void testExceptionCreateUsingControlName() {
        utilCreateUsingControlName("", "c42aceb-2350-11e6-8131-fa163ea8d2da");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateActionEmptyEvent() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.validateAction(CldsEvent.ACTION_CREATE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateActionNotExist() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.validateAction("unknown");
    }

    @Test
    public void testValidateActionFromCreate() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_CREATE);
        cldsModel.validateAction(CldsEvent.ACTION_SUBMIT);
        cldsModel.validateAction(CldsEvent.ACTION_TEST);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testValidateActionFromSubmitOrReSubmit() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_SUBMIT);
        cldsModel.validateAction(CldsEvent.ACTION_RESUBMIT);
        try {
            cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }

        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_RESUBMIT);
        cldsModel.validateAction(CldsEvent.ACTION_RESUBMIT);
        try {
            cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testValidateActionFromDistribute() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_DISTRIBUTE);
        cldsModel.validateAction(CldsEvent.ACTION_RESUBMIT);
        cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testValidateActionFromUndeploy() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_UNDEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_UPDATE);
        cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_RESUBMIT);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testValidateActionFromDeploy() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_DEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_UNDEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_UPDATE);
        cldsModel.validateAction(CldsEvent.ACTION_STOP);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testValidateActionFromRestartOrUpdate() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_RESTART);
        cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_UPDATE);
        cldsModel.validateAction(CldsEvent.ACTION_STOP);
        cldsModel.validateAction(CldsEvent.ACTION_UNDEPLOY);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }

        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_UPDATE);
        cldsModel.validateAction(CldsEvent.ACTION_DEPLOY);
        cldsModel.validateAction(CldsEvent.ACTION_UPDATE);
        cldsModel.validateAction(CldsEvent.ACTION_STOP);
        cldsModel.validateAction(CldsEvent.ACTION_UNDEPLOY);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }

    }

    @Test
    public void testValidateActionFromDelete() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_DELETE);
        cldsModel.validateAction(CldsEvent.ACTION_SUBMIT);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }

        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_DELETE);
        cldsModel.getEvent().setActionStateCd(CldsEvent.ACTION_STATE_SENT);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_SUBMIT);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testValidateActionFromStop() {
        CldsModel cldsModel = new CldsModel();
        cldsModel.getEvent().setActionCd(CldsEvent.ACTION_STOP);
        cldsModel.validateAction(CldsEvent.ACTION_UPDATE);
        cldsModel.validateAction(CldsEvent.ACTION_RESTART);
        cldsModel.validateAction(CldsEvent.ACTION_UNDEPLOY);

        try {
            cldsModel.validateAction(CldsEvent.ACTION_CREATE);
            fail("Exception should have been sent");
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Utility Method to create model from controlname and uuid.
     */
    public void utilCreateUsingControlName(String controlNamePrefix, String controlNameUuid) {
        CldsModel model = CldsModel.createUsingControlName(controlNamePrefix + controlNameUuid);
        assertEquals(controlNamePrefix, model.getControlNamePrefix());
        assertEquals(controlNameUuid, model.getControlNameUuid());
    }
}
