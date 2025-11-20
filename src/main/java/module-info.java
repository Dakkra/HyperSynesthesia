import com.avereon.xenon.Module;
import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires static lombok;

	requires com.avereon.xenon;
	requires com.github.kokorin.jaffree;
	requires fft4j;
	requires org.slf4j;
	requires org.jspecify;
	requires javafx.graphics;
	requires com.avereon.zevra;
	requires java.desktop;

	exports com.dakkra.hypersynesthesia to com.avereon.xenon;

	// Public resources
	opens com.dakkra.hypersynesthesia.bundles;
	exports com.dakkra.hypersynesthesia.bar to com.avereon.xenon;
	//opens com.dakkra.hypersynesthesia.settings;

	provides Module with HyperSynesthesia;
}
