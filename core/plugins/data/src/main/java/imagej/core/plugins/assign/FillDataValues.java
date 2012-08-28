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

package imagej.core.plugins.assign;

import imagej.Cancelable;
import imagej.data.display.ImageDisplay;
import imagej.data.display.OverlayService;
import imagej.data.options.OptionsChannels;
import imagej.data.overlay.Overlay;
import imagej.ext.plugin.RunnablePlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;
import imagej.menu.MenuConstants;
import imagej.module.ItemIO;
import imagej.options.OptionsService;
import net.imglib2.type.numeric.RealType;

/**
 * Fills the selected region of an input Dataset with the foreground values.
 * 
 * @author Barry DeZonia
 */
@Plugin(menu = {
	@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
		mnemonic = MenuConstants.EDIT_MNEMONIC),
	@Menu(label = "Fill", weight = 28, accelerator = "control F") },
	headless = true)
public class FillDataValues<T extends RealType<T>>
	implements RunnablePlugin, Cancelable
{
	// -- Parameters --

	@Parameter
	private OverlayService overlayService;

	@Parameter
	private OptionsService optionsService;
	
	@Parameter(type = ItemIO.BOTH)
	private ImageDisplay display;

	// -- instance variables that are not Parameters --

	private String err;

	// -- public interface --

	@Override
	public boolean isCanceled() {
		return err != null;
	}
	
	@Override
	public String getCancelReason() {
		return err;
	}
	
	@Override
	public void run() {
		final OptionsChannels opts =
			optionsService.getOptions(OptionsChannels.class);
		final Overlay overlay = overlayService.getActiveOverlay(display);
		if (overlay == null) {
			err = "This command requires a selection";
		}
		else
			overlayService.fillOverlay(overlay, display, opts.getFgValues());
	}

	public ImageDisplay getDisplay() {
		return display;
	}

	public void setDisplay(final ImageDisplay display) {
		this.display = display;
	}

	// -- private helpers --

	// TODO - make this part of Dataset API maybe. or somewhere else.
	/*
	private void fillSelectedRegion(final Dataset dataset,
		final ChannelCollection channels)
	{
		final RealRect bounds = overlayService.getSelectionBounds(display);
		final long minX = (long) bounds.x;
		final long minY = (long) bounds.y;
		final long maxX = (long) (bounds.x + bounds.width - 1);
		final long maxY = (long) (bounds.y + bounds.height - 1);
		final long[] pos = new long[dataset.numDimensions()];
		final int xIndex = dataset.getAxisIndex(Axes.X);
		final int yIndex = dataset.getAxisIndex(Axes.Y);
		final int chIndex = dataset.getAxisIndex(Axes.CHANNEL);
		final ImgPlus<? extends RealType<?>> imgPlus = dataset.getImgPlus();
		final Cursor<? extends RealType<?>> cursor = imgPlus.localizingCursor();
		while (cursor.hasNext()) {
			final RealType<?> pixRef = cursor.next();
			cursor.localize(pos);
			if ((pos[xIndex] < minX) || (pos[xIndex] > maxX)) continue;
			if ((pos[yIndex] < minY) || (pos[yIndex] > maxY)) continue;
			long position = 0;
			if (chIndex >= 0) position = pos[chIndex];
			double value = channels.getChannelValue(position);
			pixRef.setReal(value);
		}
		dataset.update();
	}
	*/
}
