package com.safercitizens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Arrays;

final class MineColoniesCompat {
    private static volatile boolean initialized;
    private static volatile boolean available;
    private static Method getManagerMethod;
    private static Method getClosestColonyMethod;
    private static Method isCoordInColonyMethod;

    private MineColoniesCompat() {
    }

    static boolean isInstalled() {
        return ModList.get().isLoaded("minecolonies");
    }

    static boolean isInsideColony(ServerLevel level, BlockPos pos) {
        if (!isInstalled()) {
            return false;
        }

        initialize();
        if (!available) {
            return false;
        }

        try {
            Object manager = getManagerMethod.invoke(null);
            if (manager == null) {
                return false;
            }

            Object colony = getClosestColonyMethod.invoke(manager, level, pos);
            if (colony == null) {
                return false;
            }

            Object result = isCoordInColonyMethod.invoke(colony, level, pos);
            return result instanceof Boolean inside && inside;
        } catch (ReflectiveOperationException exception) {
            available = false;
            SaferCitizensMod.LOGGER.warn("Failed to query MineColonies colony boundaries. Disabling colony-wide spawn checks until restart.", exception);
            return false;
        }
    }

    private static void initialize() {
        if (initialized) {
            return;
        }

        synchronized (MineColoniesCompat.class) {
            if (initialized) {
                return;
            }

            initialized = true;

            try {
                Class<?> managerClass = Class.forName("com.minecolonies.api.colony.IColonyManager");
                getManagerMethod = managerClass.getMethod("getInstance");
                getClosestColonyMethod = Arrays.stream(managerClass.getMethods())
                    .filter(method -> method.getName().equals("getClosestColony"))
                    .filter(method -> method.getParameterCount() == 2)
                    .filter(method -> method.getParameterTypes()[1] == BlockPos.class)
                    .findFirst()
                    .orElseThrow();

                Class<?> colonyClass = Class.forName("com.minecolonies.api.colony.IColony");
                isCoordInColonyMethod = Arrays.stream(colonyClass.getMethods())
                    .filter(method -> method.getName().equals("isCoordInColony"))
                    .filter(method -> method.getParameterCount() == 2)
                    .filter(method -> method.getParameterTypes()[1] == BlockPos.class)
                    .findFirst()
                    .orElseThrow();

                available = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                available = false;
                SaferCitizensMod.LOGGER.warn("MineColonies API methods were not resolved. Colony-wide spawn checks are disabled.", exception);
            }
        }
    }
}