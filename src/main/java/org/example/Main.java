package org.example;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.tambapps.fft4j.FastFouriers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

class ColorUtil {

	public static Color colorWithIntensity( Color color, double intensity ) {
		return new Color( (int)(color.getRed() * intensity), (int)(color.getGreen() * intensity), (int)(color.getBlue() * intensity), color.getAlpha() );
	}
}

class PrioritySpectrum implements Comparable {

	private ArrayList<Double> spectrum;

	private long frameIdx;

	public PrioritySpectrum( ArrayList<Double> spectrum, long frameIdx ) {
		this.spectrum = spectrum;
		this.frameIdx = frameIdx;
	}

	public ArrayList<Double> getSpectrum() {
		return spectrum;
	}

	public long getFrameIdx() {
		return frameIdx;
	}

	@Override
	public int compareTo( Object other ) {
		if( other instanceof PrioritySpectrum ) {
			long otherFrameIdx = ((PrioritySpectrum)other).frameIdx;
			return (int)(this.frameIdx - otherFrameIdx);
		}
		throw new UnsupportedOperationException( "Unimplemented method 'compareTo'" );
	}
}

record EnrichedImage(BufferedImage internalImage, long frameIdx) implements Comparable {

	@Override
	public int compareTo( Object other ) {
		if( other instanceof EnrichedImage ) {
			long otherFrameIdx = ((EnrichedImage)other).frameIdx;
			return (int)(this.frameIdx - otherFrameIdx);
		}
		throw new UnsupportedOperationException( "Unimplemented method 'compareTo'" );
	}
}

class DSP {

	private int peakLoudness = 0;

	private double rms = 0;

	private double[] spectrum = null;

	// Skips the fft
	public void processLight( int[] samples ) {
		for( int sample : samples ) {
			if( Math.abs( sample ) > peakLoudness ) {
				peakLoudness = Math.abs( sample );
			}
		}

		// Calculate RMS
		double sum = 0;
		for( int sample : samples ) {
			sum += ((double)sample * (double)sample);
		}
		rms = Math.sqrt( sum / samples.length );
	}

	public void processFull( int[] samples ) {
		processLight( samples );

		// Calculate FFT
		double[] real = new double[ samples.length ];
		double[] imag = new double[ samples.length ];
		for( int i = 0; i < samples.length; i++ ) {
			real[ i ] = samples[ i ];
			imag[ i ] = 0;
		}

		double[] outputReal = new double[ samples.length ];
		double[] outputImag = new double[ samples.length ];
		FastFouriers.BASIC.transform( real, imag, outputReal, outputImag );

		// Create spectrum
		spectrum = new double[ samples.length / 2 ];
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] = Math.sqrt( outputReal[ i ] * outputReal[ i ] + outputImag[ i ] * outputImag[ i ] );
		}

		// Remove bottom few buckets
		int skippedBuckets = 4;
		spectrum = new ArrayList<>( Arrays.asList( Arrays.stream( spectrum ).boxed().toArray( Double[]::new ) ) ).subList( skippedBuckets, spectrum.length ).stream().mapToDouble( i -> i ).toArray();
		// Convert to dB
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] = 20 * Math.log10( spectrum[ i ] );
		}

		// Compute max
		double max = 0;
		for( double value : spectrum ) {
			if( value > max ) {
				max = value;
			}
		}

		// Expand spectrum
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] = spectrum[ i ] - 0.85 * max;
		}

		// Remove negative values
		for( int i = 0; i < spectrum.length; i++ ) {
			if( spectrum[ i ] < 0 ) {
				spectrum[ i ] = 0;
			}
		}

		// Compute max again
		max = 0;
		for( double value : spectrum ) {
			if( value > max ) {
				max = value;
			}
		}

		// Normalize spectrum
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] /= max;
		}
	}

	public int getPeak() {
		return peakLoudness;
	}

	public double getRMS() {
		return rms;
	}

	public double[] getSpectrum() {
		return spectrum;
	}

	public double getRMSLoudness() {
		return rms / (double)Integer.MAX_VALUE;
	}

	public double getPeakLoudness() {
		return (double)peakLoudness / (double)Integer.MAX_VALUE;
	}

	public void reset() {
		peakLoudness = 0;
		spectrum = null;
	}
}

class MusicFile {

	private final File file;

	private Vector<Integer> samplesLeft;

	private Vector<Integer> samplesRight;

	private Vector<Integer> samplesAvg;

	private Long sampleRate = 0L;

	private int numSamples;

	public MusicFile( File file ) {
		this.file = file;
	}

