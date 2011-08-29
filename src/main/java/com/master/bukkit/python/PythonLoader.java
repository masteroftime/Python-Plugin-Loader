package com.master.bukkit.python;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.lahwran.bukkit.jython.PythonPluginLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

/**
 * Java plugin to initialize python plugin loader and provide it with a little moral boost.
 * @author masteroftime
 * @author lahwran
 *
 */
public class PythonLoader extends JavaPlugin {

    /**
     * Initialize and load up the plugin loader.
     */
    public PythonLoader() {
        System.out.println("PythonLoader: initializing");
        // This must occur as early as possible, and only once.
        PluginManager pm = Bukkit.getServer().getPluginManager();
        boolean needsload = true;

        String errorstr = "cannot ensure that the python loader class is not loaded twice!";
        Map<Pattern, PluginLoader> fileAssociations = ReflectionHelper.getFileAssociations(pm, errorstr);

        if (fileAssociations != null) {
            PluginLoader loader = fileAssociations.get(PythonPluginLoader.fileFilters[0]);
            if (loader != null) // already loaded
                needsload = false;
        }

        if (needsload) {
            System.out.println("PythonLoader: loading into bukkit");
            pm.registerInterface(PythonPluginLoader.class);
        }
    }

    public void onDisable() {}
    public void onEnable() {}

}
