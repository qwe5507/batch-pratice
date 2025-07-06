package com.system.batch.killbatchsystem.json.write;

public record DeathNote(
        String victimId,
        String victimName,
        String executionDate,
        String causeOfDeath
) {}