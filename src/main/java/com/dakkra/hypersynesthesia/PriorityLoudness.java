package com.dakkra.hypersynesthesia;

public record PriorityLoudness(DSP dsp) implements Comparable<PriorityLoudness> {

	public double getLoudness() {
		return dsp.getRMSLoudness();
	}

	public int getIndex() {
		return dsp.getIndex();
	}

	@Override
	public int compareTo( PriorityLoudness that ) {
		return that.getIndex() - this.getIndex();
	}

}
