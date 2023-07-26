package com.dakkra.hypersynesthesia;

import com.avereon.xenon.Mod;
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
	}

	@Override
	public void shutdown() throws Exception {
		super.shutdown();
		log.atInfo().log( "Closing HyperSynesthesia");

		unregisterAssetType( projectAssetType );
	}

	@Override
	public void unregister() {
		super.unregister();
		log.atInfo().log( "UnRegistering HyperSynesthesia");
	}
}