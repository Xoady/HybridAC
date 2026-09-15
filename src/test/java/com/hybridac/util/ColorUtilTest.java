package com.hybridac.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorUtilTest {

    @Test
    void supportsAmpersandHexFormat() {
        String colored = ColorUtil.colorize("&#12abEFTest");
        assertTrue(colored.startsWith("\u00A7x"));
    }

    @Test
    void supportsMiniMessageLikeHexFormat() {
        String colored = ColorUtil.colorize("<#12abEF>Test</#12abEF>");
        assertTrue(colored.startsWith("\u00A7x"));
    }

    @Test
    void supportsLegacyExpandedHexFormat() {
        String colored = ColorUtil.colorize("&x&1&2&a&b&E&FTest");
        assertTrue(colored.startsWith("\u00A7x"));
    }
}
