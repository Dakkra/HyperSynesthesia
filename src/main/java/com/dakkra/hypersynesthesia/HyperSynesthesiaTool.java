package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.resource.Resource;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.dakkra.hypersynesthesia.ffmpeg.MusicFile;
import com.dakkra.hypersynesthesia.ffmpeg.ProjectProcessor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
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
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@CustomLog
public class HyperSynesthesiaTool extends GuidedTool {

	private static final double MIN_SCALE = 0.05;

	private static final double MAX_SCALE = 5.0;

	private final ProjectProcessor projectProcessor;

	private final RenderPane displayPane;

	private final Scene renderScene;

	private final RenderPane renderPane;

	// NOTE The buffer size could be computed to be more efficient
	private final BlockingQueue<BufferedImage> frameBuffer;

	private double lastX;

	private double lastY;

	private int width = 1920;

	private int height = 1080;

	private MusicFile music;

	private Button importButton;

	private Button exportButton;

	//	private final Object frameBufferMutex = new Object();
	//
	//	Long musicDurationMS = null;
	//
	//	private Vector<Integer> samples_left;
	//
	//	private Vector<Integer> samples_right;
	//
	//	private long samplerate;

	public HyperSynesthesiaTool( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

		this.projectProcessor = new ProjectProcessor( product.getProgram() );

		frameBuffer = new LinkedBlockingQueue<>( 100 );

		// Set up the rendering components
		renderPane = new RenderPane();
		Label name = new Label( "HyperSynesthesia" );
		name.setStyle( "-fx-font-size: 100px" + "; -fx-font-weight: bold" + "; -fx-text-fill: #000000; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 10, 10);" );
		renderPane.setMinSize( width, height );
		renderPane.setPrefSize( width, height );
		renderPane.setMaxSize( width, height );
		renderPane.setScaleX( 1.0 );
		renderPane.setScaleY( 1.0 );
		renderScene = new Scene( renderPane );
		renderScene.setFill( Color.TRANSPARENT );
		renderPane.getChildren().add( name );

		// Set up the display components
		displayPane = new RenderPane();
		name.setStyle( "-fx-font-size: 5cm" + "; -fx-font-weight: bold" + "; -fx-text-fill: #000000; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 10, 10);" );
		displayPane.getChildren().add( name );

		displayPane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius 40% , #ffebcd, #008080);" );
		displayPane.setMaxSize( width, height );
		displayPane.setMinSize( width, height );

		double DEFAULT_SCALE = 0.5;
		displayPane.setScaleX( DEFAULT_SCALE );
		displayPane.setScaleY( DEFAULT_SCALE );

		Pane container = new Pane( displayPane );
		container.setStyle( "-fx-background-color: #000000;" );

		SplitPane splitPane = new SplitPane();

		VBox right = new VBox();

		HBox fileButtons = new HBox();

		// NOTE The process is split into two steps
		//  1. Load and analyze the music file
		//  2. Generate the video file
		importButton = new Button( "Import" );
		importButton.setOnAction( ( event ) -> requestInputFile() );

		exportButton = new Button( "Export" );
		exportButton.setOnAction( ( event ) -> requestOutputFile() );
		exportButton.setDisable( true );

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

	private void requestInputFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Open Music File" );
		fileChooser.setInitialDirectory( new File( System.getProperty( "user.home" ), "Music" ) );
		Path inputFile = fileChooser.showOpenDialog( getProgram().getWorkspaceManager().getActiveStage() ).toPath();

		Task<Void> loadTask = Task.of( "Load Music", () -> {
			music = projectProcessor.loadMusicFile( inputFile );
			// TODO Update actions
			exportButton.setDisable( false );
			return null;
		} );
		getProgram().getTaskManager().submit( loadTask );
	}

	private void requestOutputFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Export Video" );
		fileChooser.setInitialDirectory( new File( System.getProperty( "user.home" ), "Videos" ) );
		Path outputFile = fileChooser.showSaveDialog( getProgram().getWorkspaceManager().getActiveStage() ).toPath();

		Task<?> renderTask = Task.of( "Render Video", () -> projectProcessor.renderVideoFile( music, width, height, outputFile ) );
		getProgram().getTaskManager().submit( renderTask );

		// TODO Update actions
	}

}
