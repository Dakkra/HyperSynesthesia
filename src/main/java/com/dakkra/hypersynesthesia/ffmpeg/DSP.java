package com.dakkra.hypersynesthesia.ffmpeg;

import com.tambapps.fft4j.FastFouriers;
import java.util.ArrayList;
import java.util.Arrays;

class DSP {

    private int peakLoudness = 0;

    private double rms = 0;

    private double[] spectrum = null;

    // Skips the fft
    public void processLight(int[] samples) {
        final int fftSize = samples.length;

        double sum = 0;

        for (int i = 0; i < fftSize; i++) {
            int sample = samples[i];

            int absSample = Math.abs(sample);
            if (absSample > peakLoudness) {
                peakLoudness = absSample;
            }

            // Calculate RMS
            double d = sample;
            sum += d * d;
        }

        rms = Math.sqrt(sum / fftSize);
    }

    public void processFull(int[] samples) {
        processLight(samples);

        // samples.length is basically FFT size
        final int fftSize = samples.length;

        // Calculate FFT
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];

        for (int i = 0; i < fftSize; i++) {
            real[i] = samples[i];
        }

        // for (int i = 0; i < fftSize; i++) imag[i] = 0;

        double[] outputReal = new double[fftSize];
        double[] outputImag = new double[fftSize];

        FastFouriers.BASIC.transform(real, imag, outputReal, outputImag);

        // Create spectrum
        // In the future HS may have customizable FFT size
        spectrum = new double[(int) Math.floor(fftSize / 2)];

        final int halfFftSize = spectrum.length;

        for (int i = 0; i < halfFftSize; i++) {
            double realValue = outputReal[i];
            double imagValue = outputImag[i];

            spectrum[i] = Math.sqrt(realValue * realValue + imagValue * imagValue);
        }

        // Remove bottom few ~buckets~ FFT bin
        int skippedBuckets = 4;
        spectrum = Arrays.copyOfRange(spectrum, skippedBuckets, halfFftSize);

        // Convert to dB

        for (int i = 0; i < halfFftSize; i++) {
            spectrum[i] = 20 * Math.log10(spectrum[i]);
        }

        // Compute max
        double max = 0;
        for (int i = 0; i < halfFftSize; i++) {
            double value = spectrum[i];

            if (value > max) {
                max = value;
            }
        }

        // Expand spectrum
        for (int i = 0; i < halfFftSize; i++) {
            spectrum[i] = spectrum[i] - 0.85 * max;
        }

        // ~Remove negative values~
        // `spectrum` array always have positive values because we square real and imag
        // values
        /*
         * for (int i = 0; i < halfFftSize; i++) {
         * if (spectrum[i] < 0) {
         * spectrum[i] = 0;
         * }
         * }
         */

        // Compute max again
        max = 0;
        for (int i = 0; i < halfFftSize; i++) {
            double value = spectrum[i];

            if (value > max) {
                max = value;
            }
        }

        // Normalize spectrum
        for (int i = 0; i < halfFftSize; i++) {
            spectrum[i] /= max;
        }
    }

    public int getPeak() {
        return peakLoudness;
    }

    public double getRMS() {
        return rms;
    }

    public double[] getSpectrum() {
        return spectrum;
    }

    public double getRMSLoudness() {
        return (double) rms / Integer.MAX_VALUE;
    }

    public double getPeakLoudness() {
        return (double) peakLoudness / Integer.MAX_VALUE;
    }

    public void reset() {
        peakLoudness = 0;
        spectrum = null;
    }
}
