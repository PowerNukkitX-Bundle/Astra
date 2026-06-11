package org.powernukkitx.anticheat.module;

import com.google.common.reflect.ClassPath;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
@RequiredArgsConstructor
public class ModuleRegistry {

    private final AntiCheatPlugin plugin;
    private final Map<Class<? extends Module>, Module> registry = new Object2ObjectOpenHashMap<>();
    private final Map<ModuleType, Set<Module>> modulesByType = new Object2ObjectOpenHashMap<>();

    public void init() {
        try {
            final ClassPath classPath = ClassPath.from(this.getClass().getClassLoader());
            final Set<ClassPath.ClassInfo> classInfos = classPath.getTopLevelClassesRecursive(
                "org.powernukkitx.anticheat.module"
            );
            for (final ClassPath.ClassInfo classInfo : classInfos) {
                final Class<?> clazz = classInfo.load();
                if (!Module.class.isAssignableFrom(clazz) ||
                    Modifier.isAbstract(clazz.getModifiers()) ||
                    Modifier.isInterface(clazz.getModifiers())) {
                    continue;
                }
                final Module module = (Module) clazz.getConstructor(AntiCheatPlugin.class)
                    .newInstance(this.plugin);
                this.registry.put((Class<? extends Module>) clazz, module);
                this.modulesByType.computeIfAbsent(module.getType(),
                    moduleType -> new ObjectOpenHashSet<>()).add(module);
                this.plugin.getServer().getPluginManager().registerEvents(module, this.plugin);
            }
        } catch (IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public Module getModule(Class<? extends Module> clazz) {
        return this.registry.get(clazz);
    }

    public Set<Module> getModules(ModuleType type) {
        return this.modulesByType.get(type);
    }
}