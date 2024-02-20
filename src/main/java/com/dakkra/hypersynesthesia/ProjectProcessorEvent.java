package com.dakkra.hypersynesthesia;

import lombok.Getter;

@Getter
class ProjectProcessorEvent {

	public enum Type {
		PROCESSING_COMPLETE;
	}

	private final ProjectProcessor source;

	private final Type type;

	public ProjectProcessorEvent( ProjectProcessor source, Type type ) {
		this.source = source;
		this.type = type;
	}

}
