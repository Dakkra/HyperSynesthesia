package com.dakkra.hypersynesthesia;

import com.avereon.skill.RunPauseResettable;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.task.TaskManager;
import com.dakkra.hypersynesthesia.task.FfftComputeTask;
import lombok.CustomLog;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@CustomLog
class ProjectProcessor implements RunPauseResettable {

	// FIXME Temporary sample data
	private static final String resource = "samples/DemoTrack.wav";

	private final List<ProjectProcessorListener> listeners;

	private final TaskManager taskManager;

	public ProjectProcessor( TaskManager taskManager ) {
		this.taskManager = taskManager;
		this.listeners = new CopyOnWriteArrayList<>();
	}

	@Override
	public void run() {
		taskManager.submit( Task.of( () -> {
			// Load the music data from an input stream
			try( InputStream input = getClass().getResourceAsStream( resource ) ) {
				MusicData music = new MusicData( input ).load();

				int ffftCount = (int)(music.getNumSamples() / (music.getSampleRate() / 60.0));
				List<Future<DSP>> fftQueue = new ArrayList<>( ffftCount );

				// Submit FFFT compute tasks to the executor
				for( int frameIdx = 0; frameIdx <= ffftCount; frameIdx++ ) {
					fftQueue.add(  taskManager.submit( new FfftComputeTask( music, frameIdx ) ) );
				}

				// TODO Collect the results
				for( Future<DSP> future : fftQueue ) {
					DSP dsp = future.get();
					// TODO Do something with the results
				}
			} catch( Exception exception ) {
				log.atError( exception ).log( "Error loading music data" );
			} finally {
				fireEvent( new ProjectProcessorEvent( this, ProjectProcessorEvent.Type.PROCESSING_COMPLETE ) );
			}
		} ) );
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
