package com.dakkra.hypersynesthesia;

import com.avereon.xenon.task.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;

@Timeout( 10 )
@ExtendWith( org.mockito.junit.jupiter.MockitoExtension.class )
class ProjectProcessorTest {

	private ProjectProcessor processor;

	@BeforeEach
	public void setUp() {
		TaskManager taskManager = new TaskManager().start();
		InputStream inputStream = getClass().getResourceAsStream( "samples/DemoTrack.wav" );
		processor = new ProjectProcessor( taskManager, inputStream );
	}

	@Test
	public void testExtractMusicData() throws Exception {
		processor.extractMusicData();
	}

	@Test
	public void testProcessProject() {
		processor.processProject();
	}

}
