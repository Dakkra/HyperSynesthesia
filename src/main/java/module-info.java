import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires static lombok;

	requires com.avereon.xenon;
	requires com.github.kokorin.jaffree;
	requires fft4j;
	requires java.logging;
	requires org.slf4j;

	opens com.dakkra.hypersynesthesia.bundles;
	//opens com.dakkra.hypersynesthesia.settings;
	opens com.dakkra.hypersynesthesia to org.testfx.junit5;

	exports com.dakkra.hypersynesthesia to com.avereon.xenon;
	provides com.avereon.xenon.Mod with HyperSynesthesia;
}
