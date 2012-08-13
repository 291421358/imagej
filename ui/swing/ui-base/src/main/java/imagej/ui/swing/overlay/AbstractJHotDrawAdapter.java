/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.ui.swing.overlay;

import imagej.Prioritized;
import imagej.Priority;
import imagej.data.display.OverlayService;
import imagej.data.display.OverlayView;
import imagej.data.overlay.Overlay;
import imagej.data.overlay.OverlaySettings;
import imagej.ext.tool.AbstractTool;
import imagej.util.ColorRGB;
import imagej.util.awt.AWTColors;

import java.awt.Color;

import org.jhotdraw.draw.AttributeKeys;
import org.jhotdraw.draw.Figure;
import org.jhotdraw.draw.decoration.ArrowTip;

/**
 * An abstract class that gives default behavior for the {@link JHotDrawAdapter}
 * interface.
 * 
 * @author Lee Kamentsky
 */
public abstract class AbstractJHotDrawAdapter<O extends Overlay> extends
	AbstractTool implements JHotDrawAdapter
{

	// NB: The line styles here are taken from
	// org.jhotdraw.draw.action.ButtonFactory.
	// Copyright (c) 1996-2010 by the original authors of
	// JHotDraw and all its contributors. All rights reserved.

	static final protected double[] solidLineStyle = null;
	static final protected double[] dashLineStyle = { 4, 4 };
	static final protected double[] dotLineStyle = { 1, 2 };
	static final protected double[] dotDashLineStyle = { 6, 2, 1, 2 };

	private double priority = Priority.NORMAL_PRIORITY;

	// -- JHotDrawAdapter methods --

	@Override
	public void updateFigure(final OverlayView overlayView, final Figure figure) {
		final Overlay overlay = overlayView.getData();
		final ColorRGB lineColor = overlay.getLineColor();
		if (overlay.getLineStyle() != Overlay.LineStyle.NONE) {
			figure.set(AttributeKeys.STROKE_COLOR, AWTColors.getColor(lineColor));

			// FIXME - is this next line dangerous for drawing attributes? width could
			// conceivably need to always stay 0.
			figure.set(AttributeKeys.STROKE_WIDTH, overlay.getLineWidth());
			double[] dash_pattern;
			switch (overlay.getLineStyle()) {
				case SOLID:
					dash_pattern = null;
					break;
				case DASH:
					dash_pattern = dashLineStyle;
					break;
				case DOT:
					dash_pattern = dotLineStyle;
					break;
				case DOT_DASH:
					dash_pattern = dotDashLineStyle;
					break;
				default:
					throw new UnsupportedOperationException("Unsupported line style: " +
						overlay.getLineStyle());
			}
			figure.set(AttributeKeys.STROKE_DASHES, dash_pattern);
		}
		else {
			// Render a "NONE" line style as alpha = transparent.
			figure.set(AttributeKeys.STROKE_COLOR, new Color(0, 0, 0, 0));
		}
		final ColorRGB fillColor = overlay.getFillColor();
		final int alpha = overlay.getAlpha();
		figure.set(AttributeKeys.FILL_COLOR, AWTColors.getColor(fillColor, alpha));
		switch (overlay.getLineStartArrowStyle()) {
			case ARROW:
				figure.set(AttributeKeys.START_DECORATION, new ArrowTip());
				break;
			case NONE:
				figure.set(AttributeKeys.START_DECORATION, null);
		}
		switch (overlay.getLineEndArrowStyle()) {
			case ARROW:
				figure.set(AttributeKeys.END_DECORATION, new ArrowTip());
				break;
			case NONE:
				figure.set(AttributeKeys.END_DECORATION, null);
				break;
		}
	}

	@Override
	public void updateOverlay(final Figure figure, final OverlayView overlayView) {
		final Color strokeColor = figure.get(AttributeKeys.STROKE_COLOR);
		final Overlay overlay = overlayView.getData();
		overlay.setLineColor(AWTColors.getColorRGB(strokeColor));
		// The line style is intentionally omitted here because it is ambiguous and
		// because there is no UI for setting it by the JHotDraw UI.

		// FIXME - is this next line dangerous for drawing attributes? width could
		// conceivably be 0.
		overlay.setLineWidth(figure.get(AttributeKeys.STROKE_WIDTH));
		final Color fillColor = figure.get(AttributeKeys.FILL_COLOR);
		overlay.setFillColor(AWTColors.getColorRGB(fillColor));
		overlay.setAlpha(fillColor.getAlpha());
	}

	// -- Prioritized methods --

	@Override
	public double getPriority() {
		return priority;
	}

	@Override
	public void setPriority(final double priority) {
		this.priority = priority;
	}

	// -- Comparable methods --

	@Override
	public int compareTo(final Prioritized p) {
		return Priority.compare(this, p);
	}

	// -- Internal methods --

	protected void initDefaultSettings(final Figure figure) {
		final OverlayService overlayService =
			getContext().getService(OverlayService.class);
		final OverlaySettings settings = overlayService.getDefaultSettings();
		figure.set(AttributeKeys.STROKE_WIDTH, getDefaultLineWidth(settings));
		figure.set(AttributeKeys.FILL_COLOR, getDefaultFillColor(settings));
		figure.set(AttributeKeys.STROKE_COLOR, getDefaultStrokeColor(settings));
		// Avoid IllegalArgumentException: miter limit < 1 on the EDT
		figure.set(AttributeKeys.IS_STROKE_MITER_LIMIT_FACTOR, false);
	}

	// -- Helper methods --

	private double getDefaultLineWidth(final OverlaySettings settings) {
		return settings.getLineWidth();
	}

	private Color getDefaultStrokeColor(final OverlaySettings settings) {
		final ColorRGB color = settings.getLineColor();
		final int r = color.getRed();
		final int g = color.getGreen();
		final int b = color.getBlue();
		return new Color(r, g, b, 255);
	}

	private Color getDefaultFillColor(final OverlaySettings settings) {
		final ColorRGB color = settings.getFillColor();
		final int r = color.getRed();
		final int g = color.getGreen();
		final int b = color.getBlue();
		final int a = settings.getAlpha();
		return new Color(r, g, b, a);
	}

}
