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

package imagej.legacy.translate;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import imagej.AbstractContextual;
import imagej.ImageJ;
import imagej.data.display.ImageDisplay;
import imagej.data.display.OverlayService;
import imagej.data.overlay.AngleOverlay;
import imagej.data.overlay.BinaryMaskOverlay;
import imagej.data.overlay.CompositeOverlay;
import imagej.data.overlay.EllipseOverlay;
import imagej.data.overlay.LineOverlay;
import imagej.data.overlay.Overlay;
import imagej.data.overlay.PointOverlay;
import imagej.data.overlay.PolygonOverlay;
import imagej.data.overlay.RectangleOverlay;
import imagej.util.ColorRGB;
import imagej.log.LogService;
import imagej.util.awt.AWTColors;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.BitArray;
import net.imglib2.img.transform.ImgTranslationAdapter;
import net.imglib2.roi.BinaryMaskRegionOfInterest;
import net.imglib2.roi.EllipseRegionOfInterest;
import net.imglib2.roi.PolygonRegionOfInterest;
import net.imglib2.roi.RectangleRegionOfInterest;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.logic.BitType;

/**
 * OverlayTranslator moves regions of interest back and forth between
 * {@link Overlay}s and {@link ImagePlus} {@link Roi}s.
 * 
 * @author Lee Kamentsky
 * @author Curtis Rueden
 * @author Barry DeZonia
 */
