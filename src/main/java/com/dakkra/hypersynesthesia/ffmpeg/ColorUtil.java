package com.dakkra.hypersynesthesia.ffmpeg;

import java.awt.*;

class ColorUtil {

	public static Color colorWithIntensity( Color color, double intensity ) {
		return new Color( (int)(color.getRed() * intensity), (int)(color.getGreen() * intensity), (int)(color.getBlue() * intensity), color.getAlpha() );
	}
}
