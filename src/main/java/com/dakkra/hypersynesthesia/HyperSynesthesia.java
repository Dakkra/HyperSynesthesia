package com.dakkra.hypersynesthesia;

import com.avereon.xenon.Mod;
import com.avereon.xenon.ToolRegistration;
import lombok.CustomLog;

@CustomLog
public class HyperSynesthesia extends Mod {

	private ProjectAssetType projectAssetType;

	@Override
	public void register() {
		super.register();
		log.atInfo().log( "Registering HyperSynesthesia");
	}

	@Override
	public void startup() throws Exception {
		super.startup();
		log.atInfo().log( "Initializing HyperSynesthesia");

		projectAssetType = new ProjectAssetType( this );
		registerAssetType( projectAssetType );
		//ToolRegistration registration = new ToolRegistration( this, HyperSynesthesiaTool.class);
		//registration.setName( "HyperSynestheisa Tool" );
		//registerTool( projectAssetType, registration );
		ToolRegistration registrationV2 = new ToolRegistration( this, HyperSynesthesiaTool2.class);
		registrationV2.setName( "HyperSynestheisa Tool v2" );
		registerTool( projectAssetType, registrationV2 );
	}

	@Override
	public void shutdown() throws Exception {
		log.atInfo().log( "Closing HyperSynesthesia");

		unregisterTool( projectAssetType, HyperSynesthesiaTool2.class );
		//unregisterTool( projectAssetType, HyperSynesthesiaTool.class );
		unregisterAssetType( projectAssetType );
		super.shutdown();
	}

	@Override
	public void unregister() {
		log.atInfo().log( "UnRegistering HyperSynesthesia");
		super.unregister();
	}
}
