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

public class RoundOutlineBarDrawer extends BarDrawer {

	@Override
	public void drawBar( Graphics2D g, int width, int height, int x, int half ) {
		int center = width / 2;
		int drawBarWidth = width - 3;
		int drawHeight = height + 1; // make it so we never see a value of 0
		int radius = drawBarWidth;
		Stroke stroke = new BasicStroke( 1 );
		g.setStroke( stroke );
		g.drawRoundRect( x - center, half - height, drawBarWidth, drawHeight * 2, radius, radius );
	}

}
