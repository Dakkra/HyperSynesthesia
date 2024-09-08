package com.dakkra.hypersynesthesia.ffmpeg;

import java.util.ArrayList;

class PrioritySpectrum implements Comparable {

	private ArrayList<Double> spectrum;

	private long frameIdx;

	public PrioritySpectrum( ArrayList<Double> spectrum, long frameIdx ) {
		this.spectrum = spectrum;
		this.frameIdx = frameIdx;
	}

	public ArrayList<Double> getSpectrum() {
		return spectrum;
	}

	public long getFrameIdx() {
		return frameIdx;
	}

	@Override
	public int compareTo( Object other ) {
		if( other instanceof PrioritySpectrum ) {
			long otherFrameIdx = ((PrioritySpectrum)other).frameIdx;
			return (int)(this.frameIdx - otherFrameIdx);
		}
		throw new UnsupportedOperationException( "Unimplemented method 'compareTo'" );
	}
}
