/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.UnknownSoftDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author lahwran
 *
 */
public class PythonPluginLoader implements PluginLoader {

    private Server server = null;
    public static final Pattern[] fileFilters = new Pattern[] {
            Pattern.compile("\\.py.dir$"),
            Pattern.compile("\\.py.zip$"),
            Pattern.compile("\\.pypl$"),
        };

    private HashSet<String> loadedplugins = new HashSet<String>();

    public PythonPluginLoader(Server server) {
        this.server = server;
    }

    @Override
    public EventExecutor createExecutor(Type type, Listener listener) {
        return new EventExecutor() {
            public void execute(Listener listener, Event event) {
                ((PythonListener) listener).onEvent(event);
            }
        };
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (!(plugin instanceof PythonPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (plugin.isEnabled()) {
            PythonPlugin pyPlugin = (PythonPlugin) plugin;
            //ClassLoader cloader = jPlugin.getClassLoader();

            try {
                pyPlugin.setEnabled(false);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?): " + ex.getMessage(), ex);
            }

            server.getPluginManager().callEvent(new PluginDisableEvent(plugin));

            //loaders.remove(jPlugin.getDescription().getName());

            /*if (cloader instanceof PluginClassLoader) {
                PluginClassLoader loader = (PluginClassLoader) cloader;
                Set<String> names = loader.getClasses();

                for (String name : names) {
                    classes.remove(name);
                }
            }*/
        }
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        if (!(plugin instanceof PythonPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (!plugin.isEnabled()) {
            PythonPlugin pyPlugin = (PythonPlugin) plugin;

            String pluginName = pyPlugin.getDescription().getName();

            //if (!loaders.containsKey(pluginName)) {
            //    loaders.put(pluginName, (PluginClassLoader) jPlugin.getClassLoader());
            //}

            try {
                pyPlugin.setEnabled(true);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?): " + ex.getMessage(), ex);
            }

            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException,
            UnknownDependencyException {
        return loadPlugin(file, false);
    }

    @Override
    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies)
            throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        PythonPlugin result = null;
        PluginDescriptionFile description = null;

        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
        }

        boolean isdir = file.isDirectory();

        try {
            InputStream stream = null;
            ZipFile zip = null;
            if (isdir) {
                File pluginyml = new File(file, "plugin.yml");

                if (!pluginyml.exists())
                    throw new InvalidPluginException(new FileNotFoundException("Dir does not contain plugin.yml"));

                stream = new FileInputStream(pluginyml);
            } else {
                zip = new ZipFile(file);
                ZipEntry entry = zip.getEntry("plugin.yml");
    
                if (entry == null) {
                    throw new InvalidPluginException(new FileNotFoundException("Zip does not contain plugin.yml"));
                }

                stream = zip.getInputStream(entry);
            }

            description = new PluginDescriptionFile(stream);

            stream.close();
            if (zip != null)
                zip.close();
        } catch (IOException ex) {
            throw new InvalidPluginException(ex);
        } catch (YAMLException ex) {
            throw new InvalidPluginException(ex);
        }

        File dataFolder = new File(file.getParentFile(), description.getName());

        if (dataFolder.getAbsolutePath().equals(file.getAbsolutePath())) {
            throw new InvalidPluginException(new Exception(String.format(
                    "Projected datafolder: '%s' for %s is the same file as the plugin itself (%s)",
                    dataFolder,
                    description.getName(),
                    file
                )));
        }

        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidPluginException(new Exception(String.format(
                "Projected datafolder: '%s' for %s (%s) exists and is not a directory",
                dataFolder,
                description.getName(),
                file
            )));
        }

//        ArrayList<String> depend;
//
//        try {
//            depend = (ArrayList) description.getDepend();
//            if (depend == null) {
//                depend = new ArrayList<String>();
//            }
//        } catch (ClassCastException ex) {
//            throw new InvalidPluginException(ex);
//        }
//
//        for (String pluginName : depend) {
//            if (loaders == null) {
//                throw new UnknownDependencyException(pluginName);
//            }
//            PluginClassLoader current = loaders.get(pluginName);
//
//            if (current == null) {
//                throw new UnknownDependencyException(pluginName);
//            }
//        }
//
//        if (!ignoreSoftDependencies) {
//            ArrayList<String> softDepend;
//
//            try {
//                softDepend = (ArrayList) description.getSoftDepend();
//                if (softDepend == null) {
//                    softDepend = new ArrayList<String>();
//                }
//            } catch (ClassCastException ex) {
//                throw new InvalidPluginException(ex);
//            }
//
//            for (String pluginName : softDepend) {
//                if (loaders == null) {
//                    throw new UnknownSoftDependencyException(pluginName);
//                }
//                PluginClassLoader current = loaders.get(pluginName);
//
//                if (current == null) {
//                    throw new UnknownSoftDependencyException(pluginName);
//                }
//            }
//        }

        PyString filepath = new PyString(file.getAbsolutePath());
        PyList pythonpath = Py.getSystemState().path;
        if (pythonpath.__contains__(filepath)) {
            throw new InvalidPluginException(new Exception("path "+filepath+" already on pythonpath!"));
        }
        pythonpath.append(filepath);

        try {
            result = new PythonPlugin();
            
            PythonInterpreter interp = new PythonInterpreter();
            File mainfile = new File(file, description.getMain());
            interp.set("pyplugin", result);
            FileInputStream instream = new FileInputStream(mainfile);
            interp.execfile(instream);
            
            result.initialize(this, server, description, dataFolder, file);
        } catch (Throwable t) {
            if (pythonpath.__contains__(filepath)) {
                pythonpath.remove(filepath);
            }
            throw new InvalidPluginException(t);
        }

        if (!loadedplugins.contains(description.getName()))
            loadedplugins.add(description.getName());
        return (Plugin) result;
    }

}
