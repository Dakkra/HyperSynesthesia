/*
Copyright 2018 Ryan Schroeder
Copyright 2025 Mark Soderquist - Modified for use in HyperSynesthesia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.dakkra.hypersynesthesia.bar;

import java.awt.*;

public class OutlineBlockBarDrawer extends BarDrawer {

	@Override
	public void drawBar( Graphics2D g, int width, int height, int x, int half ) {
		Stroke stroke = new BasicStroke( 1 );
		g.setStroke( stroke );
		int k = width / 6;
		g.drawLine( x + k, half, x + k, half + height ); // draws down
		g.drawLine( x - k, half, x - k, half + height ); // draws down
		g.drawLine( x - k, half + height, x + k, half + height ); // draws across
		g.drawLine( x + k, half, x + k, half - height ); // draws up
		g.drawLine( x - k, half, x - k, half - height ); // draws up
		g.drawLine( x - k, half - height, x + k, half - height ); // draws across
	}

}
