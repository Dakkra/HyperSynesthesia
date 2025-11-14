package com.dakkra.hypersynesthesia.ffmpeg;

import com.avereon.xenon.Xenon;
import com.avereon.xenon.task.Task;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.CustomLog;
import lombok.Getter;

import java.io.File;
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

	private final Xenon program;

	@Getter
	private Duration renderDuration;

	public ProjectProcessor( Xenon program ) {
		this.program = program;
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
			int audioBufferSize = (int)(music.getSampleRate() / 60);
			int delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - frameIdx * audioBufferSize );
			Future<?> result = program.getTaskManager().submit( Task.of( () -> {
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

	public Renderer renderVideoFile( MusicFile music, int width, int height, Path outputFile ) {
		return renderVideoFile( music, width, height, outputFile, _ -> {} );
	}

	public Renderer renderVideoFile( MusicFile music, int width, int height, Path outputFile, Consumer<Double> progressConsumer ) {
		// NOTE Is this where the processing is split between loading and rendering?

		System.out.println( "Triggering render" );
		Vector<String> fileNameList = new Vector<>();
		Renderer renderer = new Renderer( program, fileNameList, music, width, height, progressConsumer );

		long initialTime = Clock.systemUTC().millis();

		// Render
		renderer.run();

		System.out.println( "Rendering complete" );
		System.out.println( "Rendered " + fileNameList.size() + " frames" );
		System.out.println( "Encoding video" );

		// Encode video
		FFmpeg
			.atPath()
			.addInput( UrlInput.fromPath( music.getFile() ) )
			.addOutput( UrlOutput.toPath( outputFile ) )
			.addArguments( "-framerate", "60" )
			.addArguments( "-i", "output%d.jpg" )
			.addArguments( "-crf", "15" )
			.execute();

		long finalTime = Clock.systemUTC().millis();
		long deltaTime = finalTime - initialTime;
		renderDuration = Duration.ofMillis( deltaTime );
		Duration musicDuration = music.getDuration();
		double renderRatio = (double)musicDuration.getSeconds() / (double)renderDuration.getSeconds();

		System.out.println( "Input file duration: " + musicDuration.toMinutesPart() + " minutes and " + musicDuration.toSecondsPart() + " seconds" );
		System.out.println( "Target video resolution: " + width + "x" + height );
		System.out.println( "Render and encoding took: " + renderDuration.toMinutesPart() + " minutes and " + renderDuration.toSecondsPart() + " seconds" );
		System.out.println( "Render and encoding was " + NUMBER_FORMAT.format( renderRatio ) + " of real time" );

		// Clean up
		for( String fileName : fileNameList ) {
			new File( fileName ).delete();
		}

		return renderer;
	}

}
