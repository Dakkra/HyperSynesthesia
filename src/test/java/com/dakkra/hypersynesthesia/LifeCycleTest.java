package com.dakkra.hypersynesthesia;

import com.avereon.product.ProductCard;
import com.avereon.xenon.ModStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
		initMod( ProductCard.card( HyperSynesthesia.class ) );
		assertNotNull( getMod().getCard() );
	}

	@Test
	void testModCardName() {
		initMod( ProductCard.card( HyperSynesthesia.class ) );
		assertEquals( "HyperSynesthesia", getMod().getCard().getName() );
	}

  @Test
	void testModCardArtifact() {
		initMod( ProductCard.card( HyperSynesthesia.class ) );
		assertEquals( "hypersynesthesia", getMod().getCard().getArtifact() );
	}
}
