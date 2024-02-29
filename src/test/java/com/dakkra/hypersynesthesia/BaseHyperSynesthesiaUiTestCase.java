package com.dakkra.hypersynesthesia;

import com.avereon.zerra.BaseModUiTestCase;

abstract class BaseHyperSynesthesiaUiTestCase extends BaseModUiTestCase<HyperSynesthesia> {

	protected BaseHyperSynesthesiaUiTestCase() {
		super( HyperSynesthesia.class );
	}

	@Override
	protected HyperSynesthesia getMod() {
		return (HyperSynesthesia)super.getMod();
	}

}
