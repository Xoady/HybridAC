package com.hybridac.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MathUtilTest {

    @Test
    void wrapsPositiveAngleAcrossBoundary() {
        assertEquals(2.0D, MathUtil.wrapAngleTo180(362.0D), 1.0E-9D);
        assertEquals(-2.0D, MathUtil.wrapAngleTo180(-362.0D), 1.0E-9D);
    }

    @Test
    void wrapsYawDifferenceIntoShortestTurn() {
        assertEquals(2.0D, MathUtil.wrapAngleTo180(-179.0D - 179.0D), 1.0E-9D);
        assertEquals(-2.0D, MathUtil.wrapAngleTo180(179.0D - -179.0D), 1.0E-9D);
    }
}
