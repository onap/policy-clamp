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

package org.onap.policy.clamp.common.acm.startstop;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.ws.rs.core.Response;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.common.utils.resources.ResourceUtils;

/**
 * This class reads and handles command line parameters.
 *
 */
public class CommonCommandLineArguments {
    private static final String FILE_MESSAGE_PREAMBLE = " file \"";
    private static final int HELP_LINE_LENGTH = 120;

    /**
     * Construct the options for the policy participant.
     *
     * @param options the options for the command line
     */
    public CommonCommandLineArguments(final Options options) {
        //@formatter:off
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("outputs the usage of this command")
                .required(false)
                .type(Boolean.class)
                .build());
        options.addOption(Option.builder("v")
                .longOpt("version")
                .desc("outputs the version of policy participant")
                .required(false)
                .type(Boolean.class)
                .build());
        options.addOption(Option.builder("c")
                .longOpt("config-file")
                .desc("the full path to the configuration file to use, "
                        + "the configuration file must be a Json file containing the "
                        + "policy participant parameters")
                .hasArg()
                .argName("CONFIG_FILE")
                .required(false)
                .type(String.class)
                .build());
        //@formatter:on
    }

    /**
     * Validate the command line options.
     *
     * @param configurationFilePath the path to the configuration file
     * @throws AutomationCompositionException on command argument validation errors
     */
    public void validate(final String configurationFilePath) throws AutomationCompositionException {
        validateReadableFile("policy participant configuration", configurationFilePath);
    }

    /**
     * Print version information for policy participant.
     *
     * @return the version string
     */
    public String version() {
        return ResourceUtils.getResourceAsString("version.txt");
    }

    /**
     * Print help information for policy participant.
     *
     * @param mainClassName the main class name
     * @param options the options for the command
     * @return the help string
     */
    public String help(final String mainClassName, final Options options) {
        final var helpFormatter = new HelpFormatter();
        final var stringWriter = new StringWriter();
        final var printWriter = new PrintWriter(stringWriter);

        helpFormatter.printHelp(printWriter, HELP_LINE_LENGTH, mainClassName + " [options...]", "options", options, 0,
            0, "");

        return stringWriter.toString();
    }

    /**
     * Validate readable file.
     *
     * @param fileTag the file tag
     * @param fileName the file name
     * @throws AutomationCompositionException on the file name passed as a parameter
     */
    private void validateReadableFile(final String fileTag, final String fileName)
        throws AutomationCompositionException {
        if (StringUtils.isEmpty(fileName)) {
            throw new AutomationCompositionException(Response.Status.NOT_ACCEPTABLE,
                fileTag + " file was not specified as an argument");
        }

        // The file name refers to a resource on the local file system
        final var fileUrl = ResourceUtils.getUrl4Resource(fileName);
        if (fileUrl == null) {
            throw new AutomationCompositionException(Response.Status.NOT_ACCEPTABLE,
                fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" does not exist");
        }

        final var theFile = new File(fileUrl.getPath());
        if (!theFile.exists()) {
            throw new AutomationCompositionException(Response.Status.NOT_ACCEPTABLE,
                fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" does not exist");
        }
        if (!theFile.isFile()) {
            throw new AutomationCompositionException(Response.Status.NOT_ACCEPTABLE,
                fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" is not a normal file");
        }
        if (!theFile.canRead()) {
            throw new AutomationCompositionException(Response.Status.NOT_ACCEPTABLE,
                fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" is unreadable");
        }
    }
}
