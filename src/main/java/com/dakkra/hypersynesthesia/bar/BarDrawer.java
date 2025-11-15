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

public abstract class BarDrawer {

	public Graphics2D g;

	public int half;

	public int barWidth;

	public BarDrawer( Graphics2D g, int half, int barWidth ) {
		this.g = g;
		this.half = half;
		this.barWidth = barWidth;
	}

	public abstract void drawBar( int height, int x );
}
