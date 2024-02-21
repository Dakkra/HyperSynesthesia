package com.dakkra.hypersynesthesia;

import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameProducer;
import com.github.kokorin.jaffree.ffmpeg.Stream;

import java.util.Collections;
import java.util.List;

public class FFmpegFrameProducer implements FrameProducer {

	// The width of the frames
	private final int width;

	// The height of the frames
	private final int height;

	// How many times per second the frames will be provided
	private final long timebase;

	public FFmpegFrameProducer( int width, int height, int timebase ) {
		this.width = width;
		this.height = height;
		this.timebase = timebase;
	}

	@Override
	public List<Stream> produceStreams() {
		return Collections.singletonList( new Stream().setType( Stream.Type.VIDEO ).setTimebase( timebase ).setWidth( width ).setHeight( height ) );
	}

	@Override
	public Frame produce() {
		// NEXT Provide ordered frames for FFmpeg from the rendering queue
		return null;
	}

}
