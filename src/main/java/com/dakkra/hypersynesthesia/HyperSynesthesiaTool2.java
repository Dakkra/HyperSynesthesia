package com.dakkra.hypersynesthesia;

import com.avereon.product.Rb;
import com.avereon.xenon.UiFactory;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.resource.Resource;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.avereon.zerra.Option;
import com.avereon.zerra.javafx.Fx;
import com.dakkra.hypersynesthesia.ffmpeg.MusicFile;
import com.dakkra.hypersynesthesia.ffmpeg.ProjectProcessor;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class HyperSynesthesiaTool2 extends GuidedTool {

	private static final int DEFAULT_WIDTH = 1920;

	private static final int DEFAULT_HEIGHT = 1080;

	private static final int DEFAULT_FRAME_RATE = 60;

	private static final Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;

	private static final Color DEFAULT_BAR_COLOR = Color.WHITE;

	private static final String BUNDLE = "tool";

	private final ProjectProcessor projectProcessor;

	// Source audio properties
	private final TextField sourceAudio;

	private final TextField sampleCount;

	private final TextField fftCount;

	private final ProgressBar audioProgressBar;

	// Video properties
	private final TextField width;

	private final TextField height;

	private final TextField frameRate;

	// Background options
	private final ColorPicker backgroundPaint;

	private final TextField backgroundImage;

	// Target file properties
	private final ComboBox<Option<OutputFormat>> outputFormat;

	private final TextField targetVideo;

	// Bar options
	private final ComboBox<Option<BarStyle>> barStyle;

	private final ColorPicker barPaint;

	private final Button executeButton;

	private MusicFile music;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

		projectProcessor = new ProjectProcessor( product.getProgram() );

		sourceAudio = new TextField();
		sampleCount = new TextField();
		fftCount = new TextField();
		audioProgressBar = new ProgressBar( 0 );

		width = new TextField( String.valueOf( DEFAULT_WIDTH ) );
		height = new TextField( String.valueOf( DEFAULT_HEIGHT ) );
		frameRate = new TextField( String.valueOf( DEFAULT_FRAME_RATE ) );

		backgroundPaint = new ColorPicker( DEFAULT_BACKGROUND_COLOR );
		backgroundImage = new TextField();

		outputFormat = new ComboBox<>( FXCollections.observableList( Option.of( product, BUNDLE, OutputFormat.values() ) ) );
		outputFormat.getSelectionModel().selectFirst();
		targetVideo = new TextField();

		barStyle = new ComboBox<>( FXCollections.observableList( Option.of( product, BUNDLE, BarStyle.values() ) ) );
		barStyle.getSelectionModel().selectFirst();
		barPaint = new ColorPicker( DEFAULT_BAR_COLOR );

		executeButton = new Button( Rb.text( getProduct(), BUNDLE, "generate" ) );
		GridPane.setHgrow( executeButton, javafx.scene.layout.Priority.ALWAYS );
		executeButton.setMaxWidth( Double.MAX_VALUE );
		executeButton.setDisable( true );

		GridPane grid = new GridPane();
		StackPane.setMargin( grid, new Insets( 10 ) );
		grid.setHgap( 10 );
		grid.setVgap( 10 );

		grid.add( createAudioSourcePane(), 0, 0, 2, 1 );
		grid.add( createVideoPropertiesPane(), 0, 1, 1, 1 );
		grid.add( createBackgroundOptionsPane(), 1, 1, 1, 1 );
		grid.add( createSourceTargetPane(), 0, 2, 1, 1 );
		grid.add( createBarOptionsPane(), 1, 2, 1, 1 );
		grid.add( executeButton, 0, 3, 2, 1 );

		getChildren().addAll( grid );

		executeButton.setOnAction( event -> {
			execute();
		} );
	}

	private void requestBackgroundImage() {

	}

	private void requestSourceAudioFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( Rb.text( getProduct(), BUNDLE, "source-audio-prompt" ) );
		fileChooser.setInitialDirectory( new File( System.getProperty( "user.home" ), "Music" ) );
		Path inputFile = fileChooser.showOpenDialog( getProgram().getWorkspaceManager().getActiveStage() ).toPath();

		sourceAudio.setText( inputFile.toString() );
		audioProgressBar.setProgress( ProgressIndicator.INDETERMINATE_PROGRESS );

		Task<Void> loadTask = Task.of(
			"Load Music", () -> {
				music = projectProcessor.loadMusicFile(
					inputFile, sampleCount -> {
						Fx.run( () -> this.sampleCount.setText( String.valueOf( sampleCount ) ) );
					}, fftCount -> {
						Fx.run( () -> this.fftCount.setText( String.valueOf( fftCount ) ) );
					}, progress -> {
						Fx.run( () -> audioProgressBar.setProgress( progress ) );
					}
				);
				return null;
			}
		);
		getProgram().getTaskManager().submit( loadTask );
	}

	private void requestTargetVideoFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( Rb.text( getProduct(), BUNDLE, "target-video-prompt" ) );
		fileChooser.setInitialDirectory( new File( System.getProperty( "user.home" ), "Videos" ) );
		Path outputFile = fileChooser.showSaveDialog( getProgram().getWorkspaceManager().getActiveStage() ).toPath();

		targetVideo.setText( outputFile.toString() );
		executeButton.setDisable( false );
	}

	private void execute() {
		int width = Integer.parseInt( this.width.getText() );
		int height = Integer.parseInt( this.height.getText() );

		Path outputPath = Path.of( targetVideo.getText() );
		Task<?> renderTask = Task.of( "Render Video", () -> {
			projectProcessor.renderVideoFile( music, width, height, outputPath );

//			Triggering render
//			Rendering complete
//			Rendered 5009 frames
//			Encoding video
//			Input file duration: 83 seconds
//			Target video resolution: 1920x1080
//			Render and encoding took: 0 minutes and 11 seconds
//			Render and encoding was 754.55% of real time

		} );
		getProgram().getTaskManager().submit( renderTask );
	}

	private TitledPane createAudioSourcePane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label sourceAudioPrompt = new Label( Rb.text( getProduct(), BUNDLE, "source-path-prompt" ) );
		Node sourceAudioFileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button sourceAudioButton = new Button( null, sourceAudioFileIcon );
		Label sampleCountPrompt = new Label( Rb.text( getProduct(), BUNDLE, "sample-count-prompt" ) );
		Label fftCountPrompt = new Label( Rb.text( getProduct(), BUNDLE, "fft-count-prompt" ) );
		grid.add( sourceAudioPrompt, 0, 0, 1, 1 );
		grid.add( sourceAudio, 1, 0, 3, 1 );
		grid.add( sourceAudioButton, 4, 0, 1, 1 );

		grid.add( sampleCountPrompt, 0, 1, 1, 1 );
		grid.add( sampleCount, 1, 1, 1, 1 );
		grid.add( fftCountPrompt, 2, 1, 1, 1 );
		grid.add( fftCount, 3, 1, 1, 1 );

		grid.add( audioProgressBar, 0, 2, 5, 1 );
		audioProgressBar.setMaxWidth( Double.MAX_VALUE );

		sampleCount.setAlignment( Pos.BASELINE_RIGHT );
		sampleCount.setEditable( false );
		fftCount.setAlignment( Pos.BASELINE_RIGHT );
		fftCount.setEditable( false );

		//GridPane.setHgrow( sourceAudioPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( sourceAudio, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( sampleCount, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( fftCount, javafx.scene.layout.Priority.ALWAYS );

		sourceAudioButton.setOnAction( event -> requestSourceAudioFile() );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "input-and-output-files-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createVideoPropertiesPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label widthLabel = new Label( Rb.text( getProduct(), BUNDLE, "video-width-prompt" ) );
		Label heightLabel = new Label( Rb.text( getProduct(), BUNDLE, "video-height-prompt" ) );
		Label frameRateLabel = new Label( Rb.text( getProduct(), BUNDLE, "video-frame-rate-prompt" ) );

		grid.add( widthLabel, 0, 0 );
		grid.add( width, 1, 0 );
		grid.add( heightLabel, 0, 1 );
		grid.add( height, 1, 1 );
		grid.add( frameRateLabel, 0, 2 );
		grid.add( frameRate, 1, 2 );

		GridPane.setHgrow( widthLabel, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( heightLabel, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( frameRateLabel, javafx.scene.layout.Priority.ALWAYS );

		width.setAlignment( Pos.BASELINE_RIGHT );
		width.setPromptText( String.valueOf( DEFAULT_WIDTH ) );

		height.setAlignment( Pos.BASELINE_RIGHT );
		height.setPromptText( String.valueOf( DEFAULT_HEIGHT ) );

		frameRate.setAlignment( Pos.BASELINE_RIGHT );
		frameRate.setPromptText( String.valueOf( DEFAULT_FRAME_RATE ) );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "video-properties-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBackgroundOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label backgroundPaintPrompt = new Label( Rb.text( getProduct(), BUNDLE, "background-color-prompt" ) );
		Label backgroundImagePrompt = new Label( Rb.text( getProduct(), BUNDLE, "background-image-prompt" ) );

		Node fileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button backgroundImageButton = new Button( null, fileIcon );

		grid.add( backgroundPaintPrompt, 0, 1 );
		grid.add( backgroundPaint, 1, 1 );
		grid.add( backgroundImagePrompt, 0, 2 );
		grid.add( backgroundImage, 1, 2 );
		grid.add( backgroundImageButton, 2, 2 );

		GridPane.setHgrow( backgroundPaintPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setColumnSpan( backgroundPaint, 2 );
		backgroundPaint.setMaxWidth( Double.MAX_VALUE );
		GridPane.setHgrow( backgroundImagePrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( backgroundImage, javafx.scene.layout.Priority.ALWAYS );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "background-options-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createSourceTargetPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label outputFormatPrompt = new Label( Rb.text( getProduct(), BUNDLE, "target-format-prompt" ) );
		Label targetVideoPrompt = new Label( Rb.text( getProduct(), BUNDLE, "target-path-prompt" ) );

		Node targetVideoFileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button targetVideoButton = new Button( null, targetVideoFileIcon );

		grid.add( outputFormatPrompt, 0, 1 );
		grid.add( outputFormat, 1, 1 );
		grid.add( targetVideoPrompt, 0, 2 );
		grid.add( targetVideo, 1, 2 );
		grid.add( targetVideoButton, 2, 2 );

		GridPane.setHgrow( outputFormatPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setColumnSpan( outputFormat, 2 );
		outputFormat.setMaxWidth( Double.MAX_VALUE );
		GridPane.setHgrow( targetVideoPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( targetVideo, javafx.scene.layout.Priority.ALWAYS );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "input-and-output-files-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		targetVideoButton.setOnAction( event -> requestTargetVideoFile() );

		return pane;
	}

	private TitledPane createBarOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label barStylePrompt = new Label( Rb.text( getProduct(), BUNDLE, "bar-style-prompt" ) );
		Label barColorPrompt = new Label( Rb.text( getProduct(), BUNDLE, "bar-color-prompt" ) );

		grid.add( barStylePrompt, 0, 0 );
		grid.add( barStyle, 1, 0 );
		grid.add( barColorPrompt, 0, 1 );
		grid.add( barPaint, 1, 1 );

		GridPane.setHgrow( barStylePrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( barColorPrompt, javafx.scene.layout.Priority.ALWAYS );

		barStyle.setMaxWidth( Double.MAX_VALUE );
		barPaint.setMaxWidth( Double.MAX_VALUE );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "bar-customization-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

}
