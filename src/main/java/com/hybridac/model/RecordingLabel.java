package com.hybridac.model;

public enum RecordingLabel {
    LEGIT("legit"),
    CHEAT("cheat");

    private final String apiValue;

    RecordingLabel(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static RecordingLabel fromArgument(String argument) {
        if (argument == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        return switch (argument.toLowerCase().trim()) {
            case "legit" -> LEGIT;
            case "cheat", "cheats", "cheater" -> CHEAT;
            default -> throw new IllegalArgumentException("Unknown label: " + argument);
        };
    }
}