public class OverlayHarmonizer extends AbstractContextual implements
	DisplayHarmonizer
{
	private final LogService log;

	public OverlayHarmonizer(final ImageJ context) {
		setContext(context);
		log = context.getService(LogService.class);
	}

	/**
	 * Updates the given {@link ImageDisplay} to contain {@link Overlay}s
	 * corresponding to the given {@link ImagePlus}'s Roi.
	 */
	@Override
	public void updateDisplay(final ImageDisplay display, final ImagePlus imp) {
		final OverlayService overlayService =
			getContext().getService(OverlayService.class);
		final Roi oldRoi = createRoi(overlayService.getOverlays(display));
		if (oldRoi instanceof ShapeRoi) {
			final float[] oldPath = ((ShapeRoi) oldRoi).getShapeAsArray();
			final Roi newRoi = imp.getRoi();
			if (newRoi instanceof ShapeRoi) {
				final float[] newPath = ((ShapeRoi) newRoi).getShapeAsArray();
				if (oldPath.length == newPath.length) {
					boolean same = true;
					for (int i = 0; i < oldPath.length; i++) {
						if (oldPath[i] != newPath[i]) {
							same = false;
							break;
						}
					}
					if (same)
						if (oldRoi.getStrokeWidth() == newRoi.getStrokeWidth())
							if (sameColor(oldRoi.getFillColor(), newRoi.getFillColor()))
								if (sameColor(oldRoi.getStrokeColor(), newRoi.getStrokeColor()))
									return;
				}
			}
		}
		final List<Overlay> overlaysToRemove = overlayService.getOverlays(display);
		for (final Overlay overlay : overlaysToRemove) {
			overlayService.removeOverlay(display, overlay);
		}
		/*
		if (fullySelected(display, imp)) {
			for (DataView view : display)
				view.setSelected(true);
		}
		else {
		*/
		final List<Overlay> overlays = getOverlays(imp);
		overlayService.addOverlays(display, overlays);
		// }
	}

	/**
	 * Updates the given {@link ImagePlus}'s Roi to match the {@link Overlay}s
	 * being visualized in the given {@link ImageDisplay}.
	 */
	@Override
	public void
		updateLegacyImage(final ImageDisplay display, final ImagePlus imp)
	{
		final OverlayService overlayService =
			getContext().getService(OverlayService.class);
		final List<Overlay> overlays = overlayService.getOverlays(display);
		setOverlays(overlays, imp);
	}

	/** Extracts a list of {@link Overlay}s from the given {@link ImagePlus}. */
	public List<Overlay> getOverlays(final ImagePlus imp) {
		final Roi roi = imp.getRoi();
		final ArrayList<Overlay> overlays = new ArrayList<Overlay>();
		createOverlays(roi, overlays, 0, 0);
		return overlays;
	}

	/** Assigns a list of {@link Overlay}s to the given {@link ImagePlus}. */
	public void setOverlays(final List<Overlay> overlays, final ImagePlus imp) {
		final Roi roi = createRoi(overlays);
		imp.setRoi(roi);
	}

	private Roi createRoi(final List<Overlay> overlays) {
		if (overlays.size() == 0) return null;
		if (overlays.size() == 1) return createRoi(overlays.get(0));
		ShapeRoi roi = new ShapeRoi(createRoi(overlays.get(0)));
		for (int i = 1; i < overlays.size(); i++) {
			final Roi overlayRoi = createRoi(overlays.get(i));
			if (overlayRoi != null) roi = roi.or(new ShapeRoi(overlayRoi));
		}
		return roi;
	}

	// -- Helper methods - legacy Roi creation --

	private Roi createRoi(final Overlay overlay) {
		if (overlay instanceof RectangleOverlay) {
			return createRectangleRoi((RectangleOverlay) overlay);
		}
		if (overlay instanceof EllipseOverlay) {
			return createEllipseRoi((EllipseOverlay) overlay);
		}
		if (overlay instanceof PolygonOverlay) {
			return createPolygonRoi((PolygonOverlay) overlay);
		}
		if (overlay instanceof BinaryMaskOverlay) {
			return createBinaryMaskRoi((BinaryMaskOverlay<?, ?>) overlay);
		}
		if (overlay instanceof LineOverlay) {
			return createLineRoi((LineOverlay) overlay);
		}
		if (overlay instanceof PointOverlay) {
			return createPointRoi((PointOverlay) overlay);
		}
		if (overlay instanceof AngleOverlay) {
			return createAngleRoi((AngleOverlay) overlay);
		}
		// TODO: arrows, freehand, text
//		throw new UnsupportedOperationException("Translation of " +
//			overlay.getClass().getName() + " is unimplemented");
		return null;
	}

	// NB - there is some overloading here with createLineRoi.
	
	// From a LineOverlay
	private Roi createLineRoi(final LineOverlay overlay) {
		double[] p1 = new double[overlay.numDimensions()];
		double[] p2 = new double[overlay.numDimensions()];
		overlay.getLineStart(p1);
		overlay.getLineEnd(p2);
		return createLineRoi(overlay, p1, p2);
	}

	// From a PolygonOverlay that has two points
	private Roi createLineRoi(final PolygonOverlay overlay) {
		final PolygonRegionOfInterest region = overlay.getRegionOfInterest();
		double[] p1 = new double[overlay.numDimensions()];
		double[] p2 = new double[overlay.numDimensions()];
		final RealLocalizable vp1 = region.getVertex(0);
		final RealLocalizable vp2 = region.getVertex(1);
		vp1.localize(p1);
		vp2.localize(p2);
		return createLineRoi(overlay, p1, p2);
	}

	// helper to support other createLineRoi() methods
	private Roi createLineRoi(Overlay overlay, double[] p1, double[] p2) {
		final double x1 = p1[0];
		final double y1 = p1[1];
		final double x2 = p2[0];
		final double y2 = p2[1];
		final Line line = new Line(x1, y1, x2, y2);
		assignPropertiesToRoi(line, overlay);
		return line;
	}
	
	private Roi createRectangleRoi(final RectangleOverlay overlay) {
		final RectangleRegionOfInterest region = overlay.getRegionOfInterest();
		final int dims = region.numDimensions();
		final double[] origin = new double[dims];
		final double[] extent = new double[dims];
		region.getOrigin(origin);
		region.getExtent(extent);
		final double x = origin[0], y = origin[1];
		final double w = extent[0], h = extent[1];
		final Roi roi = new Roi(x, y, w, h);
		assignPropertiesToRoi(roi, overlay);
		return roi;
	}

	private Roi createEllipseRoi(final EllipseOverlay overlay) {
		final EllipseRegionOfInterest region = overlay.getRegionOfInterest();
		final int dims = region.numDimensions();
		final double[] origin = new double[dims];
		final double[] radii = new double[dims];
		region.getOrigin(origin);
		region.getRadii(radii);
		final double x = origin[0] - radii[0];
		final double y = origin[1] - radii[1];
		final double w = radii[0] * 2, h = radii[1] * 2;
		final Roi roi = new OvalRoi(x, y, w, h);
		assignPropertiesToRoi(roi, overlay);
		return roi;
	}

	private Roi createPolygonRoi(final PolygonOverlay overlay) {
		final PolygonRegionOfInterest region = overlay.getRegionOfInterest();
		final int vertexCount = region.getVertexCount();
		if (vertexCount == 1) return createPointRoi(overlay);
		if (vertexCount == 2) return createLineRoi(overlay);
		final float[] x = new float[vertexCount];
		final float[] y = new float[vertexCount];
		for (int v = 0; v < vertexCount; v++) {
			final RealLocalizable vertex = region.getVertex(v);
			x[v] = vertex.getFloatPosition(0);
			y[v] = vertex.getFloatPosition(1);
		}
		final Roi roi = new PolygonRoi(x, y, vertexCount, Roi.POLYGON);
		assignPropertiesToRoi(roi, overlay);
		return roi;
	}

	// NB - there is some overloading here with createPointRoi.
	
	// From a PolygonOverlay that has one point
	private Roi createPointRoi(final PolygonOverlay overlay) {
		final PolygonRegionOfInterest region = overlay.getRegionOfInterest();
		final RealLocalizable point = region.getVertex(0);
		double ptX = point.getDoublePosition(0);
		double ptY = point.getDoublePosition(1);
		return createPointRoi(overlay, ptX, ptY);
	}

	// From a PointOverlay
	private Roi createPointRoi(final PointOverlay overlay) {
		return createPointRoi(overlay, overlay.getPoint(0), overlay.getPoint(1));
	}

	// helper to support other createPointRoi() methods
	private Roi createPointRoi(final Overlay overlay, double ptX, double ptY) {
		final double x = ptX;
		final double y = ptY;
		final PointRoi point = new PointRoi(x,y);
		assignPropertiesToRoi(point, overlay);
		return point;
	}
	
	// TODO - subpixel resolution
	private Roi createAngleRoi(final AngleOverlay overlay) {
		double[] pt = new double[overlay.numDimensions()];
		overlay.getPoint1(pt);
		float xb = (float) pt[0];
		float yb = (float) pt[1];
		overlay.getCenter(pt);
		float xc = (float) pt[0];
		float yc = (float) pt[1];
		overlay.getPoint2(pt);
		float xe = (float) pt[0];
		float ye = (float) pt[1];
		float[] xpoints = new float[]{xb,xc,xe};
		float[] ypoints = new float[]{yb,yc,ye};
		return new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
	}
	
	private ShapeRoi createBinaryMaskRoi(final BinaryMaskOverlay<?, ?> overlay) {
		final RegionOfInterest roi = overlay.getRegionOfInterest();
		final double[] min = new double[roi.numDimensions()];
		roi.realMin(min);
		final double[] max = new double[roi.numDimensions()];
		roi.realMax(max);
		// TODO - is there some way to have subpixel resolution with mask rois?
		final int x = (int) Math.ceil(min[0]);
		final int y = (int) Math.ceil(min[1]);
		final int width = (int) Math.ceil(max[0]) - x + 1;
		final int height = (int) Math.ceil(max[1]) - y + 1;

		// TODO Readjust to account for 3+D binary masks.
		// Assume for now that the Roi is 2-d or that the desired plane is 0 for all
		// accessory dimensions.
		// Later we will have axes for overlays and we can pick the X and Y axes.
		// Later still, we will work out some mechanism for how all the planes are
		// sent to the legacy layer.
		//
		// We only want to return one Roi, so we have a single stack image.
		final ByteProcessor ip = new ByteProcessor(width, height);

		// set things so that true is between 1 and 3 and false is below 1
		ip.setThreshold(1, 3, ImageProcessor.NO_LUT_UPDATE);
		final RealRandomAccess<BitType> ra = roi.realRandomAccess();

		// this picks a plane at the minimum Z, T, etc within the Roi
		ra.setPosition(min);
		for (int i = 0; i < width; i++) {
			ra.setPosition(i + x, 0);
			for (int j = 0; j < height; j++) {
				ra.setPosition(j + y, 1);
				ip.set(i, j, ra.get().get() ? 2 : 0);
			}
		}
		final ThresholdToSelection plugin = new ThresholdToSelection();

		final Roi imagejroi = plugin.convert(ip);
		imagejroi.setLocation(x, y);
		return new ShapeRoi(imagejroi);
	}

	private void assignPropertiesToRoi(final Roi roi, final Overlay overlay) {
		roi.setName(overlay.getName());
		roi.setStrokeWidth((float) overlay.getLineWidth());
		roi.setStrokeColor(AWTColors.getColor(overlay.getLineColor()));
		final Color fillColor = AWTColors.getColor(overlay.getFillColor());
		final Color colorWithAlpha =
			new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(),
				overlay.getAlpha());
		roi.setFillColor(colorWithAlpha);
	}

	// -- Helper methods - IJ2 overlay creation --

	/*
	private boolean fullySelected(ImageDisplay display, ImagePlus imp) {
		Roi roi = imp.getRoi();
		if (roi != null) {
			if (roi.getType() == Roi.RECTANGLE) {
				ImageDisplayService dispServ = ImageJ.get(ImageDisplayService.class);
				Dataset ds = dispServ.getActiveDataset(display);
				long[] dims = ds.getDims();
				// TODO - FIXME - assumes X and Y are at 0 & 1
				long imageWidth = dims[0];
				long imageHeight = dims[1];
				Rectangle rect = roi.getBounds();
				if ((rect.x == 0) && (rect.y == 0) &&
						(rect.width == imageWidth) && (rect.height == imageHeight))
					return true;
			}
		}
		return false;
	}
	*/

	private void createOverlays(final Roi roi,
		final ArrayList<Overlay> overlays, final double xOff, final double yOff)
	{
		if (roi == null) return;

		log.warn("====> Roi class = " + roi.getClass().getName());
		switch (roi.getType()) {
			case Roi.RECTANGLE:
				log.warn("====> RECTANGLE: " + roi);
				overlays.add(createRectangleOverlay(roi, xOff, yOff));
				break;
			case Roi.OVAL:
				log.warn("====> OVAL: " + roi);
				overlays.add(createEllipseOverlay(roi, xOff, yOff));
				break;
			case Roi.POLYGON:
				log.warn("====> POLYGON: " + roi);
				overlays.add(createPolygonOverlay(roi, xOff, yOff));
				break;
			case Roi.FREEROI:
				log.warn("====> FREEROI: " + roi);
				overlays.add(createDefaultOverlay(roi, xOff, yOff));
				break;
			case Roi.TRACED_ROI:
				log.warn("====> TRACED_ROI: " + roi);
				overlays.add(createDefaultOverlay(roi, xOff, yOff));
				break;
			case Roi.LINE:
				log.warn("====> LINE: " + roi);
				overlays.add(createLineOverlay(roi, xOff, yOff));
				break;
			case Roi.POLYLINE:
				log.warn("====> POLYLINE: " + roi);
				// throw new UnsupportedOperationException("POLYLINE unimplemented");
				break;
			case Roi.FREELINE:
				log.warn("====> FREELINE: " + roi);
				// throw new UnsupportedOperationException("FREELINE unimplemented");
				break;
			case Roi.ANGLE:
				log.warn("====> ANGLE: " + roi);
				overlays.add(createAngleOverlay(roi, xOff, yOff));
				break;
			case Roi.POINT:
				log.warn("====> POINT: " + roi);
				overlays.addAll(createPointOverlays(roi, xOff, yOff));
				break;
			case Roi.COMPOSITE:
				log.warn("====> COMPOSITE: " + roi);
				final ShapeRoi shapeRoi = (ShapeRoi) roi;
				final Roi[] rois = shapeRoi.getRois();
				final double xO = xOff + xOff + shapeRoi.getBounds().x;
				final double yO = yOff + shapeRoi.getBounds().y;
				final ArrayList<Overlay> subOverlays = new ArrayList<Overlay>();
				for (final Roi r : rois)
					createOverlays(r, subOverlays, xO, yO);
				for (final Overlay overlay : subOverlays) {
					assignPropertiesToOverlay(overlay, shapeRoi);
				}
				if (subOverlays.size() == 1) {
					overlays.add(subOverlays.get(0));
					return;
				}
				final CompositeOverlay coverlay = new CompositeOverlay(getContext());
				for (Overlay subOverlay : subOverlays) {
					coverlay.xor(subOverlay);
				}
				/*
				 * An arbitrary guess - set the fill color to red with a 1/3 alpha
				 */
				coverlay.setFillColor(new ColorRGB(255, 0, 0));
				coverlay.setAlpha(80);
				overlays.add(coverlay);
				break;
			default:
				log.warn("====> OTHER (" + roi.getType() + ", " + "): " + roi);
				throw new UnsupportedOperationException("OTHER unimplemented");
		}
	}

	@SuppressWarnings("unused")
	private Overlay createAngleOverlay(final Roi roi, final double xOff,
		final double yOff)
	{
		assert roi instanceof PolygonRoi;
		PolygonRoi pRoi = (PolygonRoi) roi;
		FloatPolygon poly = pRoi.getFloatPolygon();
		double[] pt;
		AngleOverlay angleOverlay = new AngleOverlay(getContext());
		pt = new double[]{poly.xpoints[0], poly.ypoints[0]};
		angleOverlay.setPoint1(pt);
		pt = new double[]{poly.xpoints[1], poly.ypoints[1]};
		angleOverlay.setCenter(pt);
		pt = new double[]{poly.xpoints[2], poly.ypoints[2]};
		angleOverlay.setPoint2(pt);
		return angleOverlay;
	}
	
	private Overlay createLineOverlay(final Roi roi, final double xOff,
		final double yOff)
	{
		assert roi instanceof Line;
		final Line line = (Line) roi;
		final LineOverlay lineOverlay =
			new LineOverlay(getContext(), new double[] { line.x1d + xOff,
				line.y1d + yOff }, new double[] { line.x2d + xOff, line.y2d + yOff });
		assignPropertiesToOverlay(lineOverlay, roi);
		return lineOverlay;
	}

	private RectangleOverlay createRectangleOverlay(final Roi roi,
		final double xOff, final double yOff)
	{
		final RectangleOverlay overlay = new RectangleOverlay(getContext());
		final RectangleRegionOfInterest region = overlay.getRegionOfInterest();
		final Rectangle bounds = roi.getBounds();
		region.setOrigin(bounds.x + xOff, 0);
		region.setOrigin(bounds.y + yOff, 1);
		region.setExtent(bounds.width, 0);
		region.setExtent(bounds.height, 1);
		assignPropertiesToOverlay(overlay, roi);
		return overlay;
	}

	private EllipseOverlay createEllipseOverlay(final Roi roi, final double xOff,
		final double yOff)
	{
		final EllipseOverlay overlay = new EllipseOverlay(getContext());
		final EllipseRegionOfInterest region = overlay.getRegionOfInterest();
		final Rectangle bounds = roi.getBounds();
		final double radiusX = bounds.width / 2.0;
		final double radiusY = bounds.height / 2.0;
		region.setOrigin(bounds.x + radiusX + xOff, 0);
		region.setOrigin(bounds.y + radiusY + yOff, 1);
		region.setRadius(radiusX, 0);
		region.setRadius(radiusY, 1);
		assignPropertiesToOverlay(overlay, roi);
		return overlay;
	}

	// TODO - subpixel resolution
	
	@SuppressWarnings("unused")
	private PolygonOverlay createPolygonOverlay(final Roi roi, final double xOff,
		final double yOff)
	{
		assert roi instanceof PolygonRoi;
		final PolygonRoi polygonRoi = (PolygonRoi) roi;
		final PolygonOverlay overlay = new PolygonOverlay(getContext());
		final PolygonRegionOfInterest region = overlay.getRegionOfInterest();
		final int[] xCoords = polygonRoi.getXCoordinates();
		final int[] yCoords = polygonRoi.getYCoordinates();
		final int x0 = polygonRoi.getBounds().x;
		final int y0 = polygonRoi.getBounds().y;
		for (int i = 0; i < xCoords.length; i++) {
			final double x = xCoords[i] + x0, y = yCoords[i] + y0;
			region.addVertex(i, new RealPoint(x, y));
		}
		assignPropertiesToOverlay(overlay, roi);
		return overlay;
	}

	@SuppressWarnings("unused")
	private List<PointOverlay> createPointOverlays(
			final Roi roi, final double xOff, final double yOff)
	{
		assert roi instanceof PointRoi;
		final PointRoi ptRoi = (PointRoi) roi;
		final List<PointOverlay> overlays = new ArrayList<PointOverlay>();
		for (int i = 0; i < ptRoi.getNCoordinates(); i++) {
			final double x = ptRoi.getXCoordinates()[i] + roi.getBounds().x;
			final double y = ptRoi.getYCoordinates()[i] + roi.getBounds().y;
			final double[] pt = new double[]{x,y};
			final PointOverlay pointOverlay = new PointOverlay(getContext(), pt);
			assignPropertiesToOverlay(pointOverlay, roi);
			overlays.add(pointOverlay);
		}
		return overlays;
	}

	@SuppressWarnings("unused")
	private Overlay createDefaultOverlay(final Roi roi, final double xO,
		final double yO)
	{
		final Rectangle bounds = roi.getBounds();
		final ArrayImg<BitType, BitArray> arrayImg =
			new ArrayImgFactory<BitType>().createBitInstance(new long[] {
				bounds.width, bounds.height }, 1);
		final BitType t = new BitType(arrayImg);
		arrayImg.setLinkedType(t);
		final int xOff = bounds.x;
		final int yOff = bounds.y;
		final Img<BitType> img =
			new ImgTranslationAdapter<BitType, Img<BitType>>(arrayImg, new long[] {
				xOff, yOff });
		final RandomAccess<BitType> ra = img.randomAccess();
		final ImageProcessor ip = roi.getMask();
		for (int i = xOff; i < xOff + bounds.width; i++) {
			ra.setPosition(i, 0);
			for (int j = yOff; j < yOff + bounds.height; j++) {
				ra.setPosition(j, 1);
				ra.get().set(ip.get(i - xOff, j - yOff) > 0);
			}
		}
		final BinaryMaskRegionOfInterest<BitType, Img<BitType>> broi =
			new BinaryMaskRegionOfInterest<BitType, Img<BitType>>(img);
		return new BinaryMaskOverlay<BitType, Img<BitType>>(getContext(), broi);
	}

	private void assignPropertiesToOverlay(final Overlay overlay, final Roi roi)
	{
		overlay.setName(roi.getName());
		overlay.setLineWidth(roi.getStrokeWidth());
		final Color strokeColor = roi.getStrokeColor();
		final Color fillColor = roi.getFillColor();
		if (strokeColor != null) {
			overlay.setLineColor(AWTColors.getColorRGB(strokeColor));
		}
		if (fillColor != null) {
			overlay.setFillColor(AWTColors.getColorRGBA(fillColor));
			overlay.setAlpha(fillColor.getAlpha());
		}
	}

	private boolean sameColor(Color c1, Color c2) {
		if (c1 == null) {
			return c2 == null;
		}
		return c1.equals(c2);
	}
}
