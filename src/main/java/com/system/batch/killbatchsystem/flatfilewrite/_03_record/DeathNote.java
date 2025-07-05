package com.system.batch.killbatchsystem.flatfilewrite._03_record;

public record DeathNote(
        String victimId,
        String victimName,
        String executionDate,
        String causeOfDeath
) {}