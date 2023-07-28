package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.CustomLog;

import java.io.File;

@CustomLog
public class HyperSynesthesiaTool extends GuidedTool {

	private final double MIN_SCALE = 0.05;

	private final double MAX_SCALE = 5.0;

	private final Pane pane;

	private double lastX;

	private double lastY;

	public HyperSynesthesiaTool( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		pane = new Pane();
		Label name = new Label( "HyperSynesthesia" );
		name.setStyle( "-fx-font-size: 5cm" + "; -fx-font-weight: bold" + "; -fx-text-fill: #000000;" );
		pane.getChildren().add( name );

		pane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius 40% , #ffebcd, #008080);" );
		pane.setMaxSize( 1920, 1080 );
		pane.setMinSize( 1920, 1080 );

		double DEFAULT_SCALE = 0.5;
		pane.setScaleX( DEFAULT_SCALE );
		pane.setScaleY( DEFAULT_SCALE );

		Pane container = new Pane( pane );
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

			double scale = pane.getScaleX();
			double oldscale = scale;

			if( event.getDeltaY() > 0 ) {
				scale *= 1.1;
			} else {
				scale /= 1.1;
			}
			scale = clamp( scale, MIN_SCALE, MAX_SCALE );

			double f = (scale / oldscale) - 1;

			double deltaX = (event.getX() - (pane.getBoundsInParent().getWidth() / 2 + pane.getBoundsInParent().getMinX()));
			double deltaY = (event.getY() - (pane.getBoundsInParent().getHeight() / 2 + pane.getBoundsInParent().getMinY()));

			pane.setScaleX( scale );
			pane.setScaleY( scale );

			pane.setTranslateX( pane.getTranslateX() - f * deltaX );
			pane.setTranslateY( pane.getTranslateY() - f * deltaY );
		} );

		container.setOnMouseDragEntered( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		container.setOnMouseDragged( ( MouseEvent event ) -> {
			double deltaX = event.getX() - lastX;
			double deltaY = event.getY() - lastY;
			pane.setTranslateX( pane.getTranslateX() + deltaX );
			pane.setTranslateY( pane.getTranslateY() + deltaY );
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

	private void loadMusicFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Open Music File" );

		File file = fileChooser.showOpenDialog( getProgram().getWorkspaceManager().getActiveStage() );
	}

	private void exportVideo() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Export Video" );

		File file = fileChooser.showSaveDialog( getProgram().getWorkspaceManager().getActiveStage() );
	}

}
