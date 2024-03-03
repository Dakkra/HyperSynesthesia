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

class ColorUtil {

	public static Color colorWithIntensity( Color color, double intensity ) {
		return new Color( (int)(color.getRed() * intensity), (int)(color.getGreen() * intensity), (int)(color.getBlue() * intensity), color.getAlpha() );
	}

}

class DSP {

	private int peakLoudness = 0;

	private double rms = 0;

	private double[] spectrum = null;

	public void process( int[] samples ) {
		for( int sample : samples ) {
			if( Math.abs( sample ) > peakLoudness ) {
				peakLoudness = Math.abs( sample );
			}
		}

		//Calculate RMS
		double sum = 0;
		for( int sample : samples ) {
			sum += ((double)sample * (double)sample);
		}
		rms = Math.sqrt( sum / samples.length );

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

		//Remove negative values
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
	}

}

class SpinningSquare implements FrameProducer {

	static final int VWIDTH = 1920;

	static final int VHEIGHT = 1080;

	private long frameCounter = 0;

	private MusicFile musicFile = null;

	private BufferedImage art = null;

	private static final int RMS_QUEUE_SIZE = 5;

	private DSP dspLeft = new DSP();

	private double progress = 0.0;

	private Queue<Double> rmsQueue = new ArrayDeque<>( RMS_QUEUE_SIZE );

	private Queue<ArrayList<Double>> spectrumsQueue = null;

	public SpinningSquare( MusicFile musicFile ) {

		this.musicFile = musicFile;
		try {
			art = ImageIO.read( Objects.requireNonNull( this.getClass().getResourceAsStream( "/hs-logo.png" ) ) );
		} catch( IOException e ) {
			throw new RuntimeException( e );
		}
		for( int i = 0; i < RMS_QUEUE_SIZE; i++ ) {
			rmsQueue.add( 0.0 );
		}
		progress = 0.0;
	}

	public double getProgress() {
		return progress;
	}

	@Override
	public List<Stream> produceStreams() {
		return Collections.singletonList( new Stream().setType( Stream.Type.VIDEO ).setTimebase( 60L ).setWidth( VWIDTH ).setHeight( VHEIGHT ) );
	}

