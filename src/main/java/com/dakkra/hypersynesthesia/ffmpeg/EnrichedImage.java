package com.dakkra.hypersynesthesia.ffmpeg;

import java.awt.image.BufferedImage;

record EnrichedImage(BufferedImage internalImage, long frameIdx) implements Comparable {

	@Override
	public int compareTo( Object other ) {
		if( other instanceof EnrichedImage ) {
			long otherFrameIdx = ((EnrichedImage)other).frameIdx;
			return (int)(this.frameIdx - otherFrameIdx);
		}
		throw new UnsupportedOperationException( "Unimplemented method 'compareTo'" );
	}
}
