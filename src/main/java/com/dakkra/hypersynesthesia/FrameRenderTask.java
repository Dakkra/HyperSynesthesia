package com.dakkra.hypersynesthesia;

import com.avereon.xenon.task.Task;
import com.github.kokorin.jaffree.ffmpeg.Frame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class FrameRenderTask extends Task<Frame> {

	private static final Font FONT = new Font( "Consolas", Font.PLAIN, 32 );

	private final FFmpegFrameSequencer sequencer;

	private final VideoData videoData;

	private final MusicData musicData;

	private final int index;

	public FrameRenderTask( FFmpegFrameSequencer sequencer, VideoData videoData, MusicData musicData, int index ) {
		this.sequencer = sequencer;
		this.videoData = videoData;
		this.musicData = musicData;
		this.index = index;
	}

	public static Color colorWithIntensity( Color color, double intensity ) {
		return new Color( (int)(color.getRed() * intensity), (int)(color.getGreen() * intensity), (int)(color.getBlue() * intensity), color.getAlpha() );
	}

	@Override
	public Frame call() throws InterruptedException {
		int width = videoData.width();
		int height = videoData.height();

		BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D graphics = image.createGraphics();

		// Render the background
		graphics.setColor( Color.BLACK );
		graphics.fillRect( 0, 0, width, height );

		// Render the frame number
		graphics.setFont( FONT );
		graphics.setColor( Color.WHITE );
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		graphics.drawString( "Frame: " + index, 10, 15 );

		//Render spectrum
		List<Double> spectrum = musicData.getSpectraAverage().get( index );
		int barMaxHeight = height / 2;
		int barCount = spectrum.size();
		for( int i = 0; i < barCount; i++ ) {
			double x = Math.log10( i ) / Math.log10( barCount ) * height;
			double barWidth = Math.log10( i + 1 ) / Math.log10( barCount ) * width - x;
			barWidth = Math.max( 1, barWidth );
			int barHeight = (int)(spectrum.get( i ) * barMaxHeight);
			graphics.setPaint( colorWithIntensity( new Color( 140, 239, 137 ), spectrum.get( i ) ) );
			graphics.fillRect( (int)x, height / 2 - barHeight / 2, (int)barWidth, barHeight );
		}

		//Render loudness
		double loudness = musicData.getLoudnessAverage().get( index );
		int smallest = Math.min( width, height );
		int size = smallest / 10 + (int)(loudness * smallest);
		AffineTransform transform = graphics.getTransform();
		Rectangle2D rect = new Rectangle2D.Double( ((double)width / 2) - size / 2.0f, ((double)height / 2) - size / 2.0f, size, size );
		double rotateX = rect.getX() + rect.getWidth() / 2;
		double rotateY = rect.getY() + rect.getHeight() / 2;
		graphics.rotate( Math.toRadians( index ), rotateX, rotateY );
		graphics.setPaint( colorWithIntensity( new Color( 255, 142, 142, 128 ), loudness ) );
		graphics.fillRect( rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height );
		graphics.setTransform( transform );
		//graphics.drawImage( art, rect.getBounds().x, rect.getBounds().y, rect.getBounds().width, rect.getBounds().height, null );

		// Create the FFmpeg frame
		Frame frame = Frame.createVideoFrame( 0, index, image );
		sequencer.addFrame( frame );
		return frame;
	}

}
