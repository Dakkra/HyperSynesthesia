import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires com.avereon.xenon;
	requires static lombok;
	requires com.github.kokorin.jaffree;
	requires org.slf4j;
	exports com.dakkra.hypersynesthesia to com.avereon.xenon;
	provides com.avereon.xenon.Mod with HyperSynesthesia;
	opens com.dakkra.hypersynesthesia to org.testfx.junit5;
}