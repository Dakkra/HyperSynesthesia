package com.dakkra.hypersynesthesia;

import com.avereon.product.ProductCard;
import com.avereon.xenon.ModStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class LifeCycleTest extends HyperSynesthesiaTest{

	@BeforeEach
	void init() {
		initMod( ProductCard.card( HyperSynesthesia.class ) );
	}

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
