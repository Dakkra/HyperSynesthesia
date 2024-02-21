package com.dakkra.hypersynesthesia;

import com.avereon.xenon.task.Task;
import com.github.kokorin.jaffree.ffmpeg.Frame;

import java.awt.image.BufferedImage;

public class FrameRenderTask extends Task<Frame> {

	private final FFmpegFrameSequencer sequencer;

	private final int index;

	public FrameRenderTask( FFmpegFrameSequencer sequencer, int index ) {
		this.sequencer = sequencer;
		this.index = index;
	}

	@Override
	public Frame call() throws InterruptedException {
		BufferedImage image = new BufferedImage( 1920, 1080, BufferedImage.TYPE_3BYTE_BGR );
		Frame frame = Frame.createVideoFrame( index, index, image );
		sequencer.addFrame( frame );
		return frame;
	}

}
