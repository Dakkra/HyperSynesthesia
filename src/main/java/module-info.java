import com.dakkra.hypersynesthesia.HyperSynesthesia;

module com.dakkra.hypersynthesthesia {
	requires com.avereon.xenon;
	requires static lombok;
	exports com.dakkra.hypersynesthesia to com.avereon.xenon;
	provides com.avereon.xenon.Mod with HyperSynesthesia;
}