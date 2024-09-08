package com.dakkra.hypersynesthesia.ffmpeg;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;

class Renderer {

	public BufferedImage art;

	public static final int AVG_QUEUE_SIZE = 5;

	private final ThreadPoolExecutor renderPool;

	private final MusicFile music;

	private final int frameWidth;

	private final int frameHeight;

	private final Queue<Double> rmsQueue;

	private final PriorityBlockingQueue<PrioritySpectrum> spectralQueue;

	private final Vector<String> nameList;

	private Queue<ArrayList<Double>> spectrumsQueue;

	Renderer( ThreadPoolExecutor renderPool, Vector<String> nameList, MusicFile music, int frameWidth, int frameHeight ) {
		this.renderPool = renderPool;
		this.nameList = nameList;
		this.music = music;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.spectralQueue = music.getFftQueue();
		this.rmsQueue = new ArrayBlockingQueue<>( AVG_QUEUE_SIZE );

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
