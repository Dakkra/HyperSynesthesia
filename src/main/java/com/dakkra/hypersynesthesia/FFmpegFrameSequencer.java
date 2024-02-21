package com.dakkra.hypersynesthesia;

import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameProducer;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import lombok.CustomLog;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@CustomLog
public class FFmpegFrameSequencer implements FrameProducer {

	private static final int coreCount = Runtime.getRuntime().availableProcessors();

	private final VideoData videoData;

	private final Stream stream;

	private final BlockingQueue<Frame> waitingQueue;

	private final BlockingQueue<Frame> readyQueue;

	private int nextExpectedFrame;

	private boolean done;

	public FFmpegFrameSequencer( VideoData videoData ) {
		this.videoData = videoData;
		this.stream = new Stream().setType( Stream.Type.VIDEO ).setTimebase( videoData.timebase() ).setWidth( videoData.width() ).setHeight( videoData.height() );
		this.waitingQueue = new ArrayBlockingQueue<>( 4 * coreCount, true );
		this.readyQueue = new ArrayBlockingQueue<>( 4 * coreCount, true );
	}

	@Override
	public List<Stream> produceStreams() {
		return Collections.singletonList( stream );
	}

	@Override
	public Frame produce() {
		if( done ) {
			log.atConfig().log( "No more frames to produce!" );
			return null;
		}
		try {
			Frame frame = readyQueue.take();
			log.atConfig().log( "Producing frame %d", frame.getPts() );
			if( frame.getPts() + 1 >= videoData.frameCount() ) done = true;
			return frame;
		} catch( InterruptedException exception ) {
			return null;
		}
	}

	public void addFrame( Frame frame ) throws InterruptedException {
		waitingQueue.put( frame );
		//log.atConfig().log( "Frame %d added to waiting queue, queue size %d", frame.getPts(), waitingQueue.size() );
		checkForAvailableFrames();
	}

	private synchronized void checkForAvailableFrames() throws InterruptedException {
		List<Frame> orderedFrames = new java.util.ArrayList<>( waitingQueue );
		orderedFrames.sort( new FrameComparator() );

		for( Frame frame : orderedFrames ) {
			if( frame.getPts() == nextExpectedFrame ) {
				if( waitingQueue.remove( frame ) ) readyQueue.put( frame );
				log.atConfig().log( "Frame %d added to ready queue, queue size %d", frame.getPts(), readyQueue.size() );
				nextExpectedFrame++;
			} else {
				break;
			}
		}
	}

	private static class FrameComparator implements java.util.Comparator<Frame> {

		@Override
		public int compare( Frame frame1, Frame frame2 ) {
			return Long.compare( frame1.getPts(), frame2.getPts() );
		}

	}

}
