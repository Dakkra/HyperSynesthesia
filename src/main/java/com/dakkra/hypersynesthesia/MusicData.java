package com.dakkra.hypersynesthesia;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import lombok.CustomLog;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@CustomLog
public class MusicData {

	private final InputStream input;

	@Getter
	private Long sampleRate;

	@Getter
	private List<Integer> samplesLeft;

	@Getter
	private List<Integer> samplesRight;

	@Getter
	private List<Integer> samplesAvg;

	@Getter
	private int sampleCount;

	public MusicData( InputStream input ) {
		this.input = input;
	}

	public MusicData load() {
		samplesLeft = new ArrayList<>();
		samplesRight = new ArrayList<>();

		FFmpeg.atPath().addInput( PipeInput.pumpFrom( input ) ).addOutput( FrameOutput.withConsumer( new FrameConsumer() {

			@Override
			public void consumeStreams( List<Stream> streams ) {
				sampleRate = streams.get( 0 ).getSampleRate();
			}

			@Override
			public void consume( Frame frame ) {
				// End of stream
				if( frame == null ) return;

				// Add samples to the sample buffers
				for( int index = 0; index < frame.getSamples().length; index++ ) {
					if( index % 2 == 0 ) {
						samplesRight.add( frame.getSamples()[ index ] );
					} else {
						samplesLeft.add( frame.getSamples()[ index ] );
					}
				}
			}
		} ).disableStream( StreamType.VIDEO ).disableStream( StreamType.DATA ).disableStream( StreamType.SUBTITLE ) ).execute();

		sampleCount = samplesLeft.size();

		// Simple mean averaging of the left and right channels
		samplesAvg = new ArrayList<>( sampleCount );
		IntStream.range( 0, sampleCount ).forEach( ( index ) -> {
			samplesAvg.add(  (samplesLeft.get( index ) + samplesRight.get( index ) / 2) );
		} );

		log.atDebug().log( "Loaded sample count=%d", sampleCount );

		return this;
	}

}
