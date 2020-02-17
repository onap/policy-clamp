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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Set;
import org.apache.batik.svggen.SVGGraphics2D;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

public class Painter {
    private final int canvasSize;
    private final SVGGraphics2D g2d;
    private final DocumentBuilder documentBuilder;

    private static final int DEFAULT_CANVAS_SIZE = 900;
    private static final int SLIM_LINE = 2;
    private static final int THICK_LINE = 4;
    private static final double RECT_RATIO = 3.0 / 2.0;
    private static final int CIRCLE_RADIUS = 17;
    private static final int MINIMUM_BASE_LENGTH = 120;

    /**
     * Constructor to create instance of Painter.
     *
     * @param svgGraphics2D   svg graphics
     * @param documentBuilder document builder
     */
    public Painter(SVGGraphics2D svgGraphics2D, DocumentBuilder documentBuilder) {
        this.g2d = svgGraphics2D;
        this.documentBuilder = documentBuilder;
        this.canvasSize = DEFAULT_CANVAS_SIZE;
    }

    DocumentBuilder doPaint(String collector, Set<MicroServicePolicy> microServices, Set<OperationalPolicy> policies) {
        int numOfRectangles = 2 + microServices.size();
        int numOfArrows = numOfRectangles + 1;
        int baseLength = (canvasSize - 2 * CIRCLE_RADIUS) / (numOfArrows + numOfRectangles);
        if (baseLength < MINIMUM_BASE_LENGTH) {
            baseLength = MINIMUM_BASE_LENGTH;
        }
        int rectHeight = (int) (baseLength / RECT_RATIO);

        adjustGraphics2DProperties();

        Point origin = new Point(1, rectHeight / 2);
        ImageBuilder ib = new ImageBuilder(g2d, documentBuilder, origin, baseLength, rectHeight);

        doTheActualDrawing(collector, microServices, policies, ib);

        return ib.getDocumentBuilder();
    }

    private void doTheActualDrawing(String collector, Set<MicroServicePolicy> microServices,
                                    Set<OperationalPolicy> policies,
                                    ImageBuilder ib) {
        ib.circle("start-circle", SLIM_LINE).arrow().rectangle(collector, RectTypes.COLECTOR, collector);

        for (MicroServicePolicy ms : microServices) {
            ib.arrow().rectangle(ms.getName(),
                    RectTypes.MICROSERVICE, ms.getPolicyModel().getPolicyAcronym());
        }
        for (OperationalPolicy policy : policies) {
            ib.arrow().rectangle(policy.getName(), RectTypes.POLICY, policy.getPolicyModel().getPolicyAcronym())
                    .arrow();
        }
        ib.circle("stop-circle", THICK_LINE);
    }

    private void adjustGraphics2DProperties() {
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setStroke(new BasicStroke(SLIM_LINE));
        g2d.setPaint(Color.BLACK);
    }

}
