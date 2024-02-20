package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.avereon.xenon.workpane.ToolException;

public class HyperSynesthesiaTool2 extends GuidedTool {

	public HyperSynesthesiaTool2( XenonProgramProduct product, Asset asset ) {
		super( product, asset );
	}

	@Override
	protected void activate() throws ToolException {
		super.activate();

		// TODO Register the run/pause action

		pushTools( "runpause" );
	}

	@Override
	protected void conceal() throws ToolException {
		pullTools();

		// TODO Unregister the run/pause action

		super.conceal();
	}

}
