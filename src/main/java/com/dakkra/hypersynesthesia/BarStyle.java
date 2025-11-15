package com.dakkra.hypersynesthesia;

import com.dakkra.hypersynesthesia.bar.*;
import lombok.Getter;

@Getter
public enum BarStyle {

	THICK_BLOCK( new ThickBlockBarDrawer( null, 0, 0 ) ),
	OUTLINE_BLOCK( new OutlineBlockBarDrawer( null, 0, 0 ) ),
	THIN( new ThinBarDrawer( null, 0, 0 ) ),
	ROUND_FILLED( new RoundBlockBarDrawer( null, 0, 0 ) ),
	ROUND_OUTLINE( new RoundOutlineBarDrawer( null, 0, 0 ) ),
	POPUP_BLOCK( new PopupBlockDrawer( null, 0, 0 ) ),
	ETCHED_BLOCK( new EtchedBlockDrawer( null, 0, 0 ) ),
	OVAL_FILLED( new OvalFilledDrawer( null, 0, 0 ) ),
	OVAL_OUTLINE( new OvalOutlineDrawer( null, 0, 0 ) );

	private final BarDrawer drawer;

	BarStyle( BarDrawer drawer ) {
		this.drawer = drawer;
	}

}
