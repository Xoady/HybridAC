package com.hybridac.model;

import java.util.List;
import java.util.UUID;

public record AttackWindow(UUID playerUuid, int requestedSize, List<HitSample> hits) {
}
