package com.dakkra.hypersynesthesia;

import com.avereon.product.Rb;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.resource.Resource;
import com.avereon.xenon.tool.guide.GuidedTool;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.nio.file.Path;

public class HyperSynesthesiaTool2 extends GuidedTool {

	// Video Properties
	private int width = 1920;

	private int height = 1080;

	private int frameRate = 60;

	// Background Options
	private BackgroundOption backgroundOption = BackgroundOption.COLOR;

	private Paint backgroundPaint = Color.BLACK;

	private Path backgroundImage = null;

	// Source and target file locations
	private Path sourceAudio = null;

	private Path targetVideo = null;

	private OutputFormat outputFormat = OutputFormat.MP4;

	// Bar options
	private BarStyle barStyle = BarStyle.THICK_BLOCK;

	private Paint barPaint = Color.WHITE;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Resource resource ) {
		super( product, resource );

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
		GridPane grid = new GridPane();

		grid.add( new Label( Rb.text( "tool", "video-width-prompt" ) ), 0, 0 );
		grid.add( new Label( Rb.text( "tool", "video-height-prompt" ) ), 0, 1 );
		grid.add( new Label( Rb.text( "tool", "video-frame-rate-prompt" ) ), 0, 2 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "video-properties-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBackgroundOptionsPane() {
		GridPane grid = new GridPane();

		grid.add( new Label( Rb.text( "tool", "background-option-prompt" ) ), 0, 0 );
		grid.add( new Label( Rb.text( "tool", "background-color-prompt" ) ), 0, 1 );
		grid.add( new Label( Rb.text( "tool", "background-image-prompt" ) ), 0, 2 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "background-options-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createSourceTargetPane() {
		GridPane grid = new GridPane();

		grid.add( new Label( "Input Audio:" ), 0, 0 );
		grid.add( new Label( "Output Format:" ), 0, 1 );
		grid.add( new Label( "Output Video:" ), 0, 2 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "input-and-output-files-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

	private TitledPane createBarOptionsPane() {
		GridPane grid = new GridPane();

		grid.add( new Label( "Bar Style:" ), 0, 0 );
		grid.add( new Label( "Bar Color:" ), 0, 1 );

		TitledPane pane = new TitledPane( Rb.text( "tool", "bar-customization-title" ), grid );
		pane.setCollapsible( false );
		GridPane.setValignment( pane, javafx.geometry.VPos.TOP );
		GridPane.setHgrow( pane, javafx.scene.layout.Priority.ALWAYS );

		return pane;
	}

}
