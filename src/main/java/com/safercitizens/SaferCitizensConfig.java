package com.safercitizens;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SaferCitizensConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CONSOLE_MESSAGES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FENDED_ENTITIES;
    public static final ModConfigSpec.IntValue FENDED_RADIUS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AFFECTED_ENTITIES;
    public static final ModConfigSpec.BooleanValue ALWAYS_FEND_WITHIN_COLONIES;

    private static volatile List<String> cachedFendedEntityIds = List.of();
    private static volatile List<String> cachedAffectedEntityIds = List.of();
    private static volatile Set<EntityType<?>> cachedFendedEntityTypes = Set.of();
    private static volatile Set<EntityType<?>> cachedAffectedEntityTypes = Set.of();
    private static final Set<String> loggedInvalidIds = ConcurrentHashMap.newKeySet();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        CONSOLE_MESSAGES = builder
            .comment(
                "General settings.",
                "If true, logs startup and fending events to console."
            )
            .define("ConsoleMessages", false);

        FENDED_ENTITIES = builder
            .comment(
                "Protected entity ids.",
                "Configured hostile spawns are blocked near these entities."
            )
            .defineListAllowEmpty(
                List.of("FendedEntities"),
                List.of("minecolonies:citizen", "minecolonies:sitting_entity"),
                SaferCitizensConfig::isResourceLocation
            );

        FENDED_RADIUS = builder
            .comment(
                "Protection radius in blocks.",
                "Configured hostile spawns are blocked inside this radius around protected entities."
            )
            .defineInRange("FendedRadius", 24, 1, 128);

        AFFECTED_ENTITIES = builder
            .comment(
                "Hostile entity ids affected by this mod.",
                "Only these entity types are prevented from spawning."
            )
            .defineListAllowEmpty(
                List.of("AffectedEntities"),
                List.of("minecraft:creeper"),
                SaferCitizensConfig::isResourceLocation
            );

        ALWAYS_FEND_WITHIN_COLONIES = builder
            .comment(
                "MineColonies-only setting.",
                "If true, configured hostile spawns are always blocked inside colony borders."
            )
            .define("AlwaysFendWithinColonies", false);

        SPEC = builder.build();
    }

    private SaferCitizensConfig() {
    }

    public static int fendedRadius() {
        return FENDED_RADIUS.get();
    }

    public static boolean consoleMessages() {
        return CONSOLE_MESSAGES.get();
    }

    public static boolean alwaysFendWithinColonies() {
        return ALWAYS_FEND_WITHIN_COLONIES.get();
    }

    public static Set<EntityType<?>> fendedEntityTypes() {
        List<String> configuredIds = normalizedIds(FENDED_ENTITIES.get());
        if (!configuredIds.equals(cachedFendedEntityIds)) {
            synchronized (SaferCitizensConfig.class) {
                configuredIds = normalizedIds(FENDED_ENTITIES.get());
                if (!configuredIds.equals(cachedFendedEntityIds)) {
                    cachedFendedEntityIds = configuredIds;
                    cachedFendedEntityTypes = resolveEntityTypes(configuredIds, "FendedEntities");
                }
            }
        }

        return cachedFendedEntityTypes;
    }

    public static Set<EntityType<?>> affectedEntityTypes() {
        List<String> configuredIds = normalizedIds(AFFECTED_ENTITIES.get());
        if (!configuredIds.equals(cachedAffectedEntityIds)) {
            synchronized (SaferCitizensConfig.class) {
                configuredIds = normalizedIds(AFFECTED_ENTITIES.get());
                if (!configuredIds.equals(cachedAffectedEntityIds)) {
                    cachedAffectedEntityIds = configuredIds;
                    cachedAffectedEntityTypes = resolveEntityTypes(configuredIds, "AffectedEntities");
                }
            }
        }

        return cachedAffectedEntityTypes;
    }

    private static List<String> normalizedIds(List<? extends String> values) {
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }

    private static Set<EntityType<?>> resolveEntityTypes(List<String> ids, String configKey) {
        Set<EntityType<?>> resolved = new LinkedHashSet<>();

        for (String rawId : ids) {
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id == null) {
                logInvalidId(configKey, rawId);
                continue;
            }

            BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .ifPresentOrElse(resolved::add, () -> logInvalidId(configKey, rawId));
        }

        return Set.copyOf(resolved);
    }

    private static boolean isResourceLocation(Object value) {
        return value instanceof String text && ResourceLocation.tryParse(text) != null;
    }

    private static void logInvalidId(String configKey, String rawId) {
        String signature = configKey + ":" + rawId;
        if (loggedInvalidIds.add(signature)) {
            SaferCitizensMod.LOGGER.warn("Ignoring unknown entity id '{}' from {}.", rawId, configKey);
        }
    }

    public static String describeLoadedConfig() {
        String fended = cachedOrConfigured(FENDED_ENTITIES.get());
        String affected = cachedOrConfigured(AFFECTED_ENTITIES.get());
        return "ConsoleMessages=" + consoleMessages()
            + ", FendedEntities=" + fended
            + ", FendedRadius=" + fendedRadius()
            + ", AffectedEntities=" + affected
            + ", AlwaysFendWithinColonies=" + alwaysFendWithinColonies();
    }

    private static String cachedOrConfigured(List<? extends String> values) {
        return values.stream().collect(Collectors.joining(",", "[", "]"));
    }
}