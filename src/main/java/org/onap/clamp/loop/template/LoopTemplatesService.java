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

package org.onap.clamp.loop.template;

import java.util.List;
import org.onap.clamp.clds.exception.sdc.controller.BlueprintParserException;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintMicroService;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintParser;
import org.onap.clamp.clds.sdc.controller.installer.ChainGenerator;
import org.onap.clamp.clds.util.drawing.SvgFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoopTemplatesService {

    private final LoopTemplatesRepository loopTemplatesRepository;

    @Autowired
    ChainGenerator chainGenerator;

    @Autowired
    private SvgFacade svgFacade;

    /**
     * Constructor.
     */
    @Autowired
    public LoopTemplatesService(LoopTemplatesRepository loopTemplatesRepository) {
        this.loopTemplatesRepository = loopTemplatesRepository;

    }

    public LoopTemplate saveOrUpdateLoopTemplate(LoopTemplate loopTemplate) {
        return loopTemplatesRepository.save(loopTemplate);
    }

    /**
     * Saves or updates loop template Object.
     *
     * @param templateName the loop template name
     * @param loopTemplate the loop template object
     * @return the loop template
     * @throws BlueprintParserException In case of issues with the blueprint
     *         parsing
     */
    public LoopTemplate saveOrUpdateLoopTemplateByName(String templateName,
        LoopTemplate loopTemplate) throws BlueprintParserException {

        if (getLoopTemplate(templateName) != null) {
            loopTemplate.setName(getLoopTemplate(templateName).getName());
        }
        return saveOrUpdateLoopTemplate(createTemplateFromBlueprint(templateName, loopTemplate));
    }

    public List<String> getLoopTemplateNames() {
        return loopTemplatesRepository.getAllLoopTemplateNames();
    }

    public List<LoopTemplate> getAllLoopTemplates() {
        return loopTemplatesRepository.findAll();
    }

    public LoopTemplate getLoopTemplate(String name) {
        return loopTemplatesRepository.findById(name).orElse(null);
    }

    public void deleteLoopTemplate(String name) {
        loopTemplatesRepository.deleteById(name);
    }

    private LoopTemplate createTemplateFromBlueprint(String templateName, LoopTemplate loopTemplate)
        throws BlueprintParserException {

        String blueprintYaml = loopTemplate.getBlueprint();
        List<BlueprintMicroService> microServicesChain =
            chainGenerator.getChainOfMicroServices(BlueprintParser.getMicroServices(blueprintYaml));
        if (microServicesChain.isEmpty()) {
            microServicesChain = BlueprintParser.fallbackToOneMicroService();
        }
        loopTemplate.setSvgRepresentation(svgFacade.getSvgImage(microServicesChain));
        loopTemplate.setName(templateName);

        LoopTemplate existingTemplate = getLoopTemplate(templateName);
        if (existingTemplate != null) {
            loopTemplate.setName(existingTemplate.getName());
        }
        return loopTemplate;
    }
}
