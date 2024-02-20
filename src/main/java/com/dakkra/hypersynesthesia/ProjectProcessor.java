package com.dakkra.hypersynesthesia;

import com.avereon.skill.RunPauseResettable;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.task.TaskManager;
import com.dakkra.hypersynesthesia.task.FfftComputeTask;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@CustomLog
class ProjectProcessor implements RunPauseResettable {

	private static final double FRAMES_PER_SECOND = 60.0;

	private final List<ProjectProcessorListener> listeners;

	private final TaskManager taskManager;

	private final InputStream stream;

	private MusicData music;

	public ProjectProcessor( TaskManager taskManager, InputStream stream ) {
		this.taskManager = taskManager;
		this.stream = stream;
		this.listeners = new CopyOnWriteArrayList<>();
	}

	@Override
	public void run() {
		taskManager.submit( Task.of( () -> {
			try {
				music = extractMusicData();

				try {
					int ffftCount = (int)(music.getNumSamples() / (music.getSampleRate() / FRAMES_PER_SECOND));
					List<Future<DSP>> ffftResults = new ArrayList<>( ffftCount );

					// Submit FFFT compute tasks to the executor
					for( int frameIdx = 0; frameIdx <= ffftCount; frameIdx++ ) {
						ffftResults.add( taskManager.submit( new FfftComputeTask( music, frameIdx ) ) );
					}

					// TODO Collect the results
					for( Future<DSP> future : ffftResults ) {
						DSP dsp = future.get();
						// TODO Do something with the results
						//fftQueue.offer( new PrioritySpectrum( new ArrayList<>( Arrays.asList( Arrays.stream( dsp.getSpectrum() ).boxed().toArray( Double[]::new ) ) ), index ) );
						//loudnessQueue.offer( new PriorityLoudness( dsp.getRMSLoudness(), index ) );
					}
				} catch( Exception exception ) {
					log.atError( exception ).log( "Error loading music data" );
				}
			} finally {
				fireEvent( new ProjectProcessorEvent( this, ProjectProcessorEvent.Type.PROCESSING_COMPLETE ) );
			}
		} ) );
	}

	private MusicData extractMusicData() {
		try( stream ) {
			MusicData result = new MusicData( stream ).load();
			if( stream != null ) stream.close();
			return result;
		} catch(IOException exception ) {
			// FIXME Why not throw a TaskException?
			throw new RuntimeException( exception );
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
