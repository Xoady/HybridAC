package com.hybridac.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern AMPERSAND_HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern MINI_HEX = Pattern.compile("(?i)<#([0-9a-f]{6})>");
    private static final Pattern MINI_CLOSE_HEX = Pattern.compile("(?i)</#([0-9a-f]{6})>");
    private static final String LEGACY_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String resolved = replaceHex(input, MINI_HEX);
        resolved = replaceHex(resolved, AMPERSAND_HEX);
        resolved = MINI_CLOSE_HEX.matcher(resolved).replaceAll(Matcher.quoteReplacement("§r"));
        resolved = resolved.replace("<reset>", "§r");
        return translateAmpersandCodes(resolved);
    }

    public static String colorize(String input, Map<String, String> placeholders) {
        String resolved = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return colorize(resolved);
    }

    private static String replaceHex(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = toLegacyHex(matcher.group(1));
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String toLegacyHex(String hex) {
        String normalized = hex.toUpperCase();
        StringBuilder builder = new StringBuilder("§x");
        for (char character : normalized.toCharArray()) {
            builder.append('§').append(character);
        }
        return builder.toString();
    }

    private static String translateAmpersandCodes(String input) {
        char[] characters = input.toCharArray();
        for (int index = 0; index < characters.length - 1; index++) {
            if (characters[index] == '&' && LEGACY_CODES.indexOf(characters[index + 1]) >= 0) {
                characters[index] = '§';
            }
        }
        return new String(characters);
    }
}
