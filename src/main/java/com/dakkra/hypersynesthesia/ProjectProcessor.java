package com.dakkra.hypersynesthesia;

import com.avereon.skill.RunPauseResettable;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.task.TaskException;
import com.avereon.xenon.task.TaskManager;
import com.dakkra.hypersynesthesia.task.FftComputeTask;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@CustomLog
class ProjectProcessor implements RunPauseResettable {

	private static final double FRAMES_PER_SECOND = 60.0;

	private final List<ProjectProcessorListener> listeners;

	private final TaskManager taskManager;

	private final InputStream stream;

	private MusicData music;

	private int frameCount;

	public ProjectProcessor( TaskManager taskManager, InputStream stream ) {
		this.taskManager = taskManager;
		this.stream = stream;
		this.listeners = new CopyOnWriteArrayList<>();
	}

	@Override
	public void run() {
		taskManager.submit( Task.of( this::processProject ) );
	}

	private void processProject() {
		try {
			this.music = extractMusicData();
			this.frameCount = (int)(music.getNumSamples() / (music.getSampleRate() / FRAMES_PER_SECOND));

			// Submit FFT compute tasks to the executor
			List<Future<DSP>> fftResults = new ArrayList<>( frameCount );
			for( int frameIndex = 0; frameIndex <= frameCount; frameIndex++ ) {
				fftResults.add( taskManager.submit( new FftComputeTask( music, frameIndex ) ) );
			}

			// Collect the results. This blocks until the results are ready.
			Queue<PrioritySpectrum> spectrumQueue = new PriorityQueue<>();
			Queue<PriorityLoudness> loudnessQueue = new PriorityQueue<>();
			for( Future<DSP> future : fftResults ) {
				DSP dsp = future.get();
				spectrumQueue.offer( new PrioritySpectrum( dsp ) );
				loudnessQueue.offer( new PriorityLoudness( dsp ) );
			}

			// NEXT Submit the spectrum and loudness data for rendering the frames


		} catch( Exception exception ) {
			throw new TaskException( exception );
		} finally {
			fireEvent( new ProjectProcessorEvent( this, ProjectProcessorEvent.Type.PROCESSING_COMPLETE ) );
		}
	}

	private MusicData extractMusicData() throws IOException {
		try( stream ) {
			MusicData result = new MusicData( stream ).load();
			if( stream != null ) stream.close();
			return result;
		}
	}

	@Override
	public void pause() {
		// TODO Hold all the tasks from the executor
	}

	@Override
	public void reset() {
		// TODO Throw away all the tasks
	}

	public void addListener( ProjectProcessorListener listener ) {
		listeners.add( listener );
	}

	public void removeListener( ProjectProcessorListener listener ) {
		listeners.remove( listener );
	}

	private void fireEvent( ProjectProcessorEvent event ) {
		listeners.forEach( listener -> listener.handleEvent( event ) );
	}

}
