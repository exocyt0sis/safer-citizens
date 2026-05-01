package com.safercitizens;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.util.Set;

@Mod(SaferCitizensMod.MOD_ID)
public final class SaferCitizensMod {
    public static final String MOD_ID = "safercitizens";
    static final Logger LOGGER = LogUtils.getLogger();

    public SaferCitizensMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SaferCitizensConfig.SPEC, "safercitizens.toml");
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onMobPositionCheck(MobSpawnEvent.PositionCheck event) {
            if (!(event.getEntity() instanceof Enemy)) {
                return;
            }

            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }

            if (!isProtectedSpawnType(event.getSpawnType())) {
                return;
            }

            Mob mob = event.getEntity();
            Set<EntityType<?>> affectedTypes = SaferCitizensConfig.affectedEntityTypes();
            if (affectedTypes.isEmpty() || !affectedTypes.contains(mob.getType())) {
                return;
            }

            BlockPos spawnPos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
            if (SaferCitizensConfig.alwaysFendWithinColonies() && MineColoniesCompat.isInsideColony(level, spawnPos)) {
                denySpawn(event, mob.getType(), spawnPos, "inside colony");
                return;
            }

            Set<EntityType<?>> fendedTypes = SaferCitizensConfig.fendedEntityTypes();
            if (fendedTypes.isEmpty()) {
                return;
            }

            int radius = SaferCitizensConfig.fendedRadius();
            AABB searchBox = new AABB(spawnPos).inflate(radius);
            boolean protectedEntityNearby = level.getEntities((Entity) null, searchBox, entity -> fendedTypes.contains(entity.getType()))
                .stream()
                .findAny()
                .isPresent();

            if (protectedEntityNearby) {
                denySpawn(event, mob.getType(), spawnPos, "near protected entity");
            }
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            LOGGER.info("Styx's Safer Citizens started.");

            if (SaferCitizensConfig.consoleMessages()) {
                LOGGER.info("Active config: {}.", SaferCitizensConfig.describeLoadedConfig());
            }

            if (!MineColoniesCompat.isInstalled()) {
                LOGGER.warn("MineColonies is not installed. Colony boundary checks are disabled.");

                if (SaferCitizensConfig.alwaysFendWithinColonies()) {
                    LOGGER.warn("AlwaysFendWithinColonies is enabled, but MineColonies is missing, so that setting will be ignored.");
                }

                if (SaferCitizensConfig.fendedEntityTypes().isEmpty()) {
                    LOGGER.warn("No configured FendedEntities are currently available. If you are not using MineColonies, update FendedEntities in safercitizens.toml to target entities present in your modpack.");
                }
            }
        }

        private static void denySpawn(MobSpawnEvent.PositionCheck event, EntityType<?> type, BlockPos pos, String reason) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);

            if (SaferCitizensConfig.consoleMessages()) {
                LOGGER.info("Prevented spawn of {} at {} because {}.", entityId(type), pos, reason);
            }
        }

        private static boolean isProtectedSpawnType(MobSpawnType spawnType) {
            return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.SPAWNER;
        }

        private static ResourceLocation entityId(EntityType<?> type) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        }
    }
}