	@Override
	public Frame produce() {
		int[] samplesLeft;
		int audioBufferSize = (int)(musicFile.getSampleRate() / 60);
		int delta = Math.min( audioBufferSize, musicFile.getSamplesLeft().size() - (int)frameCounter * audioBufferSize );
		progress = frameCounter * audioBufferSize / (double)musicFile.getSamplesLeft().size();
		if( delta <= 0 ) {
			return null;
		}

		// Initial setup
		BufferedImage image = new BufferedImage( VWIDTH, VHEIGHT, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D graphics = image.createGraphics();
		samplesLeft = musicFile.getSamplesLeft().subList( (int)frameCounter * audioBufferSize, (int)frameCounter * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
		dspLeft.process( samplesLeft );
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		graphics.setPaint( new Color( 0, 0, 0 ) );
		graphics.fillRect( 0, 0, VWIDTH, VHEIGHT );

		//Avg spectrum
		double[] newSpectrum = dspLeft.getSpectrum();
		if( spectrumsQueue == null ) {
			spectrumsQueue = new ArrayDeque<>( RMS_QUEUE_SIZE );
			for( int i = 0; i < RMS_QUEUE_SIZE; i++ ) {
				spectrumsQueue.add( new ArrayList<>( Arrays.asList( Arrays.stream( new double[ newSpectrum.length ] ).boxed().toArray( Double[]::new ) ) ) );
			}
		}
		spectrumsQueue.remove();
		spectrumsQueue.add( new ArrayList<>( Arrays.asList( Arrays.stream( newSpectrum ).boxed().toArray( Double[]::new ) ) ) );
		ArrayList<Double> spectrum = new ArrayList<>( newSpectrum.length );
		for( int i = 0; i < newSpectrum.length; i++ ) {
			double sum = 0;
			for( ArrayList<Double> nextSpectrum : spectrumsQueue ) {
				sum += nextSpectrum.get( i ) != null ? nextSpectrum.get( i ) : 0;
			}
			spectrum.add( sum / spectrumsQueue.size() );
		}

		//Render spectrum
		int barMaxHeight = VHEIGHT / 2;
		int barCount = spectrum.size();
		for( int i = 0; i < barCount; i++ ) {
			double x = Math.log10( i ) / Math.log10( barCount ) * VWIDTH;
			double barWidth = Math.log10( i + 1 ) / Math.log10( barCount ) * VWIDTH - x;
			barWidth = Math.max( 1, barWidth );
			int barHeight = (int)(spectrum.get( i ) * barMaxHeight);
			graphics.setPaint( ColorUtil.colorWithIntensity( new Color( 239, 137, 222 ), spectrum.get( i ) ) );
			graphics.fillRect( (int)x, VHEIGHT / 2 - barHeight / 2, (int)barWidth, barHeight );
		}

		// Generate RMS queue
		double intensity = dspLeft.getRMSLoudness();
		double avg = rmsQueue.stream().mapToDouble( Double::doubleValue ).average().orElse( 0.0 );
		rmsQueue.remove();
		rmsQueue.add( intensity );

		int smallest = Math.min( VWIDTH, VHEIGHT );
		int size = smallest / 10 + (int)(avg * smallest);
		AffineTransform transform = graphics.getTransform();
		Rectangle2D rect = new Rectangle2D.Double( ((double)VWIDTH / 2) - size / 2.0f, ((double)VHEIGHT / 2) - size / 2.0f, size, size );
		double rotateX = rect.getX() + rect.getWidth() / 2;
		double rotateY = rect.getY() + rect.getHeight() / 2;
		//		graphics.translate( 100 * avg, 100 * avg );
		//		graphics.rotate( Math.toRadians( frameCounter ), rotateX, rotateY );
		//		graphics.setPaint( new Color( 0, 0, 0, 50 ) );
		//		graphics.fillRect( rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height );
		//		graphics.setTransform( transform );
		graphics.rotate( Math.toRadians( frameCounter ), rotateX, rotateY );
		graphics.setPaint( ColorUtil.colorWithIntensity( new Color( 127, 255, 197, 150 ), avg ) );
		graphics.fillRect( rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height );
		graphics.setTransform( transform );
		graphics.drawImage( art, rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height, null );

		long pts = frameCounter; // Frame PTS in Stream Timebase
		Frame videoFrame = Frame.createVideoFrame( 0, pts, image );
		frameCounter++;

		return videoFrame;
	}

}

class MusicFile {

	private final File file;

	private Vector<Integer> samplesLeft;

	private Vector<Integer> samplesRight;

	private Vector<Integer> samplesAvg;

	private Long sampleRate = 0L;

	public MusicFile( File file ) {
		this.file = file;
	}

	public void load() {
		if( !file.exists() ) {
			throw new RuntimeException( "Input file not found: " + file.getAbsolutePath() );
		}

		samplesLeft = new Vector<>();
		samplesRight = new Vector<>();

		FFmpeg.atPath().addInput( UrlInput.fromPath( file.toPath() ) ).addOutput( FrameOutput.withConsumer( new FrameConsumer() {

			@Override
			public void consumeStreams( List<Stream> streams ) {
				sampleRate = streams.get( 0 ).getSampleRate();
			}

			@Override
			public void consume( Frame frame ) {
				//End of stream
				if( frame == null ) return;

				//Add samples to the sample buffers
				for( int index = 0; index < frame.getSamples().length; index++ ) {
					if( index % 2 == 0 ) {
						samplesRight.add( frame.getSamples()[ index ] );
					} else {
						samplesLeft.add( frame.getSamples()[ index ] );
					}
				}
			}
		} ).disableStream( StreamType.VIDEO ).disableStream( StreamType.DATA ).disableStream( StreamType.SUBTITLE ) ).execute();
	}

	public Vector<Integer> getSamplesLeft() {
		return samplesLeft;
	}

	public Vector<Integer> getSamplesRight() {
		return samplesRight;
	}

	public Long getSampleRate() {
		return sampleRate;
	}

}

public class Main {

	public static void main( String[] args ) {
		Timer timer = new Timer();
		File input = new File( "DeadFriend.wav" );
		if( !input.exists() ) {
			throw new RuntimeException( "Input file not found: " + input.getAbsolutePath() );
		}
		MusicFile musicFile = new MusicFile( input );
		musicFile.load();

		File out = new File( "output.mp4" );
		if( out.exists() ) {
			out.delete();
		}
		SpinningSquare producer = new SpinningSquare( musicFile );

		timer.scheduleAtFixedRate( new TimerTask() {

			@Override
			public void run() {
				DecimalFormat df = new DecimalFormat( "##.##%" );
				double percent = producer.getProgress();
				String formattedPercent = df.format( percent );
				System.out.println( "Progress: " + formattedPercent );
				//				ui.setProgress(formattedPercent);
			}
		}, 0, 5 * 1000 );

		long initialTime = Clock.systemUTC().millis();
		FFmpeg.atPath().addInput( UrlInput.fromPath( input.toPath() ) ).addInput( FrameInput.withProducer( producer ) ).addOutput( UrlOutput.toUrl( "output.mp4" ) ).addArguments( "-crf", "15" ).execute();

		long finalTime = Clock.systemUTC().millis();
		long deltaTime = finalTime - initialTime;

		Duration duration = Duration.ofMillis( deltaTime );
		System.out.println( "Encoding took: " + duration.toMinutesPart() + " minutes and " + duration.toSecondsPart() + " seconds" );
		timer.cancel();
		timer.purge();
	}

}
