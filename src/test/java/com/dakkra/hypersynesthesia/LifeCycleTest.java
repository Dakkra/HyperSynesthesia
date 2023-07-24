package com.dakkra.hypersynesthesia;

import com.avereon.xenon.ModStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LifeCycleTest extends HyperSynesthesiaTest{

	@Test
	void testModLfeCycle() {
		assertThat(getProgram().getProductManager().isModEnabled( getMod() )).isTrue();
		assertThat( getMod().getStatus() ).isEqualTo( ModStatus.STARTED );

		getProgram().getProductManager().setModEnabled( getMod().getCard(), false );

		assertThat( getProgram().getProductManager().isModEnabled( getMod() ) ).isFalse();
		assertThat( getMod().getStatus() ).isEqualTo( ModStatus.STOPPED );
	}

	@Test
	void testModHasCardInfo() {
		assertNotNull( getMod().getCard() );
	}

	@Test
	void testModCardName() {
		assertEquals( "HyperSynesthesia", getMod().getCard().getName() );
	}

  @Test
	void testModCardArtifact() {
		assertEquals( "hypersynesthesia", getMod().getCard().getArtifact() );
	}

}
