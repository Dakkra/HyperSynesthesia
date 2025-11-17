package com.dakkra.hypersynesthesia.ffmpeg;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DSPTest {

    @Test
    void initialState_hasZeroPeakAndNullSpectrum() {
        DSP dsp = new DSP();
        assertEquals(0, dsp.getPeak());
        assertNull(dsp.getSpectrum());
        // RMS starts at 0
        assertEquals(0.0, dsp.getRMS(), 0.0);
    }

    @Test
    void processLight_computesPeakAndRms() {
        DSP dsp = new DSP();
        int[] samples = new int[]{0, -3, 4, -2};

        dsp.processLight(samples);

        assertEquals(4, dsp.getPeak(), "Peak loudness should be absolute max of samples");
        double expectedRms = Math.sqrt((9 + 16 + 4) / 4.0);
        assertEquals(expectedRms, dsp.getRMS(), 1e-12);

        // Loudness getters are normalized by Integer.MAX_VALUE
        assertEquals(dsp.getPeak() / (double) Integer.MAX_VALUE, dsp.getPeakLoudness(), 0.0);
        assertEquals(dsp.getRMS() / (double) Integer.MAX_VALUE, dsp.getRMSLoudness(), 0.0);
        assertNull(dsp.getSpectrum(), "Spectrum should remain null after processLight()");
    }

    @Test
    void processFull_producesNormalizedSpectrum() {
        DSP dsp = new DSP();

        // Use an impulse to get flat magnitude spectrum across bins
        int n = 32; // power of two for FFT
        int[] samples = new int[n];
        samples[0] = 1000;

        dsp.processFull(samples);

        double[] spectrum = dsp.getSpectrum();
        assertNotNull(spectrum, "Spectrum should not be null after processFull()");
        // Expect half-size minus the 4 skipped buckets
        assertEquals(n / 2 - 4, spectrum.length);

        // All values should be in [0, 1] and, for an impulse, equal after normalization
        double max = 0;
        for (double v : spectrum) {
            assertTrue(v >= 0.0 && v <= 1.0, "Spectrum values should be clamped to [0,1]");
            if (v > max) max = v;
        }
        assertEquals(1.0, max, 1e-12, "Spectrum should be normalized such that max is 1");

        // For impulse input, all remaining bins should normalize to 1
        for (double v : spectrum) {
            assertEquals(1.0, v, 1e-12);
        }
    }

    @Test
    void reset_clearsPeakAndSpectrum_butKeepsRms() {
        DSP dsp = new DSP();
        // Use a longer array so that processFull() can skip the first 4 buckets safely
        int[] samples = new int[32];
        Arrays.fill(samples, 2);
        dsp.processFull(samples);

        // Capture current RMS
        double rmsBefore = dsp.getRMS();
        assertNotNull(dsp.getSpectrum());
        assertTrue(dsp.getPeak() > 0);

        dsp.reset();

        assertEquals(0, dsp.getPeak(), "Peak should reset to 0");
        assertNull(dsp.getSpectrum(), "Spectrum should reset to null");
        // RMS is not reset by reset()
        assertEquals(rmsBefore, dsp.getRMS(), 0.0, "RMS should remain unchanged after reset()");
    }
}
