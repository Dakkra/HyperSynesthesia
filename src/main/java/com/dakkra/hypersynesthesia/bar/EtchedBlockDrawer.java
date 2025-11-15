/*
Copyright 2018 Ryan Schroeder

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Copyright 2025 Mark Soderquist - Modified for use in HyperSynesthesia
*/

package com.dakkra.hypersynesthesia.bar;

import java.awt.*;

public class EtchedBlockDrawer extends BarDrawer {

	public EtchedBlockDrawer( Graphics2D g, int half, int barWidth ) {
		super( g, half, barWidth );
		g.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE );
		g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

	}

	@Override
	public void drawBar( int height, int x ) {
		int center = barWidth / 2;
		int drawBarWidth = barWidth - 3;
		int drawHeight = height + 1; // make it so we never see a value of 0
		Stroke stroke = new BasicStroke( 1 );
		g.setStroke( stroke );

		// new way
		// System.out.println(radius); --system runs on truncation for non ints
		g.fill3DRect( x - center, half - height, drawBarWidth, drawHeight * 2, false );// draw up and down

	}

}
