package com.dakkra.hypersynesthesia.task;

import com.avereon.xenon.task.Task;
import com.dakkra.hypersynesthesia.DSP;
import com.dakkra.hypersynesthesia.MusicData;

public class FfftComputeTask extends Task<DSP> {

	private final MusicData music;

	private final int index;

	private final int audioBufferSize;

	private final int delta;

	public FfftComputeTask( MusicData music, int index ) {
		this.music = music;
		this.index = index;
		this.audioBufferSize = (int)(music.getSampleRate() / 60);
		this.delta = Math.min( audioBufferSize, music.getSamplesAvg().size() - index * audioBufferSize );
	}

	@Override
	public DSP call() {
		int[] samplesAvg = music.getSamplesAvg().subList( index * audioBufferSize, index * audioBufferSize + delta ).stream().mapToInt( i -> i ).toArray();
		DSP dsp = new DSP();
		dsp.processFull( samplesAvg );
		//fftQueue.offer( new PrioritySpectrum( new ArrayList<>( Arrays.asList( Arrays.stream( dsp.getSpectrum() ).boxed().toArray( Double[]::new ) ) ), index ) );
		//loudnessQueue.offer( new PriorityLoudness( dsp.getRMSLoudness(), index ) );
		return dsp;
	}

}
