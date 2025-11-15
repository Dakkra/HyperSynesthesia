package com.dakkra.hypersynesthesia;

import com.avereon.product.Rb;
import com.avereon.xenon.UiFactory;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.resource.Resource;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.avereon.zerra.Option;
import com.avereon.zerra.javafx.Fx;
import com.dakkra.hypersynesthesia.ffmpeg.FrameRenderer;
import com.dakkra.hypersynesthesia.ffmpeg.MusicFile;
import com.dakkra.hypersynesthesia.ffmpeg.ProjectProcessor;
import com.dakkra.hypersynesthesia.ffmpeg.RenderSettings;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

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

	private final TextField sourceAudioDuration;

	private final TextField sampleCount;

	private final TextField fftCount;

	private final ProgressBar audioProgressBar;

	// Video properties
	private final TextField width;

	private final TextField height;

	private final TextField frameRate;

	// Background options
	private final ColorPicker backgroundColor;

	private final TextField backgroundImage;

	// Target file properties
	private final ComboBox<Option<OutputFormat>> outputFormat;

	private final TextField targetVideo;

	// Bar options
	private final ComboBox<Option<BarStyle>> barStyle;

	private final ColorPicker barColor;

	private final Button executeButton;

	private final ProgressBar videoProgressBar;

	private final TextField videoFrames;

	private final TextField videoResolution;

	private final TextField renderDuration;

	private final TextField renderEfficiency;

	private MusicFile music;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

		projectProcessor = new ProjectProcessor( product );

		sourceAudio = new TextField();
		sourceAudioDuration = new TextField();
		sampleCount = new TextField();
		fftCount = new TextField();
		audioProgressBar = new ProgressBar( 0 );

		width = new TextField( String.valueOf( DEFAULT_WIDTH ) );
		height = new TextField( String.valueOf( DEFAULT_HEIGHT ) );
		frameRate = new TextField( String.valueOf( DEFAULT_FRAME_RATE ) );

		backgroundColor = new ColorPicker( DEFAULT_BACKGROUND_COLOR );
		backgroundImage = new TextField();

		outputFormat = new ComboBox<>( FXCollections.observableList( Option.of( product, BUNDLE, OutputFormat.values() ) ) );
		outputFormat.getSelectionModel().selectFirst();
		targetVideo = new TextField();

		barStyle = new ComboBox<>( FXCollections.observableList( Option.of( product, BUNDLE, BarStyle.values() ) ) );
		barStyle.getSelectionModel().selectFirst();
		barColor = new ColorPicker( DEFAULT_BAR_COLOR );

		executeButton = new Button( Rb.text( getProduct(), BUNDLE, "generate" ) );
		videoProgressBar = new ProgressBar( 0 );
		videoFrames = new TextField();
		videoResolution = new TextField();
		renderDuration = new TextField();
		renderEfficiency = new TextField();

		GridPane grid = new GridPane();
		StackPane.setMargin( grid, new Insets( 10 ) );
		grid.setHgap( 10 );
		grid.setVgap( 10 );

		grid.add( createAudioSourcePane(), 0, 0, 2, 1 );
		grid.add( createVideoPropertiesPane(), 0, 1, 1, 1 );
		grid.add( createBackgroundOptionsPane(), 1, 1, 1, 1 );
		grid.add( createTargetVideoPane(), 0, 2, 1, 1 );
		grid.add( createBarOptionsPane(), 1, 2, 1, 1 );
		grid.add( createGenerateVideoPane(), 0, 3, 2, 1 );

		getChildren().addAll( grid );

		executeButton.setOnAction( event -> {
			execute();
		} );
	}

	private void requestBackgroundImage() {

	}

	private void requestSourceAudioFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( Rb.text( getProduct(), BUNDLE, "source-path-title" ) );
		fileChooser.setInitialDirectory( new File( System.getProperty( "user.home" ), "Music" ) );
		File inputFile = fileChooser.showOpenDialog( getProgram().getWorkspaceManager().getActiveStage() );
		if( inputFile == null ) return;

		sourceAudio.setText( inputFile.toString() );
		audioProgressBar.setProgress( ProgressIndicator.INDETERMINATE_PROGRESS );

		Task<Void> loadTask = Task.of(
			"Load Music", () -> {
				this.music = projectProcessor.loadMusicFile(
					inputFile.toPath(), progress -> {
						Fx.run( () -> audioProgressBar.setProgress( progress ) );
					}
				);

				Fx.run( () -> this.sourceAudioDuration.setText( formatDuration( music.getDuration() ) ) );
				Fx.run( () -> this.sampleCount.setText( String.valueOf( music.getNumSamples() ) ) );
				Fx.run( () -> this.fftCount.setText( String.valueOf( music.getFftQueue().size() ) ) );

				updateActions();
				return null;
			}
		);
		getProgram().getTaskManager().submit( loadTask );
	}

	private void requestTargetVideoFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( Rb.text( getProduct(), BUNDLE, "target-path-title" ) );
		fileChooser.setInitialDirectory( new File( System.getProperty( "user.home" ), "Videos" ) );
		File outputFile = fileChooser.showSaveDialog( getProgram().getWorkspaceManager().getActiveStage() );
		if( outputFile == null ) return;

		targetVideo.setText( outputFile.toString() );
		updateActions();
	}

	private void execute() {
		int width = Integer.parseInt( this.width.getText() );
		int height = Integer.parseInt( this.height.getText() );
		int frameRate = Integer.parseInt( this.frameRate.getText() );
		Color backgroundPaint = this.backgroundColor.getValue();
		Path backgroundImage = Path.of( this.backgroundImage.getText() );
		BarStyle barStyle = this.barStyle.getSelectionModel().getSelectedItem().key();
		Color barPaint = this.barColor.getValue();

		Path outputPath = Path.of( this.targetVideo.getText() );
		OutputFormat outputFormat = this.outputFormat.getSelectionModel().getSelectedItem().key();

		Fx.run( () -> {
			videoProgressBar.setProgress( ProgressIndicator.INDETERMINATE_PROGRESS );
			renderEfficiency.setText( null );
			videoFrames.setText( null );
			videoResolution.setText( null );
			renderDuration.setText( null );
		} );

		String taskName = Rb.text( getProduct(), BUNDLE, "generate-video-title" );
		Task<?> renderTask = Task.of(
			taskName, () -> {
				RenderSettings settings = new RenderSettings()
					.width( width )
					.height( height )
					.frameRate( frameRate )
					.backgroundColor( backgroundPaint )
					.backgroundImage( backgroundImage )
					.barStyle( barStyle )
					.barColor( barPaint )
					.targetPath( outputPath )
					.outputFormat( outputFormat );
				FrameRenderer frameRenderer = projectProcessor.renderVideoFile(
					music,
					settings,
					progress -> Fx.run( () -> videoProgressBar.setProgress( progress ) ),
					message -> Fx.run( () -> renderEfficiency.setText( message ) )
				);

				double musicMillis = music.getDuration().toMillis();
				double renderMillis = projectProcessor.getRenderDuration().toMillis();
				double renderRatioValue = 100 * (renderMillis / musicMillis);

				Fx.run( () -> {
					videoProgressBar.setProgress( 1.0 );
					renderEfficiency.setText( Rb.text( getProduct(), BUNDLE, "render-ratio", renderRatioValue ) );
					videoFrames.setText( String.valueOf( frameRenderer.getFrameCount() ) );
					videoResolution.setText( width + "x" + height );
					this.renderDuration.setText( formatDuration( projectProcessor.getRenderDuration() ) );
				} );
			}
		);
		getProgram().getTaskManager().submit( renderTask );
	}

	private String formatDuration( Duration duration ) {
		String minutesAbbreviation = Rb.text( getProduct(), BUNDLE, "minutes-abbreviation" );
		String secondsAbbreviation = Rb.text( getProduct(), BUNDLE, "seconds-abbreviation" );
		String minutesText = duration.toMinutes() + " " + minutesAbbreviation;
		String secondsText = duration.toSecondsPart() + " " + secondsAbbreviation;
		return minutesText + " " + secondsText;
	}

	private TitledPane createAudioSourcePane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		int row = 0;
		Label sourceAudioPrompt = new Label( Rb.text( getProduct(), BUNDLE, "source-path-prompt" ) );
		Node sourceAudioFileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button sourceAudioButton = new Button( null, sourceAudioFileIcon );
		GridPane.setHgrow( sourceAudio, javafx.scene.layout.Priority.ALWAYS );
		grid.add( sourceAudioPrompt, 0, row, 1, 1 );
		grid.add( sourceAudio, 1, row, 3, 1 );
		grid.add( sourceAudioButton, 4, row, 1, 1 );

		row++;
		Label audioProgressPrompt = new Label( Rb.text( getProduct(), BUNDLE, "generate-spectrum-prompt" ) );
		audioProgressBar.setMaxWidth( Double.MAX_VALUE );
		grid.add( audioProgressPrompt, 0, row, 1, 1 );
		grid.add( audioProgressBar, 1, row, 3, 1 );

		row++;
		Label sampleCountPrompt = new Label( Rb.text( getProduct(), BUNDLE, "sample-count-prompt" ) );
		sampleCount.setAlignment( Pos.BASELINE_RIGHT );
		sampleCount.setEditable( false );
		GridPane.setHgrow( sampleCount, javafx.scene.layout.Priority.ALWAYS );
		Label fftCountPrompt = new Label( Rb.text( getProduct(), BUNDLE, "fft-count-prompt" ) );
		GridPane.setHgrow( fftCount, javafx.scene.layout.Priority.ALWAYS );
		fftCount.setAlignment( Pos.BASELINE_RIGHT );
		fftCount.setEditable( false );
		grid.add( sampleCountPrompt, 0, row, 1, 1 );
		grid.add( sampleCount, 1, row, 1, 1 );
		grid.add( fftCountPrompt, 2, row, 1, 1 );
		grid.add( fftCount, 3, row, 1, 1 );

		row++;
		Label sourceAudioDurationPrompt = new Label( Rb.text( getProduct(), BUNDLE, "source-audio-duration-prompt" ) );
		GridPane.setHgrow( sourceAudioDuration, javafx.scene.layout.Priority.ALWAYS );
		sourceAudioDuration.setAlignment( Pos.BASELINE_RIGHT );
		sourceAudioDuration.setEditable( false );
		grid.add( sourceAudioDurationPrompt, 0, row, 1, 1 );
		grid.add( sourceAudioDuration, 1, row, 1, 1 );

		sourceAudioButton.setOnAction( event -> requestSourceAudioFile() );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "source-path-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createVideoPropertiesPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label widthLabel = new Label( Rb.text( getProduct(), BUNDLE, "video-width-prompt" ) );
		GridPane.setHgrow( width, javafx.scene.layout.Priority.ALWAYS );
		width.setAlignment( Pos.BASELINE_RIGHT );
		width.setPromptText( String.valueOf( DEFAULT_WIDTH ) );
		Label heightLabel = new Label( Rb.text( getProduct(), BUNDLE, "video-height-prompt" ) );
		GridPane.setHgrow( height, javafx.scene.layout.Priority.ALWAYS );
		height.setAlignment( Pos.BASELINE_RIGHT );
		height.setPromptText( String.valueOf( DEFAULT_HEIGHT ) );
		Label frameRateLabel = new Label( Rb.text( getProduct(), BUNDLE, "video-frame-rate-prompt" ) );
		GridPane.setHgrow( frameRate, javafx.scene.layout.Priority.ALWAYS );
		frameRate.setAlignment( Pos.BASELINE_RIGHT );
		frameRate.setPromptText( String.valueOf( DEFAULT_FRAME_RATE ) );

		int row = 0;
		grid.add( widthLabel, 0, row, 1, 1 );
		grid.add( width, 1, row, 1, 1 );

		row++;
		grid.add( heightLabel, 0, row, 1, 1 );
		grid.add( height, 1, row, 1, 1 );

		row++;
		grid.add( frameRateLabel, 0, row, 1, 1 );
		grid.add( frameRate, 1, row, 1, 1 );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "video-properties-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBackgroundOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label backgroundPaintPrompt = new Label( Rb.text( getProduct(), BUNDLE, "background-color-prompt" ) );
		GridPane.setHgrow( backgroundColor, javafx.scene.layout.Priority.ALWAYS );
		backgroundColor.setMaxWidth( Double.MAX_VALUE );
		Label backgroundImagePrompt = new Label( Rb.text( getProduct(), BUNDLE, "background-image-prompt" ) );
		GridPane.setHgrow( backgroundImage, javafx.scene.layout.Priority.ALWAYS );
		Node fileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button backgroundImageButton = new Button( null, fileIcon );

		int row = 0;
		grid.add( backgroundPaintPrompt, 0, row, 1, 1 );
		grid.add( backgroundColor, 1, row, 2, 1 );

		row++;
		grid.add( backgroundImagePrompt, 0, row, 1, 1 );
		grid.add( backgroundImage, 1, row, 1, 1 );
		grid.add( backgroundImageButton, 2, row, 1, 1 );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "background-options-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createTargetVideoPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label outputFormatPrompt = new Label( Rb.text( getProduct(), BUNDLE, "target-format-prompt" ) );
		GridPane.setHgrow( outputFormat, javafx.scene.layout.Priority.ALWAYS );
		outputFormat.setMaxWidth( Double.MAX_VALUE );
		Label targetVideoPrompt = new Label( Rb.text( getProduct(), BUNDLE, "target-path-prompt" ) );
		GridPane.setHgrow( targetVideo, javafx.scene.layout.Priority.ALWAYS );
		Node targetVideoFileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button targetVideoButton = new Button( null, targetVideoFileIcon );

		int row = 0;
		grid.add( targetVideoPrompt, 0, row, 1, 1 );
		grid.add( targetVideo, 1, row, 1, 1 );
		grid.add( targetVideoButton, 2, row, 1, 1 );

		row++;
		grid.add( outputFormatPrompt, 0, row, 1, 1 );
		grid.add( outputFormat, 1, row, 2, 1 );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "target-path-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		targetVideoButton.setOnAction( _ -> requestTargetVideoFile() );

		return pane;
	}

	private TitledPane createBarOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label barStylePrompt = new Label( Rb.text( getProduct(), BUNDLE, "bar-style-prompt" ) );
		GridPane.setHgrow( barStyle, javafx.scene.layout.Priority.ALWAYS );
		barStyle.setMaxWidth( Double.MAX_VALUE );
		Label barPaintPrompt = new Label( Rb.text( getProduct(), BUNDLE, "bar-color-prompt" ) );
		GridPane.setHgrow( barColor, javafx.scene.layout.Priority.ALWAYS );
		barColor.setMaxWidth( Double.MAX_VALUE );

		grid.add( barStylePrompt, 0, 0 );
		grid.add( barStyle, 1, 0 );
		grid.add( barPaintPrompt, 0, 1 );
		grid.add( barColor, 1, 1 );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "bar-customization-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createGenerateVideoPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		GridPane.setHgrow( executeButton, javafx.scene.layout.Priority.ALWAYS );
		executeButton.setMaxWidth( Double.MAX_VALUE );
		executeButton.setDisable( true );

		Label progressPrompt = new Label( Rb.text( getProduct(), BUNDLE, "generate-video-prompt" ) );
		GridPane.setHgrow( videoProgressBar, javafx.scene.layout.Priority.ALWAYS );
		videoProgressBar.setMaxWidth( Double.MAX_VALUE );

		Label renderRatioPrompt = new Label( Rb.text( getProduct(), BUNDLE, "render-ratio-prompt" ) );
		GridPane.setHgrow( renderEfficiency, javafx.scene.layout.Priority.ALWAYS );
		renderEfficiency.setAlignment( Pos.BASELINE_LEFT );
		renderEfficiency.setEditable( false );

		Label videoFramePrompt = new Label( Rb.text( getProduct(), BUNDLE, "video-frames-prompt" ) );
		GridPane.setHgrow( videoFrames, javafx.scene.layout.Priority.ALWAYS );
		videoFrames.setAlignment( Pos.BASELINE_RIGHT );
		videoFrames.setEditable( false );

		Label videoResolutionPrompt = new Label( Rb.text( getProduct(), BUNDLE, "video-resolution-prompt" ) );
		GridPane.setHgrow( videoResolution, javafx.scene.layout.Priority.ALWAYS );
		videoResolution.setAlignment( Pos.BASELINE_RIGHT );
		videoResolution.setEditable( false );

		Label videoDurationPrompt = new Label( Rb.text( getProduct(), BUNDLE, "video-duration-prompt" ) );
		GridPane.setHgrow( renderDuration, javafx.scene.layout.Priority.ALWAYS );
		renderDuration.setAlignment( Pos.BASELINE_RIGHT );
		renderDuration.setEditable( false );

		int row = 0;
		grid.add( executeButton, 0, row, 4, 1 );

		row++;
		grid.add( progressPrompt, 0, row, 1, 1 );
		grid.add( videoProgressBar, 1, row, 3, 1 );

		row++;
		grid.add( renderRatioPrompt, 0, row, 1, 1 );
		grid.add( renderEfficiency, 1, row, 3, 1 );

		row++;
		grid.add( videoFramePrompt, 0, row, 1, 1 );
		grid.add( videoFrames, 1, row, 1, 1 );
		grid.add( videoResolutionPrompt, 2, row, 1, 1 );
		grid.add( videoResolution, 3, row, 1, 1 );

		row++;
		grid.add( videoDurationPrompt, 0, row, 1, 1 );
		grid.add( renderDuration, 1, row, 1, 1 );

		TitledPane pane = new TitledPane( Rb.text( getProduct(), BUNDLE, "generate-video-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private void updateActions() {
		String sourceText = sourceAudio.getText();
		String targetText = targetVideo.getText();

		boolean sourceExists = !sourceText.isBlank() && Files.exists( Path.of( sourceText ) );
		boolean targetExists = !targetText.isBlank() && Files.exists( Path.of( targetText ) );

		executeButton.setDisable( !sourceExists || !targetExists );
	}

}
