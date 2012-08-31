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

package imagej.core.plugins.restructure;

import imagej.data.Dataset;
import imagej.data.display.ImageDisplayService;
import imagej.data.event.DataRestructuredEvent;
import imagej.display.event.DisplayActivatedEvent;
import imagej.event.EventHandler;
import imagej.event.EventService;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;
import imagej.plugin.CommandInfo;
import imagej.plugin.PluginService;
import imagej.service.AbstractService;
import imagej.service.Service;
import net.imglib2.meta.Axes;

/**
 * Service that monitors context for the @{link SplitChannelsContext} plugin.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class SplitChannelsContextMonitor extends AbstractService {

	@Parameter
	private EventService eventService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	// -- Service methods --

	@Override
	public void initialize() {
		subscribeToEvents(eventService);
	}

	// -- Event handlers --

	@EventHandler
	protected void onEvent(@SuppressWarnings("unused")
	final DisplayActivatedEvent event)
	{
		checkContext();
	}

	@EventHandler
	protected void onEvent(@SuppressWarnings("unused")
	final DataRestructuredEvent event)
	{
		checkContext();
	}

	// -- Helper methods --

	private void checkContext() {
		final Dataset dataset = imageDisplayService.getActiveDataset();
		if (dataset == null) {
			setContextAppropriate(false);
			return;
		}
		final int channelIndex = dataset.getAxisIndex(Axes.CHANNEL);
		if (channelIndex < 0) {
			setContextAppropriate(false);
			return;
		}
		final long channelCount = dataset.dimension(channelIndex);
		setContextAppropriate(channelCount > 1);
	}

	// TODO: Figure out why, when right-clicking an inactive window:
	// module info is not up-to-date.
	// To test:
	// 1) Open Blobs.
	// 2) Open Clown.
	// 3) Right-click Blobs. "Split Channels" is not grayed out.
	// WinActivated does fire before MsPressed, so the behavior is mysterious.

	private void setContextAppropriate(final boolean enabled) {
		final CommandInfo<SplitChannelsContext> info =
			pluginService.getCommand(SplitChannelsContext.class);
		info.setEnabled(enabled);
		info.update(eventService); // TODO: Is this needed here?
	}

}
