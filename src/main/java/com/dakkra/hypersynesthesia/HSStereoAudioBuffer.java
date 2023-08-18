package com.dakkra.hypersynesthesia;

public class HSStereoAudioBuffer {

	private final HSAudioBuffer leftBuffer;

	private final HSAudioBuffer rightBuffer;

	public HSStereoAudioBuffer() {
		this.leftBuffer = new HSAudioBuffer();
		this.rightBuffer = new HSAudioBuffer();
	}

	public void addSample( short leftSample, short rightSample ) {
		leftBuffer.addSample( leftSample );
		rightBuffer.addSample( rightSample );
	}

	public void addSamples( short[] leftSamples, short[] rightSamples ) {
		if( leftSamples == null || rightSamples == null ) throw new NullPointerException( "Samples cannot be null" );
		if( leftSamples.length != rightSamples.length ) throw new IllegalArgumentException( "Left and right samples must be the same length" );
		for( int i = 0; i < leftSamples.length; i++ ) {
			addSample( leftSamples[ i ], rightSamples[ i ] );
		}
	}
}
