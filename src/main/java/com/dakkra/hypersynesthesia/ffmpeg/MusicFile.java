package com.dakkra.hypersynesthesia.ffmpeg;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.IntStream;

@Getter
public class MusicFile {

	private final Path file;

	private Vector<Integer> samplesLeft;

	private Vector<Integer> samplesRight;

	private Vector<Integer> samplesAvg;

	private int sampleRate;

	private int numSamples;

	private Duration duration;

	// FIXME This is modified in the rendering process
	// This is loaded by ProjectProcessor.loadMusicFile()
	private PriorityBlockingQueue<PrioritySpectrum> fftQueue;

	public MusicFile( Path file ) {
		this.file = file;
	}

	public MusicFile load() {
		if( !Files.exists( file ) ) {
			throw new RuntimeException( "Input file not found: " + file.toAbsolutePath() );
		}

		samplesLeft = new Vector<>();
		samplesRight = new Vector<>();
		samplesAvg = new Vector<>();

		FFmpeg.atPath().addInput( UrlInput.fromPath( file ) ).addOutput( FrameOutput.withConsumer( new FrameConsumer() {

			@Override
			public void consumeStreams( List<Stream> streams ) {
				sampleRate = streams.getFirst().getSampleRate().intValue();
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

		// Simple mean averaging of the left and right channels
		samplesAvg.setSize( samplesLeft.size() );
		IntStream.range( 0, samplesAvg.size() ).forEach( ( index ) -> {
			samplesAvg.set( index, (samplesLeft.get( index ) + samplesRight.get( index ) / 2) );
		} );

		numSamples = samplesAvg.size();
		duration = Duration.ofSeconds( numSamples / sampleRate );
		this.fftQueue = new PriorityBlockingQueue<>( sampleRate / 60 );

		return this;
	}

}
