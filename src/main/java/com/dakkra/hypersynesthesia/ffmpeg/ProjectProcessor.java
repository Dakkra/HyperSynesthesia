package com.dakkra.hypersynesthesia.ffmpeg;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class ProjectProcessor {

	private static final int threadCount = Runtime.getRuntime().availableProcessors();

	private static final DecimalFormat df = new DecimalFormat( "##.##%" );

	// FIXME This can be replaced with the Xenon task pool executor
	private final ThreadPoolExecutor pool;

	public ProjectProcessor() {
		pool = (ThreadPoolExecutor)Executors.newFixedThreadPool( threadCount );
	}

	@Deprecated
	public static void main( String[] args ) {
		new ProjectProcessor().run();
	}

	@Deprecated
	private void run() {
		System.out.println( "Starting HyperSynestheisa multi-threaded tech demo" );
		System.out.println( "Discovered " + threadCount + " threads" );

		int width = 1920;
		int height = 1080;

		Path inputFile = Path.of( "./DemoTrack.wav" );

		Path outputFile = Path.of( "output.mp4" );
		if( Files.exists( outputFile ) && !outputFile.toFile().delete() ) {
			throw new RuntimeException( "Failed to delete output file" );
		}

		// Step one
		MusicFile music = loadMusicFile( inputFile );

		// Step two
		renderVideoFile( music, width, height, outputFile );

		System.out.println( "Done" );
	}

	public MusicFile loadMusicFile( Path inputFile ) {
		MusicFile music = new MusicFile( inputFile ).load();
		System.out.println( "Compute pool size: " + pool.getCorePoolSize() );

		// pre calc FFTs multithreaded
		System.out.println( "Pre-calculating FFTs" );
		ArrayList<Future<?>> futures = new ArrayList<>();
		int numFFTs = (int)(music.getNumSamples() / (music.getSampleRate() / 60.0));
		System.out.println( "Number of FFT tasks: " + numFFTs );
		for( int frameIdx = 0; frameIdx <= numFFTs; frameIdx++ ) {
			int index = frameIdx;
			int audioBufferSize = (int)(music.getSampleRate() / 60);
			int delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - frameIdx * audioBufferSize );
			Future<?> result = pool.submit( () -> {
				int[] samplesAvg = music.getSamplesAvg().subList( index * audioBufferSize, index * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
				DSP dsp = new DSP();
				dsp.processFull( samplesAvg );
				music.getFftQueue().offer( new PrioritySpectrum( new ArrayList<>( Arrays.asList( Arrays.stream( dsp.getSpectrum() ).boxed().toArray( Double[]::new ) ) ), index ) );
			} );
			futures.add( result );
		}

		System.out.println( "FFT Tasks submitted, waiting for completion" );

		for( Future<?> future : futures ) {
			try {
				future.get();
			} catch( InterruptedException | ExecutionException e ) {
				e.printStackTrace();
			}
		}

		System.out.println( "Done pre-calculating FFTs, waiting for threads to finish" );
		System.out.println( "FFT Queue size: " + music.getFftQueue().size() );

		return music;
	}

	public void renderVideoFile( MusicFile music, int width, int height, Path outputFile ) {

		// NOTE Is this where the processing is split between loading and rendering?

		System.out.println( "Triggering render" );
		Vector<String> fileNameList = new Vector<>();
		Renderer renderer = new Renderer( pool, fileNameList, music, width, height );

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
		Duration duration = Duration.ofMillis( deltaTime );

		long musicSeconds = music.getNumSamples() / music.getSampleRate();
		System.out.println( "Input file duration: " + musicSeconds + " seconds" );
		System.out.println( "Target video resolution: " + width + "x" + height );
		System.out.println( "Render and encoding took: " + duration.toMinutesPart() + " minutes and " + duration.toSecondsPart() + " seconds" );
		System.out.println( "Render and encoding was " + df.format( (double)musicSeconds / (double)duration.getSeconds() ) + " of real time" );

		// Clean up
		for( String fileName : fileNameList ) {
			new File( fileName ).delete();
		}

		pool.shutdown();
	}
}