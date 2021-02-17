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

package org.onap.policy.clamp.controlloop.runtime.main.startstop;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import javax.ws.rs.core.Response;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.common.utils.resources.ResourceUtils;


/**
 * This class reads and handles command line parameters for the control loop runtime service.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class ClRuntimeCommandLineArguments {
    private static final String FILE_MESSAGE_PREAMBLE = " file \"";
    private static final int HELP_LINE_LENGTH = 120;

    private final Options options;
    private String configurationFilePath = null;

    /**
     * Construct the options for the control loop runtime component.
     */
    public ClRuntimeCommandLineArguments() {
        //@formatter:off
        options = new Options();
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("outputs the usage of this command")
                .required(false)
                .type(Boolean.class)
                .build());
        options.addOption(Option.builder("v")
                .longOpt("version")
                .desc("outputs the version of control loop runtime")
                .required(false)
                .type(Boolean.class)
                .build());
        options.addOption(Option.builder("c")
                .longOpt("config-file")
                .desc("the full path to the configuration file to use, "
                        + "the configuration file must be a Json file containing the control loop runtime parameters")
                .hasArg()
                .argName("CONFIG_FILE")
                .required(false)
                .type(String.class)
                .build());
        //@formatter:on
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
                    "invalid command line arguments specified", e);
        }

        // Arguments left over after Commons CLI does its stuff
        final String[] remainingArgs = commandLine.getArgs();

        if (remainingArgs.length > 0) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    "too many command line arguments specified : " + Arrays.toString(args));
        }

        if (remainingArgs.length == 1) {
            configurationFilePath = remainingArgs[0];
        }

        if (commandLine.hasOption('h')) {
            return help(Main.class.getName());
        }

        if (commandLine.hasOption('v')) {
            return version();
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
        validateReadableFile("control loop runtime configuration", configurationFilePath);
    }

    /**
     * Print version information for control loop runtime.
     *
     * @return the version string
     */
    public String version() {
        return ResourceUtils.getResourceAsString("version.txt");
    }

    /**
     * Print help information for control loop runtime.
     *
     * @param mainClassName the main class name
     * @return the help string
     */
    public String help(final String mainClassName) {
        final HelpFormatter helpFormatter = new HelpFormatter();
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        helpFormatter.printHelp(printWriter, HELP_LINE_LENGTH, mainClassName + " [options...]", "options", options, 0,
                0, "");

        return stringWriter.toString();
    }

    /**
     * Gets the configuration file path.
     *
     * @return the configuration file path
     */
    public String getConfigurationFilePath() {
        return configurationFilePath;
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
     * Sets the configuration file path.
     *
     * @param configurationFilePath the configuration file path
     */
    public void setConfigurationFilePath(final String configurationFilePath) {
        this.configurationFilePath = configurationFilePath;

    }

    /**
     * Validate readable file.
     *
     * @param fileTag the file tag
     * @param fileName the file name
     * @throws ControlLoopException on the file name passed as a parameter
     */
    private void validateReadableFile(final String fileTag, final String fileName) throws ControlLoopException {
        if (fileName == null || fileName.length() == 0) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    fileTag + " file was not specified as an argument");
        }

        // The file name refers to a resource on the local file system
        final URL fileUrl = ResourceUtils.getUrl4Resource(fileName);
        if (fileUrl == null) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" does not exist");
        }

        final File theFile = new File(fileUrl.getPath());
        if (!theFile.exists()) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" does not exist");
        }
        if (!theFile.isFile()) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" is not a normal file");
        }
        if (!theFile.canRead()) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" is ureadable");
        }
    }
}
