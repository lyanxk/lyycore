package org.lyy.lyycore.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import org.lyy.lyycore.LyyCore;

/** Optional Iris public API access; no dependency on its rendering internals. */
final class ShaderPackCompat {
    private record Access(MethodHandle active, MethodHandle shadow) { }
    private static Access access = findAccess();

    private ShaderPackCompat() { }

    private static Access findAccess() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            var lookup = MethodHandles.publicLookup();
            return new Access(lookup.unreflect(api.getMethod("isShaderPackInUse")).bindTo(instance),
                    lookup.unreflect(api.getMethod("isRenderingShadowPass")).bindTo(instance));
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException | LinkageError error) {
            LyyCore.LOGGER.warn("Cannot initialize Iris rendering compatibility", error);
            return null;
        }
    }

    static boolean active() { return access != null && query(access.active); }
    static boolean shadowPass() { return access != null && query(access.shadow); }

    private static boolean query(MethodHandle method) {
        try {
            return (boolean) method.invokeExact();
        } catch (Throwable error) {
            access = null;
            LyyCore.LOGGER.warn("Disabling Iris rendering compatibility after an API error", error);
            return false;
        }
    }
}
