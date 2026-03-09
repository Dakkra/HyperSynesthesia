package com.dakkra.hypersynesthesia;

import com.acromere.xenon.Xenon;
import com.acromere.xenon.XenonProgramProduct;
import com.acromere.xenon.resource.Resource;
import com.acromere.xenon.resource.ResourceType;
import com.acromere.xenon.resource.exception.ResourceException;

public class ProjectResourceType extends ResourceType {

	public ProjectResourceType( XenonProgramProduct product ) {
		super( product, "hypersynesthesia" );
	}

	@Override
	public String getKey() {
		return "com.hypersynesthesia.project";
	}

	@Override
	public boolean assetNew( Xenon program, Resource resource ) throws ResourceException {
		// TODO implement default resolution of (screen dimensions) here
		return super.assetNew( program, resource );
	}

	@Override
	public boolean assetOpen( Xenon program, Resource resource ) throws ResourceException {
		return super.assetOpen( program, resource );
	}
}
