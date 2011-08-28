package com.master.bukkit.python;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

import net.lahwran.bukkit.jython.PythonPluginLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class PythonLoader extends JavaPlugin {

    public PythonLoader() {
        System.out.println("PythonLoader: initializing");
        // This must occur as early as possible, and only once.
        PluginManager pm = Bukkit.getServer().getPluginManager();
        boolean needsload = true;

        String errorstr = "cannot ensure that the python loader class is not loaded twice!";
        Class pmclass = null;
        try {
            pmclass = Class.forName("org.bukkit.plugin.SimplePluginManager");
        } catch (ClassNotFoundException e) {
            printerr("Did not find SimplePluginManager", errorstr);
        } catch (Throwable t) {
            printerr("Error while checking for SimplePluginManager", errorstr);
            t.printStackTrace();
        }

        Field fieldFileAssociations = null;
        if (pmclass != null) {
            try {
                fieldFileAssociations = pmclass.getDeclaredField("fileAssociations");
            } catch (SecurityException e) {
                printerr("SecurityException while checking for fileAssociations field in SimplePluginManager", errorstr);
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                printerr("SimplePluginManager does not have fileAssociations field", errorstr);
            } catch (Throwable t) {
                printerr("Error while checking for fileAssociations field in SimplePluginManager", errorstr);
                t.printStackTrace();
            }
        }

        Map<Pattern, PluginLoader> fileAssociations = null;
        if (fieldFileAssociations != null) {
            try {
                fieldFileAssociations.setAccessible(true);
                fileAssociations = (Map<Pattern, PluginLoader>) fieldFileAssociations.get(pm);
            } catch (ClassCastException e) {
                printerr("fileAssociations is not of type Map<Pattern, PluginLoader>", errorstr);
                
            } catch (Throwable t) {
                printerr("Error while getting fileAssociations from PluginManager", errorstr);
                t.printStackTrace();
            }
        }

        if (fileAssociations != null) {
            PluginLoader loader = fileAssociations.get(PythonPluginLoader.fileFilters[0]);
            if (loader != null) // already loaded
                needsload = false;
        }

        if (needsload) {
            System.out.println("PythonLoader: loading into bukkit");
            pm.registerInterface(PythonPluginLoader.class);
            pm.registerInterface(org.bukkit.plugin.python.PythonPluginLoader.class);
        }
    }

    private void printerr(String cause, String issue) {
        System.err.println("PythonLoader: "+cause+", "+issue);
    }
    
    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {
        /*System.out.println("Enabling Python Plugin loader.");

        //load all plugin classes that they are managed by the same plugin loader
        try {
            Class.forName("org.bukkit.plugin.python.PythonPlugin");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            int i = 1;
            while (true) {
                Class.forName("org.bukkit.plugin.python.PythonPluginLoader$" + i);
                i++;
            }
        } catch (ClassNotFoundException e) {

        }

        File pluginDir = new File("plugins");
        if (!pluginDir.exists())
            pluginDir.mkdir();

        getServer().getPluginManager().registerInterface(PythonPluginLoader.class);
        getServer().getPluginManager().loadPlugins(pluginDir);

        for (Plugin p : getServer().getPluginManager().getPlugins()) {
            if (p instanceof PythonPlugin) {
                getServer().getPluginManager().enablePlugin(p);
            }
        }*/
    }

}
