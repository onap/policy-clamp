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
 * ===================================================================
 *
 */

package org.onap.clamp.clds.util.drawing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

public class AwtUtils {
    private static final int ARROW_W = 4;
    private static final int ARROW_H = 2;
    private static final  int FONT_SIZE = 12;
    private static final  int FONT_STYLE = Font.PLAIN;
    private static final String FONT_FACE = "SansSerif";
    private static final Color TRANSPARENT = new Color(0.0f, 0.0f,0.0f,0.0f);

    static void rectWithText(Graphics2D g2d, String text, Point p, int w, int h) {
        Rectangle rect = new Rectangle(p.x, p.y, w, h);
        g2d.draw(rect);
        Color oldColor = g2d.getColor();
        g2d.setColor(TRANSPARENT);
        g2d.fill(rect);
        g2d.setColor(oldColor);
        addText(g2d, text, p.x+w/2, p.y+h/2);
    }

    static void drawArrow(Graphics2D g2d, Point from, Point to, int lineThickness) {
        int x2 = to.x - lineThickness;
        g2d.drawLine(from.x, from.y, x2-lineThickness, to.y);
        g2d.drawPolygon(new int[] {x2-ARROW_W, x2-ARROW_W, x2},new int[] {to.y- ARROW_H, to.y+ ARROW_H, to.y},3);
        g2d.fillPolygon(new int[] {x2-ARROW_W, x2-ARROW_W, x2},new int[] {to.y- ARROW_H, to.y+ ARROW_H, to.y},3);
    }

    private static void addText(Graphics2D g2d, String text, int x, int y) {
        Font f = new Font(FONT_FACE, FONT_STYLE, FONT_SIZE);
        g2d.setFont(f);

        FontMetrics fm1 = g2d.getFontMetrics();
        int w1 = fm1.stringWidth(text);
        int x1 = x - (w1 / 2);

        g2d.setFont(f);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x1, y);
    }

}
