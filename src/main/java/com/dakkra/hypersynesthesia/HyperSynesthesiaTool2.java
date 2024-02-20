package com.dakkra.hypersynesthesia;

import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.action.common.RunPauseAction;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.avereon.xenon.workpane.ToolException;
import lombok.CustomLog;

@CustomLog
public class HyperSynesthesiaTool2 extends GuidedTool {

	private final RunPauseAction runPauseAction;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		ProjectProcessor processor = new ProjectProcessor( getProgram().getTaskManager() );
		this.runPauseAction = new RunPauseAction( product.getProgram(), processor );

		// Add event handlers
		processor.addListener( e -> {
			if( e.getType() == ProjectProcessorEvent.Type.PROCESSING_COMPLETE ) {
				runPauseAction.setState( "run" );
			}
		} );
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

}
