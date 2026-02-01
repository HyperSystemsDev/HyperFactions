package com.hyperfactions.integration;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Permission provider for VaultUnlocked (Vault2 API).
 * Uses reflection to integrate with VaultUnlocked's PermissionUnlocked and ChatUnlocked interfaces.
 *
 * VaultUnlocked uses TriState for permission results:
 * - TRUE: Permission is explicitly granted
 * - FALSE: Permission is explicitly denied
 * - UNDEFINED: Permission is not set (fall through to next provider)
 */
public class VaultUnlockedProvider implements PermissionProvider {

    private boolean available = false;

    // Reflection references
    private Object permissionService = null;
    private Object chatService = null;
    private Method hasPermissionMethod = null;
    private Method getPrefixMethod = null;
    private Method getSuffixMethod = null;
    private Method getPrimaryGroupMethod = null;

    // TriState enum values
    private Object triStateTrue = null;
    private Object triStateFalse = null;
    private Object triStateUndefined = null;

    // Context and Subject helpers
    private Method createSubjectMethod = null;
    private Object globalContext = null;

    @Override
    @NotNull
    public String getName() {
        return "VaultUnlocked";
    }

    /**
     * Initializes the VaultUnlocked provider.
     * Attempts to load VaultUnlocked classes via reflection.
     */
    public void init() {
        try {
            // Load TriState enum first
            Class<?> triStateClass = Class.forName("net.milkbowl.vault2.helper.TriState");
            triStateTrue = Enum.valueOf((Class<Enum>) triStateClass, "TRUE");
            triStateFalse = Enum.valueOf((Class<Enum>) triStateClass, "FALSE");
            triStateUndefined = Enum.valueOf((Class<Enum>) triStateClass, "UNDEFINED");

            // Load Subject class
            Class<?> subjectClass = Class.forName("net.milkbowl.vault2.helper.Subject");
            // Subject.player(UUID, String) static method
            createSubjectMethod = subjectClass.getMethod("player", UUID.class, String.class);

            // Load Context class and get global context
            Class<?> contextClass = Class.forName("net.milkbowl.vault2.helper.Context");
            try {
                Method globalMethod = contextClass.getMethod("global");
                globalContext = globalMethod.invoke(null);
            } catch (NoSuchMethodException e) {
                // Try alternative: Context.GLOBAL static field
                try {
                    globalContext = contextClass.getField("GLOBAL").get(null);
                } catch (NoSuchFieldException e2) {
                    // Create empty context
                    globalContext = contextClass.getConstructor().newInstance();
                }
            }

            // Try to get services from Hytale's service registry or a static accessor
            // VaultUnlocked typically registers as a service
            Class<?> vaultUnlockedClass = Class.forName("net.milkbowl.vault2.VaultUnlocked");

            // Try static accessor methods
            try {
                Method permissionMethod = vaultUnlockedClass.getMethod("permission");
                permissionService = permissionMethod.invoke(null);
            } catch (NoSuchMethodException e) {
                // Try alternative accessor
                try {
                    Method getPermissionMethod = vaultUnlockedClass.getMethod("getPermission");
                    permissionService = getPermissionMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    Logger.debug("[VaultUnlockedProvider] No permission accessor found");
                }
            }

            // Try to get chat service
            try {
                Method chatMethod = vaultUnlockedClass.getMethod("chat");
                chatService = chatMethod.invoke(null);
            } catch (NoSuchMethodException e) {
                try {
                    Method getChatMethod = vaultUnlockedClass.getMethod("getChat");
                    chatService = getChatMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    Logger.debug("[VaultUnlockedProvider] No chat accessor found");
                }
            }

            if (permissionService == null) {
                available = false;
                Logger.debug("[VaultUnlockedProvider] Permission service not available");
                return;
            }

            // Get permission check method: has(Context, Subject, String) -> TriState
            Class<?> permissionClass = permissionService.getClass();
            hasPermissionMethod = findMethod(permissionClass, "has", contextClass, subjectClass, String.class);

            if (hasPermissionMethod == null) {
                // Try alternative method name
                hasPermissionMethod = findMethod(permissionClass, "hasPermission", contextClass, subjectClass, String.class);
            }

            if (hasPermissionMethod == null) {
                available = false;
                Logger.debug("[VaultUnlockedProvider] No permission check method found");
                return;
            }

            // Get chat methods if chat service available
            if (chatService != null) {
                Class<?> chatClass = chatService.getClass();
                getPrefixMethod = findMethod(chatClass, "getPrefix", contextClass, subjectClass);
                getSuffixMethod = findMethod(chatClass, "getSuffix", contextClass, subjectClass);
            }

            // Get primary group method
            getPrimaryGroupMethod = findMethod(permissionClass, "getPrimaryGroup", contextClass, subjectClass);

            available = true;
            Logger.info("[PermissionManager] VaultUnlocked provider initialized");

        } catch (ClassNotFoundException e) {
            available = false;
            Logger.debug("[VaultUnlockedProvider] VaultUnlocked not found");
        } catch (Exception e) {
            available = false;
            Logger.debug("[VaultUnlockedProvider] Failed to initialize: %s", e.getMessage());
        }
    }

