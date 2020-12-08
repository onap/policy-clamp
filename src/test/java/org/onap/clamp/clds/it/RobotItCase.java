/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import java.io.File;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:robotframework/robotframework-test.properties")
@DirtiesContext
public class RobotItCase {

    @Value("${server.port}")
    private String httpPort;
    private static final int TIMEOUT_S = 150;
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(RobotItCase.class);

    @Test
    public void robotTests() throws Exception {
        File robotFolder = new File(getClass().getClassLoader().getResource("robotframework").getFile());
        Volume testsVolume = new Volume("/opt/robotframework/tests");
        DockerClient client = DockerClientBuilder
                .getInstance()
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                .build();


        BuildImageResultCallback callback = new BuildImageResultCallback() {
            @Override
            public void onNext(BuildResponseItem item) {
                System.out.println("XXX ITEM " + item);
                super.onNext(item);
            }
        };

        String imageId = client.buildImageCmd(robotFolder).exec(callback).awaitImageId();
        CreateContainerResponse createContainerResponse = client.createContainerCmd(imageId)
                .withVolumes(testsVolume)
                .withBinds(
                        new Bind(robotFolder.getAbsolutePath() + "/tests/", testsVolume, AccessMode.rw))
                .withEnv("CLAMP_PORT=" + httpPort)
                .withStopTimeout(TIMEOUT_S)
                .withNetworkMode("host")
                .exec();
        String id = createContainerResponse.getId();
        client.startContainerCmd(id).exec();
        InspectContainerResponse exec;

        int tries = 0;
        do {
            Thread.sleep(1000);
            exec = client.inspectContainerCmd(id).exec();
            tries++;
        } while (exec.getState().getRunning() && tries < TIMEOUT_S);
        Assert.assertEquals(exec.getState().getError(), 0L,
                Objects.requireNonNull(exec.getState().getExitCodeLong()).longValue());
        LogContainerCmd logContainerCmd = client.logContainerCmd(id);
        logContainerCmd.withStdOut(true).withStdErr(true);
        try {
            logContainerCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame item) {
                    logger.info(item.toString());
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new Exception("Failed to retrieve logs of container " + id, e);
        }
        client.stopContainerCmd(id);
    }
}
