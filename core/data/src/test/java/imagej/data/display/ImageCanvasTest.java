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

package imagej.data.display;

import static org.junit.Assert.assertEquals;
import imagej.ImageJ;
import imagej.event.EventService;
import imagej.util.IntCoords;
import imagej.util.IntRect;
import imagej.util.RealCoords;
import imagej.util.RealRect;

import org.junit.Test;

/**
 * Unit tests for {@link DefaultImageCanvas}.
 * 
 * @author Curtis Rueden
 */
public class ImageCanvasTest {

	private final double xDataMin = 12.2, yDataMin = 56.3;
	private final double xDataMax = 290.4, yDataMax = 782.001;
	private final int panelWidth = 120, panelHeight = 150;
	private final double epsilon = 0.001;

	private final double dataWidth = xDataMax - xDataMin;
	private final double dataHeight = yDataMax - yDataMin;

	private final RealCoords dataCenter = new RealCoords(
		(xDataMax + xDataMin) / 2, (yDataMax + yDataMin) / 2);
	private final RealCoords dataTopLeft = new RealCoords(xDataMin, yDataMin);
	private final RealCoords dataBottomRight = new RealCoords(xDataMax, yDataMax);

	private final RealRect dataBounds = new RealRect(xDataMin, yDataMin,
		dataWidth, dataHeight);

	private final IntCoords panelCenter = new IntCoords(panelWidth / 2,
		panelHeight / 2);
	private final IntCoords panelTopLeft = new IntCoords(0, 0);
	private final IntCoords panelBottomRight = new IntCoords(panelWidth,
		panelHeight);

	private final IntRect panelBounds =
		new IntRect(0, 0, panelWidth, panelHeight);

	@Test
	public void testPan() {
		final ImageCanvas canvas = createImageCanvas();

		assertApproximatelyEqual(dataCenter, canvas.getPanCenter());

		canvas.setPanCenter(dataBottomRight);
		assertEquals(dataBottomRight, canvas.getPanCenter());

		canvas.panReset();
		assertApproximatelyEqual(dataCenter, canvas.getPanCenter());

		canvas.setPanCenter(panelCenter);
		assertApproximatelyEqual(dataCenter, canvas.getPanCenter());

		final RealCoords eTL = canvas.panelToImageCoords(panelTopLeft);
		canvas.setPanCenter(panelTopLeft);
		assertApproximatelyEqual(eTL, canvas.getPanCenter());

		final RealCoords eBR = canvas.panelToImageCoords(panelBottomRight);
		canvas.setPanCenter(panelBottomRight);
		assertApproximatelyEqual(eBR, canvas.getPanCenter());
	}

	@Test
	public void testZoom() {
		final ImageCanvas canvas = createImageCanvas();

		assertEquals(1, canvas.getInitialScale(), 0);
		assertEquals(1, canvas.getZoomFactor(), 0);

		// setZoom

		canvas.setZoom(1.5);
		assertEquals(1.5, canvas.getZoomFactor(), 0);

		// zoomIn

		canvas.panReset();
		canvas.zoomIn();
		assertEquals(2, canvas.getZoomFactor(), 0);

		canvas.zoomIn();
		assertEquals(3, canvas.getZoomFactor(), 0);

		// zoomOut

		canvas.zoomOut();
		assertEquals(2, canvas.getZoomFactor(), 0);

		canvas.setZoom(0.5);
		assertEquals(0.5, canvas.getZoomFactor(), 0);

		canvas.zoomOut();
		assertEquals(1 / 3d, canvas.getZoomFactor(), 0);

		// zoomToFit

		final double eZoomFit =
			Math.min(panelWidth / dataWidth, panelHeight / dataHeight);

		canvas.zoomToFit(dataBounds);
		assertEquals(eZoomFit, canvas.getZoomFactor(), 0);

		canvas.zoomToFit(panelBounds);
		assertEquals(eZoomFit, canvas.getZoomFactor(), 0);
	}
	
	@Test
	public void testPanPlusZoom() {
		final ImageCanvas canvas = createImageCanvas();

		final RealCoords dataCoords =
			new RealCoords(dataWidth / 4, 3 * dataHeight / 4);

		final double zoom = 2.3;
		canvas.setZoom(zoom, dataCoords);
		assertEquals(zoom, canvas.getZoomFactor(), 0);
		assertEquals(dataCoords, canvas.getPanCenter());
	}

