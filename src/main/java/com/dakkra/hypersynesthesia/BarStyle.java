package com.dakkra.hypersynesthesia;

import com.dakkra.hypersynesthesia.bar.*;
import lombok.Getter;

@Getter
public enum BarStyle {

	THICK_BLOCK( new ThickBlockBarDrawer() ),
	OUTLINE_BLOCK( new OutlineBlockBarDrawer() ),
	THIN( new ThinBarDrawer() ),
	ROUND_FILLED( new RoundBlockBarDrawer() ),
	ROUND_OUTLINE( new RoundOutlineBarDrawer() ),
	POPUP_BLOCK( new PopupBlockDrawer() ),
	ETCHED_BLOCK( new EtchedBlockDrawer() ),
	OVAL_FILLED( new OvalFilledDrawer() ),
	OVAL_OUTLINE( new OvalOutlineDrawer() );

	private final BarDrawer drawer;

	BarStyle( BarDrawer drawer ) {
		this.drawer = drawer;
	}

}
