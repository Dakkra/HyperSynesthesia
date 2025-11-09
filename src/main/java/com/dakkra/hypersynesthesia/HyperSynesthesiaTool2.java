package com.dakkra.hypersynesthesia;

import com.avereon.product.Rb;
import com.avereon.xenon.UiFactory;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.resource.Resource;
import com.avereon.xenon.tool.guide.GuidedTool;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class HyperSynesthesiaTool2 extends GuidedTool {

	private static final int DEFAULT_WIDTH = 1920;

	private static final int DEFAULT_HEIGHT = 1080;

	private static final int DEFAULT_FRAME_RATE = 60;

	private static final Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;

	private static final Color DEFAULT_BAR_COLOR = Color.WHITE;

	private static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.MP4;

	//	// Video Properties
	//	private int width = 1920;
	//
	//	private int height = 1080;
	//
	//	private int frameRate = 60;
	//
	//	// Background Options
	//	private BackgroundOption backgroundOption = BackgroundOption.COLOR;
	//
	//	private Paint backgroundPaint = Color.BLACK;
	//
	//	private Path backgroundImage = null;
	//
	//	// Source and target file locations
	//	private Path sourceAudio = null;
	//
	//	private Path targetVideo = null;
	//
	//	private OutputFormat outputFormat = OutputFormat.MP4;
	//
	//	// Bar options
	//	private BarStyle barStyle = BarStyle.THICK_BLOCK;
	//
	//	private Paint barPaint = Color.WHITE;

	// Video Properties
	private final TextField width;

	private final TextField height;

	private final TextField frameRate;

	// Background Options
	private final ColorPicker backgroundPaint;

	private final TextField backgroundImage;

	// Source and target file locations
	private final TextField sourceAudio;

	private final ComboBox<Option<OutputFormat>> outputFormat;

	private final TextField targetVideo;

	// Bar options
	private ComboBox<Option<BarStyle>> barStyle;

	private ColorPicker barPaint;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

		width = new TextField( String.valueOf( DEFAULT_WIDTH ) );
		height = new TextField( String.valueOf( DEFAULT_HEIGHT ) );
		frameRate = new TextField( String.valueOf( DEFAULT_FRAME_RATE ) );

		backgroundPaint = new ColorPicker( DEFAULT_BACKGROUND_COLOR );
		backgroundImage = new TextField();

		sourceAudio = new TextField();
		outputFormat = new ComboBox<>( FXCollections.observableList( Option.ofEnum( OutputFormat.values() ) ) );
		outputFormat.getSelectionModel().selectFirst();
		targetVideo = new TextField();

		barStyle = new ComboBox<>( FXCollections.observableList( Option.ofEnum( BarStyle.values() ) ) );
		barStyle.getSelectionModel().selectFirst();
		barPaint = new ColorPicker( DEFAULT_BAR_COLOR );

		GridPane grid = new GridPane();
		StackPane.setMargin( grid, new Insets( 10 ) );
		grid.setHgap( 10 );
		grid.setVgap( 10 );

		grid.add( createVideoPropertiesPane(), 0, 0 );
		grid.add( createBackgroundOptionsPane(), 1, 0 );
		grid.add( createSourceTargetPane(), 0, 1 );
		grid.add( createBarOptionsPane(), 1, 1 );

		getChildren().addAll( grid );
	}

	private TitledPane createVideoPropertiesPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label widthLabel = new Label( Rb.text( "tool", "video-width-prompt" ) );
		Label heightLabel = new Label( Rb.text( "tool", "video-height-prompt" ) );
		Label frameRateLabel = new Label( Rb.text( "tool", "video-frame-rate-prompt" ) );

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
		width.setPromptText( "1920" );

		height.setAlignment( Pos.BASELINE_RIGHT );
		height.setPromptText( "1080" );

		frameRate.setAlignment( Pos.BASELINE_RIGHT );
		frameRate.setPromptText( "60" );

		TitledPane pane = new TitledPane( Rb.text( "tool", "video-properties-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBackgroundOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label backgroundPaintPrompt = new Label( Rb.text( "tool", "background-color-prompt" ) );
		Label backgroundImagePrompt = new Label( Rb.text( "tool", "background-image-prompt" ) );

		Node fileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button backgroundImageButton = new Button( null, fileIcon);

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

		TitledPane pane = new TitledPane( Rb.text( "tool", "background-options-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createSourceTargetPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label sourceAudioPrompt = new Label( Rb.text( "tool", "source-path-prompt" ) );
		Label outputFormatPrompt = new Label( Rb.text( "tool", "target-format-prompt" ) );
		Label targetVideoPrompt = new Label( Rb.text( "tool", "target-path-prompt" ) );

		Node sourceAudioFileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button sourceAudioButton = new Button( null, sourceAudioFileIcon );
		Node targetVideoFileIcon = getProgram().getIconLibrary().getIcon( "file" );
		Button targetVideoButton = new Button( null, targetVideoFileIcon );

		grid.add( sourceAudioPrompt, 0, 0 );
		grid.add( sourceAudio, 1, 0 );
		grid.add( sourceAudioButton, 2, 0 );
		grid.add( outputFormatPrompt, 0, 1 );
		grid.add( outputFormat, 1, 1 );
		grid.add( targetVideoPrompt, 0, 2 );
		grid.add( targetVideo, 1, 2 );
		grid.add( targetVideoButton, 2, 2 );

		GridPane.setHgrow( sourceAudioPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( sourceAudio, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( outputFormatPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setColumnSpan( outputFormat, 2 );
		outputFormat.setMaxWidth( Double.MAX_VALUE );
		GridPane.setHgrow( targetVideoPrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( targetVideo, javafx.scene.layout.Priority.ALWAYS );

		TitledPane pane = new TitledPane( Rb.text( "tool", "input-and-output-files-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBarOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		Label barStylePrompt = new Label( Rb.text( "tool", "bar-style-prompt" ) );
		Label barColorPrompt = new Label( Rb.text( "tool", "bar-color-prompt" ) );

		grid.add( barStylePrompt, 0, 0 );
		grid.add( barStyle, 1, 0 );
		grid.add( barColorPrompt, 0, 1 );
		grid.add( barPaint, 1, 1 );

		GridPane.setHgrow( barStylePrompt, javafx.scene.layout.Priority.ALWAYS );
		GridPane.setHgrow( barColorPrompt, javafx.scene.layout.Priority.ALWAYS );

		barStyle.setMaxWidth( Double.MAX_VALUE );
		barPaint.setMaxWidth( Double.MAX_VALUE );

		TitledPane pane = new TitledPane( Rb.text( "tool", "bar-customization-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

}
