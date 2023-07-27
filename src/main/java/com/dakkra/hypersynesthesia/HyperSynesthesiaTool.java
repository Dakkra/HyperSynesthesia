package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

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

		this.setOnScroll( ( ScrollEvent event ) -> {

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

		this.setOnMouseDragEntered( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		this.setOnMouseDragged( ( MouseEvent event ) -> {
			double deltaX = event.getX() - lastX;
			double deltaY = event.getY() - lastY;
			pane.setTranslateX( pane.getTranslateX() + deltaX );
			pane.setTranslateY( pane.getTranslateY() + deltaY );
			lastX = event.getX();
			lastY = event.getY();
		} );

		this.setOnMouseMoved( ( MouseEvent event ) -> {
			lastX = event.getX();
			lastY = event.getY();
		} );

		this.getChildren().add( pane );
	}

	public static double clamp( double value, double min, double max ) {

		if( Double.compare( value, min ) < 0 ) return min;

		if( Double.compare( value, max ) > 0 ) return max;

		return value;
	}
}