	@Test
	public void testImageToPanelCoords() {
		final ImageCanvas canvas = createImageCanvas();

		final IntCoords computedPanelCenter = canvas.imageToPanelCoords(dataCenter);
		assertEquals(panelCenter, computedPanelCenter);

		final IntCoords aTL = canvas.imageToPanelCoords(dataTopLeft);
		final IntCoords eTL = dataTopLeftToPanel(1);
		assertEquals(eTL, aTL);

		final IntCoords aBR = canvas.imageToPanelCoords(dataBottomRight);
		final IntCoords eBR = dataBottomRightToPanel(1);
		assertEquals(eBR, aBR);

		final double zoom = 5.5;
		canvas.setZoom(zoom);

		final IntCoords aTLZoomed = canvas.imageToPanelCoords(dataTopLeft);
		final IntCoords eTLZoomed = dataTopLeftToPanel(zoom);
		assertEquals(eTLZoomed, aTLZoomed);

		final IntCoords aBRZoomed = canvas.imageToPanelCoords(dataBottomRight);
		final IntCoords eBRZoomed = dataBottomRightToPanel(zoom);
		assertEquals(eBRZoomed, aBRZoomed);
	}

	@Test
	public void testPanelToImageCoords() {
		final ImageCanvas canvas = createImageCanvas();

		final RealCoords computedDataCenter =
			canvas.panelToImageCoords(panelCenter);
		assertApproximatelyEqual(dataCenter, computedDataCenter);

		final RealCoords aTL = canvas.panelToImageCoords(panelTopLeft);
		final RealCoords eTL = panelTopLeftToData(1);
		assertApproximatelyEqual(eTL, aTL);

		final RealCoords aBR = canvas.panelToImageCoords(panelBottomRight);
		final RealCoords eBR = panelBottomRightToData(1);
		assertApproximatelyEqual(eBR, aBR);

		final double zoom = 0.37;
		canvas.setZoom(zoom);

		final RealCoords aTLZoomed = canvas.panelToImageCoords(panelTopLeft);
		final RealCoords eTLZoomed = panelTopLeftToData(zoom);
		assertApproximatelyEqual(eTLZoomed, aTLZoomed);

		final RealCoords aBRZoomed = canvas.panelToImageCoords(panelBottomRight);
		final RealCoords eBRZoomed = panelBottomRightToData(zoom);
		assertApproximatelyEqual(eBRZoomed, aBRZoomed);
	}

	// -- Helper methods --

	private ImageCanvas createImageCanvas() {
		final ImageJ context = ImageJ.createContext(EventService.class);
		final ImageDisplay display = new DefaultImageDisplay() {

			@Override
			public RealRect getPlaneExtents() {
				return new RealRect(xDataMin, yDataMin, dataWidth, dataHeight);
			}
		};
		display.setContext(context);
		final ImageCanvas canvas = new DefaultImageCanvas(display);
		canvas.setViewportSize(panelWidth, panelHeight);
		return canvas;
	}

	private void assertApproximatelyEqual(final RealCoords expected,
		final RealCoords actual)
	{
		assertEquals("X coordinate:", expected.x, actual.x, epsilon);
		assertEquals("Y coordinate:", expected.y, actual.y, epsilon);
	}

	private IntCoords dataTopLeftToPanel(double zoom) {
		final int x = (int) Math.round(panelCenter.x - zoom * dataWidth / 2);
		final int y = (int) Math.round(panelCenter.y - zoom * dataHeight / 2);
		return new IntCoords(x, y);
	}

	private IntCoords dataBottomRightToPanel(double zoom) {
		final int x = (int) Math.round(panelCenter.x + zoom * dataWidth / 2);
		final int y = (int) Math.round(panelCenter.y + zoom * dataHeight / 2);
		return new IntCoords(x, y);
	}

	private RealCoords panelTopLeftToData(double zoom) {
		final double x = dataCenter.x - panelWidth / 2.0 / zoom;
		final double y = dataCenter.y - panelHeight / 2.0 / zoom;
		return new RealCoords(x, y);
	}

	private RealCoords panelBottomRightToData(double zoom) {
		final double x = dataCenter.x + panelWidth / 2.0 / zoom;
		final double y = dataCenter.y + panelHeight / 2.0 / zoom;
		return new RealCoords(x, y);
	}

}