	public void load() {
		if( !file.exists() ) {
			throw new RuntimeException( "Input file not found: " + file.getAbsolutePath() );
		}

		samplesLeft = new Vector<>();
		samplesRight = new Vector<>();
		samplesAvg = new Vector<>();

		FFmpeg.atPath().addInput( UrlInput.fromPath( file.toPath() ) ).addOutput( FrameOutput.withConsumer( new FrameConsumer() {

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

		// Simple mean averaging of the left and right channels
		samplesAvg.setSize( samplesLeft.size() );
		IntStream.range( 0, samplesAvg.size() ).forEach( ( index ) -> {
			samplesAvg.set( index, (samplesLeft.get( index ) + samplesRight.get( index ) / 2) );
		} );

		numSamples = samplesAvg.size();

		System.out.println( "Music File " + file.getName() + " has " + numSamples + " samples" );
	}

	public Vector<Integer> getSamplesLeft() {
		return samplesLeft;
	}

	public Vector<Integer> getSamplesRight() {
		return samplesRight;
	}

	public Vector<Integer> getSamplesAvg() {
		return samplesAvg;
	}

	public int getNumSamples() {
		return numSamples;
	}

	public Long getSampleRate() {
		return sampleRate;
	}
}

class Renderer {

	public BufferedImage art;

	public static final int AVG_QUEUE_SIZE = 5;

	private ThreadPoolExecutor renderPool;

	private MusicFile music;

	private int frameWidth;

	private int frameHeight;

	private Queue<Double> rmsQueue = new ArrayBlockingQueue<>( AVG_QUEUE_SIZE );

	private Queue<ArrayList<Double>> spectrumsQueue = null;

	private PriorityBlockingQueue<PrioritySpectrum> spectralQueue;

	private Vector<String> nameList;

	Renderer(
		ThreadPoolExecutor renderPool, Vector<String> nameList, PriorityBlockingQueue<PrioritySpectrum> spectralQueue, MusicFile music, int frameWidth, int frameHeight
	) {
		this.renderPool = renderPool;
		this.nameList = nameList;
		this.music = music;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.spectralQueue = spectralQueue;
		try {
			art = ImageIO.read( Objects.requireNonNull( this.getClass().getResourceAsStream( "/hs-logo.png" ) ) );
		} catch( IOException e ) {
			throw new RuntimeException( e );
		}

		for( int i = 0; i < AVG_QUEUE_SIZE; i++ ) {
			rmsQueue.add( 0.0 );
		}
	}

	private static EnrichedImage renderImage(
		double loudness, ArrayList<Double> spectrum, int frameWidth, int frameHeight, long frameIdx, BufferedImage art
	) {
		BufferedImage image = new BufferedImage( frameWidth, frameHeight, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D graphics = image.createGraphics();
		graphics.setColor( Color.BLACK );
		graphics.fillRect( 0, 0, frameWidth, frameHeight );
		graphics.setColor( Color.WHITE );
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		Font font = new Font( "Serif", Font.PLAIN, 32 );
		graphics.setFont( font );
		graphics.drawString( "Frame: " + frameIdx, 10, 20 );

		//Render spectrum
		int barMaxHeight = frameHeight / 2;
		int barCount = spectrum.size();
		for( int i = 0; i < barCount; i++ ) {
			double x = Math.log10( i ) / Math.log10( barCount ) * frameWidth;
			double barWidth = Math.log10( i + 1 ) / Math.log10( barCount ) * frameWidth - x;
			barWidth = Math.max( 1, barWidth );
			int barHeight = (int)(spectrum.get( i ) * barMaxHeight);
			graphics.setPaint( ColorUtil.colorWithIntensity( new Color( 140, 239, 137 ), spectrum.get( i ) ) );
			graphics.fillRect( (int)x, frameHeight / 2 - barHeight / 2, (int)barWidth, barHeight );
		}

		int smallest = Math.min( frameWidth, frameHeight );
		int size = smallest / 10 + (int)(loudness * smallest);
		AffineTransform transform = graphics.getTransform();
		Rectangle2D rect = new Rectangle2D.Double( ((double)frameWidth / 2) - size / 2.0f, ((double)frameHeight / 2) - size / 2.0f, size, size );
		double rotateX = rect.getX() + rect.getWidth() / 2;
		double rotateY = rect.getY() + rect.getHeight() / 2;
		graphics.rotate( Math.toRadians( frameIdx ), rotateX, rotateY );
		graphics.setPaint( ColorUtil.colorWithIntensity( new Color( 127, 255, 197, 150 ), loudness ) );
		graphics.fillRect( rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height );
		graphics.setTransform( transform );
		graphics.drawImage( art, rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height, null );

		return new EnrichedImage( image, frameIdx );
	}

	public void run() {
		final int audioBufferSize = (int)(music.getSampleRate() / 60);
		final int numFrames = spectralQueue.size();
		Vector<Future<?>> futures = new Vector<>();
		for( int counter = 0; counter < numFrames; counter++ ) {
			DSP dsp = new DSP();
			int delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - counter * audioBufferSize );
			if( delta <= 0 ) {
				return;
			}
			int[] samplesAvg = music.getSamplesAvg().subList( (int)counter * audioBufferSize, (int)counter * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
			dsp.processLight( samplesAvg );

			// Smooth RMS
			double intensity = dsp.getRMSLoudness();
			double avg = rmsQueue.stream().mapToDouble( Double::doubleValue ).average().orElse( 0.0 );
			rmsQueue.remove();
			rmsQueue.offer( intensity );

			// Smooth spectrum
			double[] newSpectrum = new double[ 0 ];
			try {
				newSpectrum = spectralQueue.take().getSpectrum().stream().mapToDouble( i -> i ).toArray();
			} catch( InterruptedException e ) {
				throw new RuntimeException( e );
			}
			if( spectrumsQueue == null ) {
				spectrumsQueue = new ArrayBlockingQueue<>( AVG_QUEUE_SIZE );
				for( int i = 0; i < AVG_QUEUE_SIZE; i++ ) {
					spectrumsQueue.offer( new ArrayList<>( Arrays.asList( Arrays.stream( new double[ newSpectrum.length ] ).boxed().toArray( Double[]::new ) ) ) );
				}
			}
			spectrumsQueue.remove();
			spectrumsQueue.offer( new ArrayList<>( Arrays.asList( Arrays.stream( newSpectrum ).boxed().toArray( Double[]::new ) ) ) );
			ArrayList<Double> spectrumAvg = new ArrayList<>( newSpectrum.length );
			for( int bucket = 0; bucket < newSpectrum.length; bucket++ ) {
				double sum = 0;
				for( ArrayList<Double> nextSpectrum : spectrumsQueue ) {
					sum += nextSpectrum.get( bucket ) != null ? nextSpectrum.get( bucket ) : 0;
				}
				spectrumAvg.add( sum / spectrumsQueue.size() );
			}

			int index = counter;
			Future<?> future = this.renderPool.submit( () -> {
				try {
					ImageIO.write( renderImage( avg, new ArrayList<>( spectrumAvg ), frameWidth, frameHeight, index, art ).internalImage(), "jpg", new File( "output" + index + ".jpg" ) );
					nameList.add( "output" + index + ".jpg" );
				} catch( IOException e ) {
					throw new RuntimeException( e );
				}
			} );
			futures.add( future );
		}
		for( Future<?> future : futures ) {
			try {
				future.get();
			} catch( InterruptedException | ExecutionException e ) {
				e.printStackTrace();
			}
		}
	}
}

public class Main {

	private static final int threadCount = Runtime.getRuntime().availableProcessors();

	public static void main( String[] args ) {
		System.out.println( "Starting HyperSynestheisa multi-threaded tech demo" );
		System.out.println( "Discovered " + threadCount + " threads" );

		int width = 1920;
		int height = 1080;

		File out = new File( "output.mp4" );
		if( out.exists() ) {
			boolean result = out.delete();
			if( !result ) {
				throw new RuntimeException( "Failed to delete output file" );
			}
		}

		Vector<String> fileNameList = new Vector<>();

		File inputFile = new File( "./DemoTrack.wav" );
		MusicFile music = new MusicFile( inputFile );
		music.load();
		ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool( threadCount );
		System.out.println( "Compute pool size: " + pool.getCorePoolSize() );

		// pre calc FFTs multi-threaded
		System.out.println( "Pre-calculating FFTs" );
		PriorityBlockingQueue<PrioritySpectrum> fftQueue = new PriorityBlockingQueue<>( (int)(music.getSampleRate() / 60) );
		DecimalFormat df = new DecimalFormat( "##.##%" );
		ArrayList<Future<?>> futures = new ArrayList<>();
		int numFFTs = (int)(music.getNumSamples() / (music.getSampleRate() / 60.0));
		System.out.println( "Number of FFT tasks: " + numFFTs );
		for( int frameIdx = 0; frameIdx <= numFFTs; frameIdx++ ) {
			Integer index = frameIdx;
			int audioBufferSize = (int)(music.getSampleRate() / 60);
			int delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - frameIdx * audioBufferSize );
			Future<?> result = pool.submit( () -> {
				int[] samplesAvg = music.getSamplesAvg().subList( index * audioBufferSize, index * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
				DSP dsp = new DSP();
				dsp.processFull( samplesAvg );
				fftQueue.offer( new PrioritySpectrum( new ArrayList<>( Arrays.asList( Arrays.stream( dsp.getSpectrum() ).boxed().toArray( Double[]::new ) ) ), index ) );
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

		System.out.println( "FFT Queue size: " + fftQueue.size() );

		System.out.println( "Triggering render" );
		Renderer renderer = new Renderer( pool, fileNameList, fftQueue, music, width, height );

		long initialTime = Clock.systemUTC().millis();

		// Render
		renderer.run();

		System.out.println( "Rendering complete" );
		System.out.println( "Rendered " + fileNameList.size() + " frames" );
		System.out.println( "Encoding video" );

		// Encode video
		FFmpeg
			.atPath()
			.addInput( UrlInput.fromPath( inputFile.toPath() ) )
			.addOutput( UrlOutput.toUrl( "output.mp4" ) )
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
			File file = new File( fileName );
			if( file.exists() ) {
				file.delete();
			}
		}

		pool.shutdown();
	}
}