package com.hybridac.config;

import java.util.List;

public record PunishmentThresholdSettings(double threshold, List<PunishmentActionSettings> actions) {
}
