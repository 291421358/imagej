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

package imagej.ui.viewer;

import imagej.display.Display;
import imagej.display.event.DisplayActivatedEvent;
import imagej.display.event.DisplayDeletedEvent;
import imagej.display.event.DisplayUpdatedEvent;
import imagej.display.event.DisplayUpdatedEvent.DisplayUpdateLevel;
import imagej.event.EventService;
import imagej.event.EventSubscriber;
import imagej.plugin.SortablePlugin;

import java.util.List;

/**
 * The AbstractDisplayViewer provides some basic generic implementations for a
 * DisplayViewer such as storing and providing the display, window and panel for
 * a DisplayViewer.
 * 
 * @author Lee Kamentsky
 * @author Curtis Rueden
 */
public abstract class AbstractDisplayViewer<T> extends SortablePlugin implements
	DisplayViewer<T>
{

	private Display<T> display;
	private DisplayWindow window;
	private DisplayPanel panel;

	private List<EventSubscriber<?>> subscribers;

	@Override
	public void view(final DisplayWindow w, final Display<?> d) {
		if (!canView(d)) {
			throw new IllegalArgumentException("Incompatible display: " + d);
		}
		@SuppressWarnings("unchecked")
		final Display<T> typedDisplay = (Display<T>) d;
		display = typedDisplay;
		window = w;

		if (subscribers != null) getEventService().unsubscribe(subscribers);
		subscribers = getEventService().subscribe(this);
	}

	@Override
	public Display<T> getDisplay() {
		return display;
	}

	@Override
	public DisplayWindow getWindow() {
		return window;
	}

	@Override
	public void setPanel(final DisplayPanel panel) {
		this.panel = panel;
	}

	@Override
	public DisplayPanel getPanel() {
		return panel;
	}

	@Override
	public void onDisplayDeletedEvent(final DisplayDeletedEvent e) {
		getPanel().getWindow().close();
	}

	@Override
	public void onDisplayUpdatedEvent(final DisplayUpdatedEvent e) {
		if (e.getLevel() == DisplayUpdateLevel.REBUILD) {
			getPanel().redoLayout();
		}
		getPanel().redraw();
	}

	@Override
	public void onDisplayActivatedEvent(final DisplayActivatedEvent e) {
		getPanel().getWindow().requestFocus();
	}

	// -- Internal AbstractDisplayViewer methods --

	/** Convenience method to obtain the appropriate {@link EventService}. */
	protected EventService getEventService() {
		// NB: It is best to use the direct reference to display here rather than
		// calling getDisplay(), since it has access to the context, and should
		// always be populated via an initial call to super.view(w, d).
		if (display == null) return null;
		return display.getContext().getService(EventService.class);
	}

}
