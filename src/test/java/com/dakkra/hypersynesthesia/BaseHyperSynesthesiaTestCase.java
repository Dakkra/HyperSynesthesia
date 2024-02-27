package com.dakkra.hypersynesthesia;

import com.avereon.zerra.BaseModTestCase;

abstract class BaseHyperSynesthesiaTestCase extends BaseModTestCase<HyperSynesthesia> {

	protected BaseHyperSynesthesiaTestCase() {
		super( HyperSynesthesia.class );
	}

	@Override
	protected HyperSynesthesia getMod() {
		return (HyperSynesthesia)super.getMod();
	}

}
