package com.dakkra.hypersynesthesia;

import com.avereon.skill.RunPauseResettable;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.action.common.RunPauseAction;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.tool.guide.GuidedTool;
import com.avereon.xenon.workpane.ToolException;
import lombok.CustomLog;

import java.io.InputStream;

@CustomLog
public class HyperSynesthesiaTool2 extends GuidedTool {

	// FIXME Temporary sample data
	private static final String resource = "samples/DemoTrack.wav";

	private final RunPauseAction runPauseAction;

	private ProjectProcessor processor;

	public HyperSynesthesiaTool2( XenonProgramProduct product, Asset asset ) {
		super( product, asset );

		this.runPauseAction = new RunPauseAction( product.getProgram(), new RunPauseActionHandler() );
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

	private class RunPauseActionHandler implements RunPauseResettable {

		@Override
		public void run() {
			InputStream inputStream = getClass().getResourceAsStream( resource );
			processor = new ProjectProcessor( getProgram().getTaskManager(), inputStream );

			// Add event handlers
			processor.addListener( e -> {
				if( e.getType() == ProjectProcessorEvent.Type.PROCESSING_COMPLETE ) {
					runPauseAction.setState( "run" );
				}
			} );

			processor.run();
		}

		@Override
		public void pause() {
			processor.pause();
		}

		@Override
		public void reset() {
			processor.reset();
		}

	}

}
