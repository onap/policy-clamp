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

import java.util.HashSet;
import java.util.Set;
import org.apache.batik.svggen.SVGGraphics2D;
import org.onap.clamp.clds.util.XmlTools;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.LoopTemplate;
import org.onap.clamp.loop.template.LoopTemplateLoopElementModel;
import org.w3c.dom.Document;

public class SvgLoopGenerator {
    /**
     * Generate the SVG images from the loop.
     *
     * @param loop The loop object, so it won't use the loop template
     * @return A String containing the SVG
     */
    public static String getSvgImage(Loop loop) {
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(XmlTools.createEmptySvgDocument());
        Document document = XmlTools.createEmptySvgDocument();
        DocumentBuilder dp = new DocumentBuilder(document, svgGraphics2D.getDOMFactory());
        Painter painter = new Painter(svgGraphics2D, dp);
        ClampGraphBuilder cgp = new ClampGraphBuilder(painter).collector("VES");
        cgp.addAllMicroServices(loop.getMicroServicePolicies());
        ClampGraph cg = cgp.addAllPolicies(loop.getOperationalPolicies()).build();
        return cg.getAsSvg();
    }

    /**
     * Generate the SVG images from the loop template.
     *
     * @param loopTemplate The loop template
     * @return A String containing the SVG
     */
    public static String getSvgImage(LoopTemplate loopTemplate) {
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(XmlTools.createEmptySvgDocument());
        Document document = XmlTools.createEmptySvgDocument();
        DocumentBuilder dp = new DocumentBuilder(document, svgGraphics2D.getDOMFactory());
        Painter painter = new Painter(svgGraphics2D, dp);
        ClampGraphBuilder cgp = new ClampGraphBuilder(painter).collector("VES");
        Set<LoopElementModel> elementModelsSet = new HashSet<>();
        for (LoopTemplateLoopElementModel elementModelLink:loopTemplate.getLoopElementModelsUsed()) {
            elementModelsSet.add(elementModelLink.getLoopElementModel());
        }
        ClampGraph cg = cgp.addAllLoopElementModels(elementModelsSet).build();
        return cg.getAsSvg();
    }

}
