package com.dakkra.hypersynesthesia;

import com.avereon.product.Rb;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public record Option<T>(T key, String name) {

	@NonNull
	@Override
	public String toString() {
		return name;
	}

	public static <T> List<Option<T>> ofEnum( T[] options ) {
		List<Option<T>> optionList = new ArrayList<>();
		for( T option : options ) {
			String name = option.toString();
			if( option instanceof Enum<?> enumOption ) {

				name = enumOption.getClass().getSimpleName().toLowerCase() + "-" + enumOption.name().toLowerCase().replace( '_', '-' );
			}
			optionList.add( new Option<>( option, Rb.text( "tool", name ) ) );
		}
		return optionList;
	}

}
