// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

/**
 * How a synthesised drum kit sounds. Times are decay constants in seconds, pitches in Hz;
 * [room] lists early reflections as (delay in seconds, gain).
 */
data class KitSound(
    val kickLow: Double = 50.0,
    val kickSweep: Double = 70.0,
    val kickSweepTime: Double = 0.04,
    val kickDecay: Double = 0.12,
    val kickBeater: Double = 0.15,
    val kickLength: Double = 0.3,
    val snareTone: Double = 190.0,
    val snareToneDecay: Double = 0.03,
    val snareToneGain: Double = 0.4,
    val snareNoiseDecay: Double = 0.045,
    val snareNoiseGain: Double = 0.6,
    /** 0 = full noise, 1 = only the high, crackling part. */
    val snareBrightness: Double = 0.0,
    val snareLength: Double = 0.18,
    val hatClosedDecay: Double = 0.018,
    val hatOpenDecay: Double = 0.12,
    /** 0 = bright, 1 = dark and washy. */
    val hatDarkness: Double = 0.0,
    val ridePitch: Double = 1.0,
    val rideDecay: Double = 0.35,
    val rideWash: Double = 0.2,
    val stickTone: Double = 1_750.0,
    val stickDecay: Double = 0.012,
    val tomLow: Double = 105.0,
    val tomDecay: Double = 0.15,
    val bellLow: Double = 545.0,
    val bellHigh: Double = 815.0,
    val bellDecay: Double = 0.06,
    val room: List<Pair<Double, Double>> = emptyList(),
) {
    companion object {
        /** The app's original kit, also used for the rudiments. */
        val STANDARD = KitSound()

        /** One sound per music style, by the name used in styles.json. */
        val PRESETS: Map<String, KitSound> = mapOf(
            "standard" to STANDARD,
            // Punchy kick, fat snare with some ring, a small live room.
            "rock" to KitSound(
                kickLow = 52.0, kickSweep = 90.0, kickDecay = 0.14, kickBeater = 0.2,
                snareTone = 185.0, snareToneGain = 0.45, snareNoiseDecay = 0.07, snareBrightness = 0.3, snareLength = 0.28,
                hatClosedDecay = 0.022,
                room = listOf(0.011 to 0.25, 0.019 to 0.18, 0.031 to 0.12),
            ),
            // Deep, tight kick, snappy bright snare, crisp short hi-hat, almost dry.
            "pop" to KitSound(
                kickLow = 45.0, kickSweep = 75.0, kickDecay = 0.2, kickBeater = 0.12, kickLength = 0.4,
                snareTone = 210.0, snareNoiseDecay = 0.05, snareNoiseGain = 0.65, snareBrightness = 0.6,
                hatClosedDecay = 0.014,
                room = listOf(0.008 to 0.12),
            ),
            // Raw and trashy: short kick, loud bright snare, sloshy hi-hat, garage room.
            "punk" to KitSound(
                kickLow = 58.0, kickDecay = 0.1, kickBeater = 0.25,
                snareTone = 200.0, snareNoiseDecay = 0.09, snareNoiseGain = 0.7, snareBrightness = 0.5, snareLength = 0.3,
                hatClosedDecay = 0.03, hatOpenDecay = 0.18,
                room = listOf(0.013 to 0.3, 0.023 to 0.22, 0.037 to 0.15),
            ),
            // Clicky, very short kick for fast double bass, high tight snare, brighter ride.
            "metal" to KitSound(
                kickLow = 62.0, kickSweep = 110.0, kickSweepTime = 0.02, kickDecay = 0.07, kickBeater = 0.4, kickLength = 0.18,
                snareTone = 240.0, snareToneGain = 0.5, snareNoiseDecay = 0.05, snareBrightness = 0.7,
                ridePitch = 1.15, rideDecay = 0.3,
                room = listOf(0.009 to 0.15),
            ),
            // Soft jazz kit: felt kick, brushy snare, dark hi-hat and ride, warm woody cross-stick.
            "bossa" to KitSound(
                kickLow = 48.0, kickSweep = 40.0, kickDecay = 0.1, kickBeater = 0.03,
                snareTone = 220.0, snareToneGain = 0.2, snareNoiseDecay = 0.1, snareNoiseGain = 0.35, snareBrightness = 0.2,
                hatClosedDecay = 0.025, hatDarkness = 0.5,
                ridePitch = 0.8, rideDecay = 0.5, rideWash = 0.3,
                stickTone = 1_350.0, stickDecay = 0.018,
                room = listOf(0.015 to 0.2, 0.027 to 0.14),
            ),
            // Cuban percussion colours: timbale-like snare, conga-like tom, bright cowbell.
            "songo" to KitSound(
                kickLow = 55.0, kickDecay = 0.11,
                snareTone = 330.0, snareToneDecay = 0.08, snareToneGain = 0.55, snareNoiseDecay = 0.035,
                tomLow = 170.0, tomDecay = 0.12,
                bellLow = 600.0, bellHigh = 900.0, bellDecay = 0.09,
                room = listOf(0.012 to 0.18),
            ),
            // Deep round kick, high cracking snare and rim, tight hi-hat, a spring-like echo.
            "reggae" to KitSound(
                kickLow = 42.0, kickSweep = 50.0, kickDecay = 0.26, kickBeater = 0.04, kickLength = 0.5,
                snareTone = 280.0, snareToneGain = 0.5, snareNoiseDecay = 0.04, snareBrightness = 0.8,
                hatClosedDecay = 0.015,
                stickTone = 1_950.0, stickDecay = 0.01,
                room = listOf(0.035 to 0.25, 0.071 to 0.16, 0.11 to 0.1),
            ),
            // Bright scraper-like hi-hat, sharp rim, soft kick, small room.
            "cumbia" to KitSound(
                kickLow = 50.0, kickBeater = 0.08,
                hatClosedDecay = 0.012, hatOpenDecay = 0.1,
                stickTone = 2_100.0, stickDecay = 0.009,
                bellLow = 650.0, bellHigh = 980.0,
                room = listOf(0.01 to 0.15),
            ),
        )
    }
}
