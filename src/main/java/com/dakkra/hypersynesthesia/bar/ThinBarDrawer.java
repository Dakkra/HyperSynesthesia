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

public class ThinBarDrawer extends BarDrawer {

	public ThinBarDrawer( Graphics2D g, int half, int barWidth ) {
		super( g, half, barWidth );
	}

	@Override
	public void drawBar( int height, int x ) {
		Stroke stroke = new BasicStroke( 2 );
		g.setStroke( stroke );
		g.drawLine( x, half, x, half + height ); // draws down
		g.drawLine( x, half, x, half - height ); // draws up
	}

}
