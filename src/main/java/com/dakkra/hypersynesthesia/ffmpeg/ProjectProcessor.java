package com.dakkra.hypersynesthesia.ffmpeg;

import com.avereon.product.Rb;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.task.Task;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.CustomLog;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@CustomLog
public class ProjectProcessor {

	private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat( "##.##%" );

	private final XenonProgramProduct product;

	@Getter
	private Duration renderDuration;

	public ProjectProcessor( XenonProgramProduct product ) {
		this.product = product;
	}

	public MusicFile loadMusicFile( Path inputFile ) {
		MusicFile music = loadMusicFile( inputFile, _ -> {} );
		System.out.println( "Music File " + inputFile.getFileName() + " has " + music.getNumSamples() + " samples" );
		System.out.println( "Number of FFT tasks: " + music.getFftQueue().size() );
		return music;
	}

	public MusicFile loadMusicFile( Path inputFile, Consumer<Double> progressConsumer ) {
		MusicFile music = new MusicFile( inputFile ).load();

		// pre calc FFTs multithreaded
		ArrayList<Future<?>> futures = new ArrayList<>();
		int numFFTs = (int)(music.getNumSamples() / (music.getSampleRate() / 60.0));
		for( int frameIdx = 0; frameIdx <= numFFTs; frameIdx++ ) {
			int index = frameIdx;
			int audioBufferSize = music.getSampleRate() / 60;
			int delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - frameIdx * audioBufferSize );
			Future<?> result = product.getProgram().getTaskManager().submit( Task.of( () -> {
				int[] samplesAvg = music.getSamplesAvg().subList( index * audioBufferSize, index * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
				DSP dsp = new DSP();
				dsp.processFull( samplesAvg );
				music.getFftQueue().offer( new PrioritySpectrum( new ArrayList<>( Arrays.asList( Arrays.stream( dsp.getSpectrum() ).boxed().toArray( Double[]::new ) ) ), index ) );
			} ) );
			futures.add( result );
		}

		double count = 0;
		double total = numFFTs + 1;
		progressConsumer.accept( 0.0 );
		for( Future<?> future : futures ) {
			try {
				future.get();
				progressConsumer.accept( ++count / total );
			} catch( InterruptedException | ExecutionException e ) {
				log.atError().withCause( e ).log( "Error calculating FFT" );
			}
		}

		return music;
	}

	public FrameRenderer renderVideoFile( MusicFile music, RenderSettings settings, Consumer<Double> progressConsumer, Consumer<String> messageConsumer ) {
		// NOTE Is this where the processing is split between loading and rendering?

		System.out.println( "Frame rendering..." );
		Vector<String> fileNameList = new Vector<>();
		FrameRenderer frameRenderer = new FrameRenderer( product.getProgram(), fileNameList, music, settings, progressConsumer );

		long initialTime = Clock.systemUTC().millis();

		// Render
		frameRenderer.run();

		System.out.println( "Frame rendering complete" );
		System.out.println( "Rendered " + frameRenderer.getFrameCount() + " frames" );

		// Encode video
		System.out.println( "Encoding video..." );
		messageConsumer.accept( Rb.text( product, "tool", "encoding-video" ) );
		try {
			Files.delete(  settings.targetPath() );
		} catch( IOException exception ) {
			throw new RuntimeException( exception );
		}
		FFmpeg
			.atPath()
			.addInput( UrlInput.fromPath( music.getFile() ) )
			.addOutput( UrlOutput.toPath( settings.targetPath() ) )
			.addArguments( "-framerate", "60" )
			.addArguments( "-i", "output%d.jpg" )
			.addArguments( "-crf", "15" )
			.execute();
		System.out.println( "Video encoding complete" );

		long finalTime = Clock.systemUTC().millis();
		long deltaTime = finalTime - initialTime;
		renderDuration = Duration.ofMillis( deltaTime );
		Duration musicDuration = music.getDuration();
		double renderRatio = (double)musicDuration.getSeconds() / (double)renderDuration.getSeconds();

		System.out.println( "Input file duration: " + musicDuration.toMinutesPart() + " minutes and " + musicDuration.toSecondsPart() + " seconds" );
		System.out.println( "Target video resolution: " + settings.width() + "x" + settings.height() );
		System.out.println( "Render and encoding took: " + renderDuration.toMinutesPart() + " minutes and " + renderDuration.toSecondsPart() + " seconds" );
		System.out.println( "Render and encoding was " + NUMBER_FORMAT.format( renderRatio ) + " of real time" );

		// Clean up
		for( String fileName : fileNameList ) {
			new File( fileName ).delete();
		}

		return frameRenderer;
	}

}
