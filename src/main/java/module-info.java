import com.avereon.xenon.Module;
import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires static lombok;

	requires com.avereon.xenon;
	requires com.github.kokorin.jaffree;
	requires fft4j;
	requires org.slf4j;
	requires org.jspecify;

	exports com.dakkra.hypersynesthesia to com.avereon.xenon;

	// Public resources
	opens com.dakkra.hypersynesthesia.bundles;
	//opens com.dakkra.hypersynesthesia.settings;

	provides Module with HyperSynesthesia;
}
