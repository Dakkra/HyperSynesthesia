package com.dakkra.hypersynesthesia;

import com.avereon.skill.RunPauseResettable;
import com.avereon.xenon.task.Task;
import com.avereon.xenon.task.TaskException;
import com.avereon.xenon.task.TaskManager;
import com.dakkra.hypersynesthesia.task.FftComputeTask;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.FrameInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

@CustomLog
class ProjectProcessor implements RunPauseResettable {

	private static final int DEFAULT_FRAME_WIDTH = 1920;

	private static final int DEFAULT_FRAME_HEIGHT = 1080;

	private static final int TIMEBASE = 60;

	private static final int AVG_QUEUE_SIZE = 5;

	private final List<ProjectProcessorListener> listeners;

	private final TaskManager taskManager;

	private final Set<Task<?>> tasks;

	private final InputStream stream;

	private MusicData music;

	private int frameCount;

	private VideoData videoData;

	public ProjectProcessor( TaskManager taskManager, InputStream stream ) {
		this.taskManager = taskManager;
		this.stream = stream;
		this.tasks = new CopyOnWriteArraySet<>();
		this.listeners = new CopyOnWriteArrayList<>();
	}

	@Override
	public void run() {
		// Get the processing off the UI thread
		taskManager.submit( Task.of( this::processProject ) );
	}

	private <T> Future<T> submitTask( Task<T> task ) {
		tasks.add( task );
		return taskManager.submit( task );
	}

	void processProject() {
		try {
			log.atConfig().log( "Loading music data..." );
			this.music = extractMusicData();
			this.frameCount = (int)(music.getSampleCount() / (music.getSampleRate() / TIMEBASE));
			this.videoData = new VideoData( frameCount, DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT, TIMEBASE );
			log.atInfo().log( "Music data loaded." );

			// Submit FFT compute tasks to the executor
			log.atConfig().log( "Calculating FFTs..." );
			List<Future<DSP>> fftResults = new ArrayList<>( frameCount );
			for( int frameIndex = 0; frameIndex <= frameCount; frameIndex++ ) {
				fftResults.add( submitTask( new FftComputeTask( music, frameIndex ) ) );
			}

			// Collect the results. This blocks until the results are ready.
			Queue<PrioritySpectrum> spectrumQueue = new PriorityQueue<>();
			Queue<PriorityLoudness> loudnessQueue = new PriorityQueue<>();
			for( Future<DSP> future : fftResults ) {
				DSP dsp = future.get();
				spectrumQueue.offer( new PrioritySpectrum( dsp ) );
				loudnessQueue.offer( new PriorityLoudness( dsp ) );
			}
			log.atInfo().log( "FFTs calculated." );

			log.atConfig().log( "Compute averaged data..." );

			List<List<Double>> spectraAvg = new ArrayList<>();
			List<Double> loudnessAvg = new ArrayList<>();

			// Queues for AVG
			Queue<Double> internalLoudnessQueue = new ArrayBlockingQueue<>( AVG_QUEUE_SIZE );
			for( int index = 0; index < AVG_QUEUE_SIZE; index++ ) internalLoudnessQueue.add( 0.0 );

			Queue<List<Double>> internalSpectraQueue = new ArrayBlockingQueue<>( AVG_QUEUE_SIZE );

			for( int frameIdx = 0; frameIdx < frameCount; frameIdx++ ) {
				// RMS section
				double loudness = Objects.requireNonNull( loudnessQueue.poll() ).getLoudness();
				double rmsAvg = internalLoudnessQueue.stream().mapToDouble( Double::doubleValue ).average().orElse( 0.0 );
				internalLoudnessQueue.poll();
				internalLoudnessQueue.add( loudness );

				// Spectra section
				double[] spectrum = Objects.requireNonNull( spectrumQueue.poll() ).getSpectrum();
				for( int i = 0; i < AVG_QUEUE_SIZE; i++ ) {
					internalSpectraQueue.offer( new ArrayList<>( Arrays.asList( Arrays.stream( new double[ spectrum.length ] ).boxed().toArray( Double[]::new ) ) ) );
				}
				internalSpectraQueue.poll();
				internalSpectraQueue.offer( new ArrayList<>( Arrays.asList( Arrays.stream( spectrum ).boxed().toArray( Double[]::new ) ) ) );

				// Smooth spectrum
				ArrayList<Double> spectrumAvg = new ArrayList<>( spectrum.length );
				for( int bucketIndex = 0; bucketIndex < spectrum.length; bucketIndex++ ) {
					double bucketSum = 0;
					for( List<Double> nextSpectrum : internalSpectraQueue ) {
						if( bucketIndex < nextSpectrum.size() ) {
							bucketSum += nextSpectrum.get( bucketIndex );
						}
					}
					// Mean value between spectra for this bucket
					spectrumAvg.add( bucketSum / internalSpectraQueue.size() );
				}

				// Add to average lists
				spectraAvg.add( spectrumAvg );
				loudnessAvg.add( rmsAvg );
			}
			log.atInfo().log( "Averaged data computed." );

			// Check if the spectra and loudness lists are the same size
			if( spectraAvg.size() != loudnessAvg.size() ) {
				throw new RuntimeException( "Spectra and loudness lists are not the same size" );
			}

			// Create the FFmpeg frame producer
			FFmpegFrameSequencer sequencer = new FFmpegFrameSequencer( videoData );

			// Setup FFmpeg to consume frames as they are rendered
			FFmpeg ffmpeg = FFmpeg.atPath();
			ffmpeg.addArgument( "-xerror" );
			ffmpeg.addArguments( "-loglevel", "info" );
			ffmpeg.addInput( FrameInput.withProducer( sequencer ) );
			//ffmpeg.addArguments( "-c:v", "ayuv" );
			ffmpeg.addOutput( UrlOutput.toUrl( "output.mp4" ) );
			FFmpegResultFuture future = ffmpeg.executeAsync();

			for( int index = 0; index < frameCount; index++ ) {
				//log.atConfig().log( "Rendering frame %d of %d", index, frameCount );
				submitTask( new FrameRenderTask( sequencer, index ).setPriority( Task.Priority.LOW ) );
			}

			// Wait for the FFmpeg process to complete
			future.get( 5, TimeUnit.MINUTES );
		} catch( Exception exception ) {
			throw new TaskException( exception );
		} finally {
			fireEvent( new ProjectProcessorEvent( this, ProjectProcessorEvent.Type.PROCESSING_COMPLETE ) );
		}
	}

	MusicData extractMusicData() throws IOException {
		try( stream ) {
			MusicData result = new MusicData( stream ).load();
			if( stream != null ) stream.close();
			return result;
		}
	}

	@Override
	public void pause() {
		reset();
	}

	@Override
	public void reset() {
		tasks.forEach( t -> t.cancel( true ) );
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
