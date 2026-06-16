package com.googlecode.jthaipdf.util;

import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.MAI_EK;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.MAI_EK_DOWN;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.NIKHAHIT;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.PO_PLA;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.SARA_AA;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.SARA_AM;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.SARA_I;
import static com.googlecode.jthaipdf.util.ThaiDisplayUtils.SARA_I_LEFT_SHIFT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the Thai glyph-rearrangement algorithm. The
 * algorithm itself is unchanged by the JasperReports 7.x port, so these lock in
 * its behaviour as a regression guard. Each case is hand-traced against the
 * rules in {@link ThaiDisplayUtils}.
 */
public class ThaiDisplayUtilsTest {

    private static final char KO_KAI = 'ก'; // ก — plain base consonant, no tail

    /** Non-Thai text passes through untouched. */
    @Test
    public void leavesAsciiUnchanged() {
        assertEquals("Hello", ThaiDisplayUtils.toDisplayString("Hello"));
    }

    /** Plain consonants with no combining marks are untouched. */
    @Test
    public void leavesPlainConsonantsUnchanged() {
        String input = "กขคง"; // กขคง
        assertEquals(input, ThaiDisplayUtils.toDisplayString(input));
    }

    /** SARA_AM (ำ) explodes into NIKHAHIT + SARA_AA, growing length by one. */
    @Test
    public void explodesSaraAm() {
        String input = "" + KO_KAI + SARA_AM;
        String expected = "" + KO_KAI + NIKHAHIT + SARA_AA;
        assertEquals(expected, ThaiDisplayUtils.toDisplayString(input));
        assertEquals(input.length() + 1, ThaiDisplayUtils.toDisplayString(input).length());
    }

    /** An upper-level-2 tone mark over a plain base is pulled down. */
    @Test
    public void pullsDownToneMarkOverPlainBase() {
        String input = "" + KO_KAI + MAI_EK;
        String expected = "" + KO_KAI + MAI_EK_DOWN;
        assertEquals(expected, ThaiDisplayUtils.toDisplayString(input));
    }

    /** An upper-level-1 vowel over an up-tail consonant is shifted left. */
    @Test
    public void shiftsLeftOverUpTailConsonant() {
        String input = "" + PO_PLA + SARA_I; // ป + sara i
        String expected = "" + PO_PLA + SARA_I_LEFT_SHIFT;
        assertEquals(expected, ThaiDisplayUtils.toDisplayString(input));
    }
}
