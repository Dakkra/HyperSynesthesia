package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
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

@CustomLog
public class HyperSynesthesiaTool extends GuidedTool {

	private final double MIN_SCALE = 0.05;

	private final double MAX_SCALE = 5.0;

	private final Pane renderPane;

	private double lastX;

	private double lastY;

	public HyperSynesthesiaTool( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		renderPane = new Pane();
		Label name = new Label( "HyperSynesthesia" );
		name.setStyle( "-fx-font-size: 5cm" + "; -fx-font-weight: bold" + "; -fx-text-fill: #000000;" );
		renderPane.getChildren().add( name );

		renderPane.setStyle( "-fx-background-color: radial-gradient(center 50% 50% , radius 40% , #ffebcd, #008080);" );
		renderPane.setMaxSize( 1920, 1080 );
		renderPane.setMinSize( 1920, 1080 );

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
	}

	public static double clamp( double value, double min, double max ) {

		if( Double.compare( value, min ) < 0 ) return min;

		if( Double.compare( value, max ) > 0 ) return max;

		return value;
	}

	private BufferedImage renderBufferedImaged() {
		WritableImage image = renderPane.snapshot( new SnapshotParameters(), null );
		return SwingFXUtils.fromFXImage( image, null );
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
