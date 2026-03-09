import com.acromere.xenon.Module;
import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires static lombok;

	requires com.acromere.xenon;
	requires com.github.kokorin.jaffree;
	requires fft4j;
	requires org.slf4j;
	requires org.jspecify;
	requires javafx.graphics;
	requires com.acromere.zevra;
	requires java.desktop;

	exports com.dakkra.hypersynesthesia to com.acromere.xenon;

	// Public resources
	opens com.dakkra.hypersynesthesia.bundles;
	exports com.dakkra.hypersynesthesia.bar to com.acromere.xenon;
	//opens com.dakkra.hypersynesthesia.settings;

	provides Module with HyperSynesthesia;
}
