package com.dakkra.hypersynesthesia;

public record PrioritySpectrum(DSP dsp) implements Comparable<PrioritySpectrum> {

	public double[] getSpectrum() {
		return dsp.getSpectrum();
	}

	public int getIndex() {
		return dsp.getIndex();
	}

	@Override
	public int compareTo( PrioritySpectrum that ) {
		return that.getIndex() - this.getIndex();
	}

}
