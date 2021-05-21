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

package org.onap.policy.clamp.controlloop.participant.policy.main.startstop;

import java.util.Arrays;
import javax.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.common.startstop.CommonCommandLineArguments;
import org.onap.policy.common.utils.resources.ResourceUtils;

/**
 * This class reads and handles command line parameters for the policy participant.
 *
 */
public class ParticipantPolicyCommandLineArguments {
    private static final String FILE_MESSAGE_PREAMBLE = " file \"";
    private static final int HELP_LINE_LENGTH = 120;

    private final Options options;
    private final CommonCommandLineArguments commonCommandLineArguments;

    @Getter()
    @Setter()
    private String configurationFilePath = null;

    /**
     * Construct the options for the policy participant.
     */
    public ParticipantPolicyCommandLineArguments() {
        options = new Options();
        commonCommandLineArguments = new CommonCommandLineArguments(options);
    }

    /**
     * Construct the options for the CLI editor and parse in the given arguments.
     *
     * @param args The command line arguments
     */
    public ParticipantPolicyCommandLineArguments(final String[] args) {
        // Set up the options with the default constructor
        this();

        // Parse the arguments
        try {
            parse(args);
        } catch (final ControlLoopException e) {
            throw new ControlLoopRuntimeException(Response.Status.NOT_ACCEPTABLE,
                    "parse error on policy participant parameters", e);
        }
    }

    /**
     * Parse the command line options.
     *
     * @param args The command line arguments
     * @return a string with a message for help and version, or null if there is no message
     * @throws ControlLoopException on command argument errors
     */
    public String parse(final String[] args) throws ControlLoopException {
        // Clear all our arguments
        setConfigurationFilePath(null);
        CommandLine commandLine = null;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (final ParseException e) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    "invalid command line arguments specified : " + e.getMessage());
        }

        // Arguments left over after Commons CLI does its stuff
        final String[] remainingArgs = commandLine.getArgs();

        if (remainingArgs.length > 0) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    "too many command line arguments specified : " + Arrays.toString(args));
        }

        if (commandLine.hasOption('h')) {
            return commonCommandLineArguments.help(Main.class.getName(), options);
        }

        if (commandLine.hasOption('v')) {
            return commonCommandLineArguments.version();
        }

        if (commandLine.hasOption('c')) {
            setConfigurationFilePath(commandLine.getOptionValue('c'));
        }

        return null;
    }

    /**
     * Validate the command line options.
     *
     * @throws ControlLoopException on command argument validation errors
     */
    public void validate() throws ControlLoopException {
        commonCommandLineArguments.validate(configurationFilePath);
    }

    /**
     * Gets the full expanded configuration file path.
     *
     * @return the configuration file path
     */
    public String getFullConfigurationFilePath() {
        return ResourceUtils.getFilePath4Resource(getConfigurationFilePath());
    }

    /**
     * Check set configuration file path.
     *
     * @return true, if check set configuration file path
     */
    public boolean checkSetConfigurationFilePath() {
        return !StringUtils.isEmpty(configurationFilePath);
    }
}
