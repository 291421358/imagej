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

package imagej.core.plugins.overlay;

import java.util.List;

import imagej.data.display.ImageDisplay;
import imagej.data.display.OverlayInfoList;
import imagej.data.display.OverlayService;
import imagej.data.overlay.Overlay;
import imagej.ext.menu.MenuConstants;
import imagej.ext.plugin.ImageJPlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;


/**
 * After running this plugin the current display will now reference (and
 * show) the overlays that are currently selected in the Overlay Manager.
 * 
 * @author Barry DeZonia
 *
 */
@Plugin(menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Overlay", mnemonic = 'o'),
	@Menu(label = "From Overlay Manager", weight = 5, mnemonic = 'f') },
	headless = true)
public class ReferenceOverlays implements ImageJPlugin {

	// -- Parameters --
	
	@Parameter(required = true)
	private ImageDisplay display;
	
	@Parameter
	private OverlayService ovrSrv;
	
	// -- ImageJPlugin methods --
	
	@Override
	public void run() {
		OverlayInfoList overlayList = ovrSrv.getOverlayInfo();
		List<Overlay> selectedRoiMgrOverlays = overlayList.selectedOverlays();
		List<Overlay> currOverlays = ovrSrv.getOverlays(display);
		boolean changes = false;
		for (Overlay overlay : selectedRoiMgrOverlays) {
			if (currOverlays.contains(overlay)) continue;
			changes = true;
			display.display(overlay);
		}
		if (changes) display.update();
	}
	
	// -- accessors --
	
	public OverlayService getOverlayService() {
		return ovrSrv;
	}
	
	public void setOverlayService(OverlayService os) {
		ovrSrv = os;
	}
	
	public ImageDisplay getImageDisplay() {
		return display;
	}
	
	public void setImageDisplay(ImageDisplay disp) {
		display = disp;
	}

}