    /**
     * Finds a method by name and parameter types, checking interfaces too.
     */
    private Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try interfaces
            for (Class<?> iface : clazz.getInterfaces()) {
                try {
                    return iface.getMethod(name, paramTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            // Try superclass
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return findMethod(superclass, name, paramTypes);
            }
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    @NotNull
    public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        if (!available || permissionService == null || hasPermissionMethod == null) {
            return Optional.empty();
        }

        try {
            // Create subject for player
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);

            // Call has(Context, Subject, String) -> TriState
            Object result = hasPermissionMethod.invoke(permissionService, globalContext, subject, permission);

            if (result == null) {
                return Optional.empty();
            }

            // Check TriState value
            if (result.equals(triStateTrue)) {
                return Optional.of(true);
            } else if (result.equals(triStateFalse)) {
                return Optional.of(false);
            } else if (result.equals(triStateUndefined)) {
                // UNDEFINED means fall through to next provider
                return Optional.empty();
            }

            // Unknown result, treat as undefined
            return Optional.empty();

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception checking permission: %s", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Nullable
    public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
        if (!available || chatService == null || getPrefixMethod == null) {
            return null;
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            Object context = worldName != null ? createWorldContext(worldName) : globalContext;

            Object result = getPrefixMethod.invoke(chatService, context, subject);

            // Result may be Optional<String>
            if (result instanceof Optional<?> opt) {
                return opt.map(Object::toString).orElse(null);
            }
            return result != null ? result.toString() : null;

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception getting prefix: %s", e.getMessage());
            return null;
        }
    }

    @Override
    @Nullable
    public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
        if (!available || chatService == null || getSuffixMethod == null) {
            return null;
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            Object context = worldName != null ? createWorldContext(worldName) : globalContext;

            Object result = getSuffixMethod.invoke(chatService, context, subject);

            // Result may be Optional<String>
            if (result instanceof Optional<?> opt) {
                return opt.map(Object::toString).orElse(null);
            }
            return result != null ? result.toString() : null;

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception getting suffix: %s", e.getMessage());
            return null;
        }
    }

    @Override
    @NotNull
    public String getPrimaryGroup(@NotNull UUID playerUuid) {
        if (!available || permissionService == null || getPrimaryGroupMethod == null) {
            return "default";
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            Object result = getPrimaryGroupMethod.invoke(permissionService, globalContext, subject);

            // Result may be Optional<String>
            if (result instanceof Optional<?> opt) {
                return opt.map(Object::toString).orElse("default");
            }
            return result != null ? result.toString() : "default";

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception getting primary group: %s", e.getMessage());
            return "default";
        }
    }

    /**
     * Creates a world-specific context.
     */
    private Object createWorldContext(String worldName) {
        try {
            Class<?> contextClass = Class.forName("net.milkbowl.vault2.helper.Context");
            // Try Context.of(String world) or similar
            try {
                Method ofMethod = contextClass.getMethod("of", String.class);
                return ofMethod.invoke(null, worldName);
            } catch (NoSuchMethodException e) {
                // Try constructor
                try {
                    return contextClass.getConstructor(String.class).newInstance(worldName);
                } catch (NoSuchMethodException e2) {
                    // Fall back to global
                    return globalContext;
                }
            }
        } catch (Exception e) {
            return globalContext;
        }
    }
}
