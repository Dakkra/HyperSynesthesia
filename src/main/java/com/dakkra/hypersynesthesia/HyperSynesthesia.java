package com.dakkra.hypersynesthesia;

import com.avereon.xenon.Module;
import com.avereon.xenon.ToolRegistration;
import lombok.CustomLog;

@CustomLog
public class HyperSynesthesia extends Module {

	private final ProjectResourceType projectResourceType;

	public HyperSynesthesia() {
		projectResourceType = new ProjectResourceType( this );
	}

	@Override
	public void register() {
		super.register();
		log.atInfo().log( "Registering HyperSynesthesia");
	}

	@Override
	public void startup() throws Exception {
		super.startup();
		log.atInfo().log( "Initializing HyperSynesthesia");

		registerAssetType( projectResourceType );
		ToolRegistration registration = new ToolRegistration( this, HyperSynesthesiaTool2.class);
		registration.setName( "HyperSynestheisa Tool" );
		registerTool( projectResourceType, registration );
	}

	@Override
	public void shutdown() throws Exception {
		super.shutdown();
		log.atInfo().log( "Closing HyperSynesthesia");

		unregisterTool( projectResourceType, HyperSynesthesiaTool2.class );
		unregisterAssetType( projectResourceType );
	}

	@Override
	public void unregister() {
		super.unregister();
		log.atInfo().log( "UnRegistering HyperSynesthesia");
	}
}
