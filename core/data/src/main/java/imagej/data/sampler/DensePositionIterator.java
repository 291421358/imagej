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

package imagej.data.sampler;


/**
 * @author Barry DeZonia
 */
class DensePositionIterator implements PositionIterator {
	private int[] maxIndexes;
	private int[] indexes;
	private long[] currPos;
	
	DensePositionIterator(SamplingDefinition def) {
		maxIndexes = calcMaxes(def);
		currPos = new long[maxIndexes.length];
		for (int i = 0; i < currPos.length; i++)
			currPos[i] = 0;
		indexes = new int[maxIndexes.length];
		indexes[0] = -1;
	}

	@Override
	public boolean hasNext() {
		for (int i = 0; i < currPos.length; i++) {
			if (indexes[i] < maxIndexes[i]) return true;
		}
		return false;
	}

	@Override
	public long[] next() {
		for (int i = 0; i < indexes.length; i++) {
			int nextPos = indexes[i] + 1;
			if (nextPos <= maxIndexes[i]) {
				indexes[i] = nextPos;
				currPos[i] = nextPos;
				return currPos;
			}
			indexes[i] = 0;
			currPos[i] = 0;
		}
		throw new IllegalArgumentException("Can't position iterator beyond end");
	}
	
	private int[] calcMaxes(SamplingDefinition def) {
		long[] dims = def.getOutputDims();
		int[] mx = new int[dims.length];
		for (int i = 0; i < dims.length; i++) {
			if (dims[i] > Integer.MAX_VALUE)
				throw new IllegalArgumentException(
					"Can only iterate <= 2 gig per dimension");
			mx[i] = (int) (dims[i] - 1);
		}
		return mx;
	}
}

