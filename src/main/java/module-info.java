import com.avereon.xenon.Module;
import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires com.avereon.xenon;
	requires static lombok;
	requires com.github.kokorin.jaffree;
	requires org.slf4j;
	requires fft4j;

	exports com.dakkra.hypersynesthesia to com.avereon.xenon;
	provides Module with HyperSynesthesia;
	opens com.dakkra.hypersynesthesia to org.testfx.junit5;
}
