package com.dakkra.hypersynesthesia;

import com.tambapps.fft4j.FastFouriers;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

public class DSP {

	@Getter
	private int index;

	private int peakLoudness;

	private double rms;

	@Getter
	private double[] spectrum ;

	// Skips the fft
	public DSP processLight(int index,  int[] samples ) {
		this.index = index;

		for( int sample : samples ) {
			if( Math.abs( sample ) > peakLoudness ) {
				peakLoudness = Math.abs( sample );
			}
		}

		// Calculate RMS
		double sum = 0;
		for( int sample : samples ) {
			sum += ((double)sample * (double)sample);
		}
		rms = Math.sqrt( sum / samples.length );

		return this;
	}

	public DSP processFull( int index, int[] samples ) {
		processLight( index, samples );

		// Calculate FFT
		double[] real = new double[ samples.length ];
		double[] imag = new double[ samples.length ];
		for( int i = 0; i < samples.length; i++ ) {
			real[ i ] = samples[ i ];
			imag[ i ] = 0;
		}

		double[] outputReal = new double[ samples.length ];
		double[] outputImag = new double[ samples.length ];
		FastFouriers.BASIC.transform( real, imag, outputReal, outputImag );

		// Create spectrum
		spectrum = new double[ samples.length / 2 ];
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] = Math.sqrt( outputReal[ i ] * outputReal[ i ] + outputImag[ i ] * outputImag[ i ] );
		}

		// Remove bottom few buckets
		int skippedBuckets = 4;
		spectrum = new ArrayList<>( Arrays.asList( Arrays.stream( spectrum ).boxed().toArray( Double[]::new ) ) ).subList( skippedBuckets, spectrum.length ).stream().mapToDouble( i -> i ).toArray();
		// Convert to dB
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] = 20 * Math.log10( spectrum[ i ] );
		}

		// Compute max
		double max = 0;
		for( double value : spectrum ) {
			if( value > max ) {
				max = value;
			}
		}

		// Expand spectrum
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] = spectrum[ i ] - 0.85 * max;
		}

		// Remove negative values
		for( int i = 0; i < spectrum.length; i++ ) {
			if( spectrum[ i ] < 0 ) {
				spectrum[ i ] = 0;
			}
		}

		// Compute max again
		max = 0;
		for( double value : spectrum ) {
			if( value > max ) {
				max = value;
			}
		}

		// Normalize spectrum
		for( int i = 0; i < spectrum.length; i++ ) {
			spectrum[ i ] /= max;
		}

		return this;
	}

	public int getPeak() {
		return peakLoudness;
	}

	public double getRMS() {
		return rms;
	}

	public double getRMSLoudness() {
		return rms / (double)Integer.MAX_VALUE;
	}

	public double getPeakLoudness() {
		return (double)peakLoudness / (double)Integer.MAX_VALUE;
	}

	public void reset() {
		peakLoudness = 0;
		spectrum = null;
	}

}
