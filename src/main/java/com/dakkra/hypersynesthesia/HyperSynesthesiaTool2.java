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

public class HyperSynesthesiaTool2 extends GuidedTool {

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
	private final TextField width = new TextField();

	private final TextField height = new TextField();

	private final TextField frameRate = new TextField();

	// Background Options
	private final ColorPicker backgroundPaint = new ColorPicker();

	private final TextField backgroundImage = new TextField();

	// Source and target file locations
	private final TextField sourceAudio = new TextField();

	private final TextField targetVideo = new TextField();

	private ComboBox<Option<OutputFormat>> outputFormat;

	// Bar options
	private TextField barStyle = new TextField();

	private TextField barPaint = new TextField();

	public HyperSynesthesiaTool2( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

		outputFormat = new ComboBox<>( FXCollections.observableList( Option.ofEnum( OutputFormat.values() ) ) );

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
		GridPane.setHgrow( backgroundImagePrompt, javafx.scene.layout.Priority.ALWAYS );

		backgroundPaint.setMaxWidth( Double.MAX_VALUE );
		GridPane.setColumnSpan( backgroundPaint, 2 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "background-options-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createSourceTargetPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		grid.add( new Label( Rb.text( "tool", "source-path-prompt" ) ), 0, 0 );
		grid.add( new Label( Rb.text( "tool", "target-format-prompt" ) ), 0, 1 );
		grid.add( new Label( Rb.text( "tool", "target-path-prompt" ) ), 0, 2 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "input-and-output-files-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBarOptionsPane() {
		GridPane grid = new GridPane( UiFactory.PAD, UiFactory.PAD );

		grid.add( new Label( Rb.text( "tool", "bar-style-prompt" ) ), 0, 0 );
		grid.add( new Label( Rb.text( "tool", "bar-color-prompt" ) ), 0, 1 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "bar-customization-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

}
