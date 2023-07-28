package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.github.kokorin.jaffree.ffmpeg.*;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.stage.FileChooser;
import lombok.CustomLog;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;

@CustomLog
public class HyperSynesthesiaTool extends GuidedTool {

	private final double MIN_SCALE = 0.05;

	private final double MAX_SCALE = 5.0;

	private final Pane renderPane;

	private double lastX;

	private double lastY;

	private int dimX = 1920;

	private int dimY = 1080;

	private FrameProducer frameProducer;

	private BufferedImage buffer;

	private File inputAudioFile = null;

	public HyperSynesthesiaTool( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		renderPane = new Pane();
		Label name = new Label( "HyperSynesthesia" );
		name.setStyle( "-fx-font-size: 5cm" + "; -fx-font-weight: bold" + "; -fx-text-fill: #000000;" );
		renderPane.getChildren().add( name );

		renderPane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius 40% , #ffebcd, #008080);" );
		renderPane.setMaxSize( dimX, dimY );
		renderPane.setMinSize( dimX, dimY );

		double DEFAULT_SCALE = 0.5;
		renderPane.setScaleX( DEFAULT_SCALE );
		renderPane.setScaleY( DEFAULT_SCALE );

		Pane container = new Pane( renderPane );
		container.setStyle( "-fx-background-color: #000000;" );

		SplitPane splitPane = new SplitPane();

		VBox right = new VBox();

		HBox fileButtons = new HBox();

		Button importButton = new Button( "Import" );
		importButton.setOnAction( ( event ) -> loadMusicFile() );

		Button exportButton = new Button( "Export" );
		exportButton.setOnAction( ( event ) -> exportVideo() );

		fileButtons.getChildren().addAll( importButton, exportButton );
		right.getChildren().addAll( new Label( "Inspector" ), fileButtons );

		splitPane.getItems().addAll( container, right );
		splitPane.setDividerPositions( 0.8 );

		container.setOnScroll( ( ScrollEvent event ) -> {
			if( Math.abs( event.getDeltaY() ) <= 0.0 ) return;

			double scale = renderPane.getScaleX();
			double oldscale = scale;

			if( event.getDeltaY() > 0 ) {
				scale *= 1.1;
			} else {
				scale /= 1.1;
			}
			scale = clamp( scale, MIN_SCALE, MAX_SCALE );

			double f = (scale / oldscale) - 1;

			double deltaX = (event.getX() - (renderPane.getBoundsInParent().getWidth() / 2 + renderPane.getBoundsInParent().getMinX()));
			double deltaY = (event.getY() - (renderPane.getBoundsInParent().getHeight() / 2 + renderPane.getBoundsInParent().getMinY()));

			renderPane.setScaleX( scale );
			renderPane.setScaleY( scale );

			renderPane.setTranslateX( renderPane.getTranslateX() - f * deltaX );
			renderPane.setTranslateY( renderPane.getTranslateY() - f * deltaY );
		} );

		container.setOnMouseDragEntered( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		container.setOnMouseDragged( ( MouseEvent event ) -> {
			double deltaX = event.getX() - lastX;
			double deltaY = event.getY() - lastY;
			renderPane.setTranslateX( renderPane.getTranslateX() + deltaX );
			renderPane.setTranslateY( renderPane.getTranslateY() + deltaY );
			lastX = event.getX();
			lastY = event.getY();
		} );

		container.setOnMouseMoved( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		this.getChildren().add( splitPane );

		frameProducer = new FrameProducer() {

			private long frameCounter = 0;

			@Override
			public List<Stream> produceStreams() {
				return Collections.singletonList( new Stream().setType( Stream.Type.VIDEO ).setTimebase( 1000L ).setWidth( dimX ).setHeight( dimY ) );
			}

			@Override
			public Frame produce() {
				if( frameCounter > 30 ) {
					return null; // return null when End of Stream is reached
				}
				long pts = frameCounter * 1000 / 10; // Frame PTS in Stream Timebase
				Frame videoFrame = Frame.createVideoFrame( 0, pts, buffer );
				frameCounter++;

				return videoFrame;
			}
		};
	}

	public static double clamp( double value, double min, double max ) {

		if( Double.compare( value, min ) < 0 ) return min;

		if( Double.compare( value, max ) > 0 ) return max;

		return value;
	}

	private void renderBufferedImaged() {
		renderPane.setScaleX( 1.0 );
		renderPane.setScaleY( 1.0 );
		WritableImage image = renderPane.snapshot( new SnapshotParameters(), null );
		buffer = new BufferedImage( dimX, dimY, BufferedImage.TYPE_3BYTE_BGR );
		BufferedImage base = SwingFXUtils.fromFXImage( image, null );
		buffer.getGraphics().drawImage( base, 0, 0, null );
		buffer.getGraphics().dispose();
	}

	private void loadMusicFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Open Music File" );

		inputAudioFile = fileChooser.showOpenDialog( getProgram().getWorkspaceManager().getActiveStage() );
	}

	private void exportVideo() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Export Video" );

		renderBufferedImaged();

		File file = fileChooser.showSaveDialog( getProgram().getWorkspaceManager().getActiveStage() );

		if( inputAudioFile != null ) {
			FFmpeg.atPath().addInput( FrameInput.withProducer( frameProducer ) ).addInput( UrlInput.fromPath( inputAudioFile.toPath() ) ).addOutput( UrlOutput.toPath( file.toPath() ) ).execute();
		} else {
			FFmpeg.atPath().addInput( FrameInput.withProducer( frameProducer ) ).addOutput( UrlOutput.toPath( file.toPath() ) ).execute();
		}

	}

}