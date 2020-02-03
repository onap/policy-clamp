/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights
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
 * Modifications copyright (c) 2019 AT&T
 * ===================================================================
 *
 */

package org.onap.clamp.clds.util.drawing;

import java.util.List;

import org.apache.batik.svggen.SVGGraphics2D;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintMicroService;
import org.onap.clamp.clds.util.XmlTools;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class SvgFacade {
    /**
     * Generate the SVG images from the microservice Chain.
     * 
     * @param microServicesChain THe chain of microservices
     * @return A String containing the SVG
     */
    public String getSvgImage(List<BlueprintMicroService> microServicesChain) {
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(XmlTools.createEmptySvgDocument());
        Document document = XmlTools.createEmptySvgDocument();
        DocumentBuilder dp = new DocumentBuilder(document, svgGraphics2D.getDOMFactory());
        Painter painter = new Painter(svgGraphics2D, dp);
        ClampGraphBuilder cgp = new ClampGraphBuilder(painter).collector("VES");
        cgp.addAllMicroServices(microServicesChain);
        ClampGraph cg = cgp.policy("OperationalPolicy").build();
        return cg.getAsSvg();
    }

}
