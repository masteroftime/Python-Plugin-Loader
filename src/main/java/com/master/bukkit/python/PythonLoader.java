package com.master.bukkit.python;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
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
        //check if jython.jar exists if not try to download
        if(!new File("lib/jython.jar").exists()) {
            getServer().getLogger().log(Level.SEVERE, "Could not find lib/jython.jar! I will try to automatically download it for you.");
            try {
                URL website = new URL("http://cloud.github.com/downloads/masteroftime/Python-Plugin-Loader/jython.jar");
                URLConnection connection = website.openConnection();
                connection.connect();

                //create lib folder if it doesn't exist
                File lib = new File("lib");
                if(!lib.exists()) {
                    lib.mkdir();
                }

                //delete temporary _dl file if it already exists
                File dl_file = new File("lib/jython.jar_dl");
                if(dl_file.exists()) {
                    dl_file.delete();
                }

                InputStream in = connection.getInputStream();
                FileOutputStream out = new FileOutputStream(dl_file);

                long total = connection.getContentLengthLong();
                long progress = 0;
                byte[] buffer = new byte[1024];
                int read;
                long start = System.currentTimeMillis();

                while((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    progress += read;
                    if(System.currentTimeMillis() - start > 2000) {
                        System.out.println("Downloading Jython: " + (progress*100/total) + " %");
                        start = System.currentTimeMillis();
                    }
                }

                out.close();
                in.close();

                dl_file.renameTo(new File("lib/jython.jar"));
                getServer().getLogger().log(Level.INFO, "Download successful!");
            } catch (IOException e) {
                getServer().getLogger().log(Level.SEVERE, "Error while donwloading jython.jar, loading of python plugins will fail! Please download jython from https://github.com/downloads/masteroftime/Python-Plugin-Loader/jython.jar and place it in the lib folder");
                e.printStackTrace();
            }
        }

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
