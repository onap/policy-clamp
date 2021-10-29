/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.controlloop.common.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class CoderHttpMesageConverter<T> extends AbstractHttpMessageConverter<T> {

    private Coder coder;

    public CoderHttpMesageConverter(String type) {
        super(new MediaType("application", type, StandardCharsets.UTF_8));
        this.coder = "json".equals(type) ? new StandardCoder() : new StandardYamlCoder();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (var is = new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8)) {
            return coder.decode(is, clazz);
        } catch (CoderException e) {
            throw new ControlLoopRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try (var writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8)) {
            coder.encode(writer, t);
        } catch (CoderException e) {
            throw new ControlLoopRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }

}
