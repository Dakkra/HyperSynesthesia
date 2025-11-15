package com.dakkra.hypersynesthesia.ffmpeg;

import com.avereon.util.FileUtil;
import com.avereon.xenon.Xenon;
import com.avereon.xenon.task.Task;
import com.avereon.zerra.color.Colors;
import com.dakkra.hypersynesthesia.OutputFormat;
import lombok.CustomLog;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@CustomLog
public class FrameRenderer {

	public static final int AVG_QUEUE_SIZE = 5;

	private final Xenon program;

	private final List<Path> nameList;

	private final MusicFile music;

	private final RenderSettings settings;

	private final Queue<Double> rmsQueue;

	private final PriorityBlockingQueue<PrioritySpectrum> spectralQueue;

	private Queue<ArrayList<Double>> spectrumsQueue;

	private final Consumer<Double> progressConsumer;

	@Getter
	private int frameCount;

	FrameRenderer( Xenon program, List<Path> nameList, MusicFile music, RenderSettings renderSettings, Consumer<Double> progressConsumer ) {
		this.program = program;
		this.nameList = nameList;
		this.music = music;

		this.settings = renderSettings;

		this.rmsQueue = new ArrayBlockingQueue<>( AVG_QUEUE_SIZE );
		this.spectralQueue = new PriorityBlockingQueue<>( music.getFftQueue() );
		this.progressConsumer = progressConsumer;
	}

	public void run() {
		final int audioBufferSize = music.getSampleRate() / 60;
		final int numFrames = spectralQueue.size();
		frameCount = numFrames;
		Vector<Future<?>> futures = new Vector<>();

		System.out.println( "Generate background" );
		// Generate background image
		// The background image is generated here, only one time, for performance
		// reasons. This image is then passed each time a frame is rendered.
		BufferedImage tempBackground = new BufferedImage( settings.width(), settings.height(), BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D graphics = tempBackground.createGraphics();
		graphics.setColor( Colors.asAwtColor( settings.backgroundColor() ) );
		graphics.fillRect( 0, 0, settings.width(), settings.height() );
		try {
			// Load custom background image
			BufferedImage tempBackgroundImage = ImageIO.read( new FileInputStream( settings.backgroundImage().toFile() ) );
			graphics.drawImage( tempBackgroundImage, 0, 0, settings.width(), settings.height(), null );
		} catch( IOException ignore ) {}
		final BufferedImage background = tempBackground;

		// Reinitialize the RMS queue
		rmsQueue.clear();
		IntStream.of( AVG_QUEUE_SIZE ).forEach( _ -> rmsQueue.add( 0.0 ) );

		// Determine the target frame path
		Path targetPath = settings.targetPath();
		OutputFormat format = settings.outputFormat();
		if( format == OutputFormat.MP4 ) {
			if( !targetPath.getFileName().toString().endsWith( ".mp4" ) ) {
				targetPath = targetPath.resolveSibling( targetPath.getFileName() + ".mp4" );
			}
			try {
				Files.createDirectories( targetPath.getParent() );
			} catch( IOException exception ) {
				throw new RuntimeException( exception );
			}
		} else {
			try {
				Files.createDirectories( targetPath );
			} catch( IOException exception ) {
				throw new RuntimeException( exception );
			}
		}

		// Determine the frame prefix from the source audio name
		String framePrefix = FileUtil.removeExtension( settings.sourcePath().getFileName().toString() );

		System.out.println( "Rendering frames: " + numFrames );
		for( int counter = 0; counter < numFrames; counter++ ) {
			DSP dsp = new DSP();
			int delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - counter * audioBufferSize );
			if( delta <= 0 ) {
				return;
			}
			int[] samplesAvg = music.getSamplesAvg().subList( counter * audioBufferSize, counter * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
			dsp.processLight( samplesAvg );

			// Smooth RMS
			double intensity = dsp.getRMSLoudness();
			double loudnessAvg = rmsQueue.stream().mapToDouble( Double::doubleValue ).average().orElse( 0.0 );
			rmsQueue.remove();
			rmsQueue.offer( intensity );

			// Smooth spectrum
			double[] newSpectrum;
			try {
				newSpectrum = spectralQueue.take().getSpectrum().stream().mapToDouble( i -> i ).toArray();
			} catch( InterruptedException exception ) {
				throw new RuntimeException( exception );
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

			final Path finalTargetPath = targetPath;
			Future<?> future = program.getTaskManager().submit( Task.of( () -> {
				Path file = finalTargetPath.resolve( framePrefix + index + ".jpg" );
				try {
					ImageIO.write( renderFrame( index, settings, background, loudnessAvg, new ArrayList<>( spectrumAvg ) ).internalImage(), "jpg", file.toFile() );
					nameList.add( file.getFileName() );
				} catch( IOException e ) {
					e.printStackTrace( System.err );
					throw new RuntimeException( e );
				}
			} ) );
			futures.add( future );
		}

		// Wait for all frames to render
		int count = 0;
		for( Future<?> future : futures ) {
			try {
				future.get();
				this.progressConsumer.accept( (double)++count / (double)numFrames );
			} catch( InterruptedException | ExecutionException e ) {
				log.atError().withCause( e ).log( "Failed to render frame" );
			}
		}
	}

	private static IndexedImage renderFrame( long frameIndex, RenderSettings settings, BufferedImage backgroundImage, double loudness, ArrayList<Double> spectrum ) {
		int frameWidth = settings.width();
		int frameHeight = settings.height();
		BufferedImage frame = new BufferedImage( frameWidth, frameHeight, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D graphics = frame.createGraphics();

		// Blit the background
		graphics.drawImage( backgroundImage, 0, 0, frameWidth, frameHeight, null );

		// Set rendering hints
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		// Bar color
		java.awt.Color barColor = Colors.asAwtColor( settings.barColor() );

		// Render spectrum
		double barWidth = (double)frameWidth / (double)spectrum.size();
		double barCenter = (double)frameHeight / 2.0;
		for( int i = 0; i < spectrum.size(); i++ ) {
			double x = barWidth * i;
			double barHeight = spectrum.get( i ) * (frameHeight / 2.0);
			graphics.setColor( ColorUtil.colorWithIntensity( barColor, spectrum.get( i ) ) );
			graphics.fillRect( (int)x, (int)(barCenter - (0.5 * barHeight)), (int)barWidth, (int)(barCenter * spectrum.get( i )) );
		}

		return new IndexedImage( frame, frameIndex );
	}

	private static IndexedImage pocRenderImage( long frameIndex, int frameWidth, int frameHeight, BufferedImage backgroundImage, double loudness, ArrayList<Double> spectrum ) {
		BufferedImage image = new BufferedImage( frameWidth, frameHeight, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D graphics = image.createGraphics();
		graphics.setColor( Color.BLACK );
		graphics.fillRect( 0, 0, frameWidth, frameHeight );
		graphics.setColor( Color.WHITE );
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		Font font = new Font( "Serif", Font.PLAIN, 32 );
		graphics.setFont( font );
		graphics.drawString( "Frame: " + frameIndex, 10, 20 );

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
		graphics.rotate( Math.toRadians( frameIndex ), rotateX, rotateY );
		graphics.setPaint( ColorUtil.colorWithIntensity( new Color( 127, 255, 197, 150 ), loudness ) );
		graphics.fillRect( rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height );
		graphics.setTransform( transform );
		graphics.drawImage( backgroundImage, rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height, null );

		return new IndexedImage( image, frameIndex );
	}

}
