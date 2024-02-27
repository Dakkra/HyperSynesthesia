package com.dakkra.hypersynesthesia;

import com.avereon.xenon.task.TaskManager;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;
import java.util.PriorityQueue;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout( 30 )
@ExtendWith( org.mockito.junit.jupiter.MockitoExtension.class )
@CustomLog
class ProjectProcessorTest {

	private ProjectProcessor processor;

	@BeforeEach
	public void setUp() {
		TaskManager taskManager = new TaskManager().start();
		InputStream inputStream = getClass().getResourceAsStream( "samples/ding.mp3" );
		processor = new ProjectProcessor( taskManager, inputStream );
	}

	@Test
	public void testExtractMusicData() throws Exception {
		MusicData musicData = processor.extractMusicData();
		assertThat( musicData.getSampleCount() ).isEqualTo( 31680 );
		assertThat( musicData.getSamplesLeft().size() ).isEqualTo( 31680 );
	}

	@Test
	public void testComputeAverages() {
		DSP dsp = new DSP();

		// Probably not enough data to get good averages
		dsp.processFull( 0, new int[]{ 0, 10, 20, 30, 40, 30, 20, 10, 0 } );

		Queue<PrioritySpectrum> spectrumQueue = new PriorityQueue<>();
		spectrumQueue.add( new PrioritySpectrum( dsp ) );

		Queue<PriorityLoudness> loudnessQueue = new PriorityQueue<>();
		loudnessQueue.add( new PriorityLoudness( dsp ) );

		MusicData musicData = new MusicData( null );
		processor.computeAverages( musicData, loudnessQueue, spectrumQueue );

		assertThat( musicData.getSpectraAverage() ).isNotNull();
		assertThat( musicData.getLoudnessAverage() ).isNotNull();
	}

	@Test
	//@Disabled( "Invalid data found when processing input" )
	public void testProcessProject() {
		processor.processProject();
	}

}
