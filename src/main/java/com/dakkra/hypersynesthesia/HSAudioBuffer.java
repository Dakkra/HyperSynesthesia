package com.dakkra.hypersynesthesia;

import java.util.List;
import java.util.Vector;

public class HSAudioBuffer {

	private List<Short> buffer;

	public HSAudioBuffer() {
		this.buffer = new Vector<>();
	}

	public void addSample( short sample ) {
		buffer.add( sample );
	}

	public void addSamples( short[] samples ) {
		if( samples == null ) throw new NullPointerException( "Samples cannot be null" );
		for( short sample : samples ) {
			addSample( sample );
		}
	}
}
