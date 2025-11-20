package com.dakkra.hypersynesthesia.ffmpeg;

import org.jspecify.annotations.NonNull;

import java.awt.image.BufferedImage;

record IndexedImage(BufferedImage internalImage, long frameIdx) implements Comparable<IndexedImage> {

	@Override
	public int compareTo( @NonNull IndexedImage other ) {
		return (int)(this.frameIdx - other.frameIdx);
	}

}
