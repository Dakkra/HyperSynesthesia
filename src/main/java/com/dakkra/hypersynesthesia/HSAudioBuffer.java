package com.dakkra.hypersynesthesia;

import java.util.List;
import java.util.Vector;

public class HSAudioBuffer {

	private List<Integer> buffer;

	public HSAudioBuffer() {
		this.buffer = new Vector<>();
	}

	public void addSample( int sample ) {
		buffer.add( sample );
	}

	public void addSamples( int[] samples ) {
		if( samples == null ) throw new NullPointerException( "Samples cannot be null" );
		for( int sample : samples ) {
			addSample( sample );
		}
	}
}
