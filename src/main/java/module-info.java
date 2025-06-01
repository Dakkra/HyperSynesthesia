import com.avereon.xenon.Module;
import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires static lombok;

	requires com.avereon.xenon;
	requires com.github.kokorin.jaffree;
	requires fft4j;
	requires org.slf4j;

	exports com.dakkra.hypersynesthesia to com.avereon.xenon;

	provides Module with HyperSynesthesia;
}
