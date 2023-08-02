package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.github.kokorin.jaffree.ffmpeg.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lombok.CustomLog;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@CustomLog
public class HyperSynesthesiaTool extends GuidedTool {

	private static final BufferedImage POISON_PILL = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_RGB );

	private final double MIN_SCALE = 0.05;

	private final double MAX_SCALE = 5.0;

	private final RenderPane displayPane;

	private final Scene renderScene;

	private final RenderPane renderPane;

	// NOTE The buffer size could be computed to be more efficient
	private final BlockingQueue<BufferedImage> frameBuffer;

	private double lastX;

	private double lastY;

	private int dimX = 1920;

	private int dimY = 1080;

	private File inputAudioFile = null;

	Button importButton;

	Button exportButton;

	Long musicDuration = null;

	public HyperSynesthesiaTool( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		frameBuffer = new LinkedBlockingQueue<>( 1000 );

		// Set up the rendering components
		renderPane = new RenderPane();
		renderPane.setMinSize( dimX, dimY );
		renderPane.setPrefSize( dimX, dimY );
		renderPane.setMaxSize( dimX, dimY );
		renderScene = new Scene( renderPane );
		renderScene.setFill( Color.TRANSPARENT );

		// Set up the display components
		displayPane = new RenderPane();
		Label name = new Label( "HyperSynesthesia" );
		name.setStyle( "-fx-font-size: 5cm" + "; -fx-font-weight: bold" + "; -fx-text-fill: #000000;" );
		displayPane.getChildren().add( name );

		displayPane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius 40% , #ffebcd, #008080);" );
		displayPane.setMaxSize( dimX, dimY );
		displayPane.setMinSize( dimX, dimY );

		double DEFAULT_SCALE = 0.5;
		displayPane.setScaleX( DEFAULT_SCALE );
		displayPane.setScaleY( DEFAULT_SCALE );

		Pane container = new Pane( displayPane );
		container.setStyle( "-fx-background-color: #000000;" );

		SplitPane splitPane = new SplitPane();

		VBox right = new VBox();

		HBox fileButtons = new HBox();

		importButton = new Button( "Import" );
		importButton.setOnAction( ( event ) -> loadMusicFile() );

		exportButton = new Button( "Export" );
		exportButton.setOnAction( ( event ) -> exportVideo() );
		//		exportButton.setDisable( true );

		fileButtons.getChildren().addAll( importButton, exportButton );
		right.getChildren().addAll( new Label( "Inspector" ), fileButtons );

		splitPane.getItems().addAll( container, right );
		splitPane.setDividerPositions( 0.8 );

		container.setOnScroll( ( ScrollEvent event ) -> {
			if( Math.abs( event.getDeltaY() ) <= 0.0 ) return;

			double scale = displayPane.getScaleX();
			double oldscale = scale;

			if( event.getDeltaY() > 0 ) {
				scale *= 1.1;
			} else {
				scale /= 1.1;
			}
			scale = clamp( scale, MIN_SCALE, MAX_SCALE );

			double f = (scale / oldscale) - 1;

			double deltaX = (event.getX() - (displayPane.getBoundsInParent().getWidth() / 2 + displayPane.getBoundsInParent().getMinX()));
			double deltaY = (event.getY() - (displayPane.getBoundsInParent().getHeight() / 2 + displayPane.getBoundsInParent().getMinY()));

			displayPane.setScaleX( scale );
			displayPane.setScaleY( scale );

			displayPane.setTranslateX( displayPane.getTranslateX() - f * deltaX );
			displayPane.setTranslateY( displayPane.getTranslateY() - f * deltaY );
		} );

		container.setOnMouseDragEntered( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		container.setOnMouseDragged( ( MouseEvent event ) -> {
			double deltaX = event.getX() - lastX;
			double deltaY = event.getY() - lastY;
			displayPane.setTranslateX( displayPane.getTranslateX() + deltaX );
			displayPane.setTranslateY( displayPane.getTranslateY() + deltaY );
			lastX = event.getX();
			lastY = event.getY();
		} );

		container.setOnMouseMoved( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		this.getChildren().add( splitPane );
	}

	public static double clamp( double value, double min, double max ) {

		if( Double.compare( value, min ) < 0 ) return min;

		if( Double.compare( value, max ) > 0 ) return max;

		return value;
	}

	private FrameProducer fxFrameProducer() {
		return new FrameProducer() {

			private long frameCounter = 0;

			@Override
			public List<Stream> produceStreams() {
				return Collections.singletonList( new Stream().setType( Stream.Type.VIDEO ).setTimebase( 60L ).setWidth( dimX ).setHeight( dimY ) );
			}

			@Override
			public Frame produce() {
				long pts = frameCounter; // Frame PTS in Stream Timebase
				try {
					synchronized( frameBufferMutex ) {
						BufferedImage image = frameBuffer.take();
						if( image == POISON_PILL ) return null;

						Frame videoFrame = Frame.createVideoFrame( 0, pts, image );
						frameCounter++;

						// Notify that there is room on the queue
						frameBufferMutex.notifyAll();

						return videoFrame;
					}
				} catch( InterruptedException exception ) {
					return null;
				}
			}
		};
	}

	private final Object frameBufferMutex = new Object();

	private class FrameRenderer extends Task<Void> {

		private long frameCounter = 0;

		public Void call() {
			final long millisPerFrame = 1000 / 60;

			try {
				while( frameCounter < musicDuration / millisPerFrame ) {
					// Wait for buffer capacity
					synchronized( frameBufferMutex ) {
						while( frameBuffer.remainingCapacity() < 1 ) {
							try {
								frameBufferMutex.wait( 100 );
							} catch( InterruptedException e ) {
								break;
							}
						}
					}

					// Render a frame and put it in the frame buffer
					final long finalFrameCounter = frameCounter;
					Platform.runLater( () -> {
						try {
							frameBuffer.offer( renderBufferedImaged( finalFrameCounter ), 10, TimeUnit.SECONDS );
						} catch( InterruptedException exception ) {
							// Stop rendering when interrupted
						}
					} );

					frameCounter++;
				}
			} finally {
				Platform.runLater( () -> {
					try {
						frameBuffer.offer( POISON_PILL, 10, TimeUnit.SECONDS );
					} catch( InterruptedException e ) {
						// Intentionally ignore exception
					}
				} );
			}

			return null;
		}

	}

	//	private FrameProducer getNewFrameProducer() {
	//		return new FrameProducer() {
	//
	//			private long frameCounter = 0;
	//
	//			@Override
	//			public List<Stream> produceStreams() {
	//				return Collections.singletonList( new Stream().setType( Stream.Type.VIDEO ).setTimebase( 60L ).setWidth( dimX ).setHeight( dimY ) );
	//			}
	//
	//			@Override
	//			public Frame produce() {
	//				final long millisPerFrame = 1000 / 60;
	//
	//				if( frameCounter > musicDuration / millisPerFrame ) {
	//					return null;
	//				}
	//				long pts = frameCounter; // Frame PTS in Stream Timebase
	//				double val = Math.abs( Math.sin( frameCounter / 10.0 ) );
	//				Platform.runLater( () -> {
	//					renderPane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius " + val * 50 + "% , #ffebcd, -fx-accent);" );
	//					renderBufferedImaged( frameCounter );
	//				} );
	//				Frame videoFrame = Frame.createVideoFrame( 0, pts, buffer );
	//				frameCounter++;
	//
	//				return videoFrame;
	//			}
	//		};
	//	}

	private BufferedImage renderBufferedImaged( long frameCounter ) {
		log.atConfig().log( "Rendering frame " + frameCounter );
		double val = Math.abs( Math.sin( frameCounter / 10.0 ) );

		//RenderPane renderPane = new RenderPane();
		//		renderPane.resize( dimX, dimY );
		renderPane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius " + val * 50 + "% , #ffebcd, -fx-accent);" );
		renderPane.setScaleX( 1.0 );
		renderPane.setScaleY( 1.0 );
		//renderPane.applyCss();
		//renderPane.layout();

		WritableImage image = renderPane.snapshot( new SnapshotParameters(), null );
		BufferedImage buffer = new BufferedImage( dimX, dimY, BufferedImage.TYPE_3BYTE_BGR );
		BufferedImage base = SwingFXUtils.fromFXImage( image, null );
		buffer.getGraphics().drawImage( base, 0, 0, null );
		buffer.getGraphics().dispose();

		return buffer;
	}

	private void loadMusicFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Open Music File" );

		inputAudioFile = fileChooser.showOpenDialog( getProgram().getWorkspaceManager().getActiveStage() );

		//		exportButton.setDisable( inputAudioFile == null );

		AtomicLong duration = new AtomicLong( 0 );

		FFmpeg.atPath().addInput( UrlInput.fromPath( inputAudioFile.toPath() ) ).addOutput( new NullOutput() ).setProgressListener( new ProgressListener() {

			@Override
			public void onProgress( FFmpegProgress progress ) {
				duration.set( progress.getTimeMillis() );
			}
		} ).execute();

		musicDuration = duration.get();
	}

	private void exportVideo() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Export Video" );

		File file = fileChooser.showSaveDialog( getProgram().getWorkspaceManager().getActiveStage() );

		if( file == null ) return;

		try {
			if( Files.exists( file.toPath() ) ) Files.delete( file.toPath() );
		} catch( IOException exception ) {
			log.atWarn().log("Unable to overwrite file " + file, exception );
			return;
		}

		getProgram().getTaskManager().submit( new FrameRenderer() );

		if( inputAudioFile != null ) {
			inputAudioFile = new File( inputAudioFile.getPath() );
			FFmpeg.atPath().addInput( FrameInput.withProducer( fxFrameProducer() ) ).addInput( UrlInput.fromPath( inputAudioFile.toPath() ) ).addOutput( UrlOutput.toPath( file.toPath() ) ).executeAsync();
		} else {
			FFmpeg.atPath().addInput( FrameInput.withProducer( fxFrameProducer() ) ).addOutput( UrlOutput.toPath( file.toPath() ) ).executeAsync();
		}
	}

}
