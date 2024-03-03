package com.dakkra.hypersynesthesia;

import com.avereon.xenon.Mod;
import com.avereon.xenon.ToolRegistration;
import lombok.CustomLog;

@CustomLog
public class HyperSynesthesia extends Mod {

	private final ProjectAssetType projectAssetType;

	public HyperSynesthesia() {
		projectAssetType = new ProjectAssetType( this );
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

		registerAssetType( projectAssetType );
		ToolRegistration registration = new ToolRegistration( this, HyperSynesthesiaTool.class);
		registration.setName( "HyperSynestheisa Tool" );
		registerTool( projectAssetType, registration );
	}

	@Override
	public void shutdown() throws Exception {
		super.shutdown();
		log.atInfo().log( "Closing HyperSynesthesia");

		unregisterTool( projectAssetType, HyperSynesthesiaTool.class );
		unregisterAssetType( projectAssetType );
	}

	@Override
	public void unregister() {
		super.unregister();
		log.atInfo().log( "UnRegistering HyperSynesthesia");
	}
}
