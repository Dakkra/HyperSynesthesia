package com.dakkra.hypersynesthesia.dsp;

public class PeakLoudness {

	private int peakLoudness = 0;

	public void process( int[] samples ) {
		for( int sample : samples ) {
			if( Math.abs( sample ) > peakLoudness ) {
				peakLoudness = Math.abs( sample );
			}
		}
	}

	public int getPeak() {
		return peakLoudness;
	}

	public double getPeakLoudness() {
		return (double)peakLoudness / (double)Integer.MAX_VALUE;
	}

	public void reset() {
		peakLoudness = 0;
	}
}
