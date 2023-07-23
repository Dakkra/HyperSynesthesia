package com.dakkra.hypersynesthesia;

import com.avereon.xenon.Mod;
import lombok.CustomLog;

@CustomLog
public class HyperSynesthesia extends Mod {

	@Override
	public void register() {
		super.register();
		log.atInfo().log( "Registering HyperSynesthesia");
	}

	@Override
	public void startup() throws Exception {
		super.startup();
		log.atInfo().log( "Initializing HyperSynesthesia");
	}

	@Override
	public void shutdown() throws Exception {
		super.shutdown();
		log.atInfo().log( "Closing HyperSynesthesia");
	}

	@Override
	public void unregister() {
		super.unregister();
		log.atInfo().log( "UnRegistering HyperSynesthesia");
	}
}