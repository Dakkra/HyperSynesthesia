package com.dakkra.hypersynesthesia;

import com.avereon.xenon.Xenon;
import com.avereon.xenon.XenonProgramProduct;
import com.avereon.xenon.asset.Asset;
import com.avereon.xenon.asset.AssetType;
import com.avereon.xenon.asset.exception.AssetException;

public class ProjectAssetType extends AssetType {

	public ProjectAssetType( XenonProgramProduct product ) {
		super( product, "hypersynesthesia" );
	}

	@Override
	public String getKey() {
		return "com.hypersynesthesia.project";
	}

	@Override
	public boolean assetNew( Xenon program, Asset asset ) throws AssetException {
		// TODO implement default resolution of (screen dimensions) here
		return super.assetNew( program, asset );
	}

	@Override
	public boolean assetOpen( Xenon program, Asset asset ) throws AssetException {
		return super.assetOpen( program, asset );
	}
}
