package com.master.bukkit.python;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lahwran.bukkit.jython.PythonPluginLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Java plugin to initialize python plugin loader and provide it with a little moral boost.
 * @author masteroftime
 * @author lahwran
 *
 */
public class PythonLoader extends JavaPlugin {

    public void onDisable() {}
    public void onEnable() {}

    /**
     * Initialize and load up the plugin loader.
     */
    @Override
    public void onLoad() {
        //System.out.println("PythonLoader: initializing");
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
            //System.out.println("PythonLoader: loading into bukkit");
            pm.registerInterface(PythonPluginLoader.class);
            //pm.loadPlugins(this.getFile().getParentFile()); //TODO Check weather this reloads java plugins which were already laoded

            for (File file : this.getFile().getParentFile().listFiles()) {
                for (Pattern filter : PythonPluginLoader.fileFilters) {
                    Matcher match = filter.matcher(file.getName());
                    if (match.find()) {
                        try {
                            pm.loadPlugin(file);
                        } catch (InvalidPluginException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InvalidDescriptionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (UnknownDependencyException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
