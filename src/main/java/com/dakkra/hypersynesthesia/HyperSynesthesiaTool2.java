package com.dakkra.hypersynesthesia;

import com.avereon.skill.RunPauseResettable;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.action.common.RunPauseAction;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.avereon.xenon.workpane.ToolException;

public class HyperSynesthesiaTool2 extends GuidedTool {

	private final RunPauseAction runPauseAction;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		this.runPauseAction = new RunPauseAction( product.getProgram(), new RunPauseResettableImpl() );
	}

	@Override
	protected void activate() throws ToolException {
		super.activate();

		pushAction( "runpause", runPauseAction );
		pushTools( "runpause" );
	}

	@Override
	protected void conceal() throws ToolException {
		pullTools();
		pullAction( "runpause", runPauseAction );

		super.conceal();
	}

	private static class RunPauseResettableImpl implements RunPauseResettable {

		@Override
		public void run() {
			// TODO Implement the run method
		}

		@Override
		public void pause() {
			// TODO Implement the pause method
		}

		@Override
		public void reset() {
			// TODO Implement the reset method
		}

	}

}
