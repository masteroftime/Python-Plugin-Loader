/**
 *
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.yaml.snakeyaml.error.YAMLException;

import com.master.bukkit.python.ReflectionHelper;

/**
 * A jython plugin loader. depends on JavaPluginLoader and SimplePluginManager.
 *
 * @author masteroftime
 * @author lahwran
 */
public class PythonPluginLoader implements PluginLoader {

    private final Server server;

    /**
     * Filter - matches all of the following, for the regex illiterate:
     * <pre>
     * plugin_py_dir
     * plugin.py.dir
     * plugin.py.zip
     * plugin.pyp
     * </pre>
     */
    public static final Pattern[] fileFilters = new Pattern[] {
            Pattern.compile("^(.*)\\.py\\.dir$"),
            Pattern.compile("^(.*)_py_dir$"),
            Pattern.compile("^(.*)\\.py\\.zip$"),
            Pattern.compile("^(.*)\\.pyp$"),
            Pattern.compile("^(.*)\\.py$"),
        };

    private HashSet<String> loadedplugins = new HashSet<String>();

    /**
     * @param server server to initialize with
     */
    public PythonPluginLoader(Server server) {
        this.server = server;
    }

    public Plugin loadPlugin(File file) throws InvalidPluginException/*, UnknownDependencyException*/ {
        return loadPlugin(file, false);
    }

    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies)
            throws InvalidPluginException/*, InvalidDescriptionException, UnknownDependencyException*/ {

        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist",
                    file.getPath())));
        }

        PluginDataFile data = null;

        if (file.getName().endsWith(".py")) {
            if (file.isDirectory())
                throw new InvalidPluginException(new Exception("python files cannot be directories! try .py.dir instead."));
            data = new PluginPythonFile(file);
        } else if (file.getName().endsWith("dir")) {
            if (!file.isDirectory())
                throw new InvalidPluginException(new Exception("python directories cannot be normal files! try .py or .py.zip instead."));
            data = new PluginPythonDirectory(file);
        } else if (file.getName().endsWith("zip") || file.getName().endsWith("pyp")) {
            if (file.isDirectory())
                throw new InvalidPluginException(new Exception("python zips cannot be directories! try .py.dir instead."));
            data = new PluginPythonZip(file);
        } else {
            throw new InvalidPluginException(new Exception("filename '"+file.getName()+"' does not end in py, dir, zip, or pyp! did you add a regex without altering loadPlugin()?"));
        }

        try {
            return loadPlugin(file, ignoreSoftDependencies, data);
        } finally {
            try {
                data.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Plugin loadPlugin(File file, boolean ignoreSoftDependencies, PluginDataFile data) throws InvalidPluginException/*, InvalidDescriptionException, UnknownDependencyException*/ {
        System.out.println("[PythonLoader] Loading Plugin " + file.getName());
        PythonPlugin result = null;
        PluginDescriptionFile description = null;
        boolean hasyml = true;
        boolean hassolidmeta = false; // whether we have coder-set metadata. true for have set metadata, false for inferred metadata.
        try {
            InputStream stream = data.getStream("plugin.yml");
            if (stream == null)
                hasyml = false;

            if (hasyml) {
                description = new PluginDescriptionFile(stream);
                hassolidmeta = true;
            } else {
                String stripped = stripExtension(file.getName());
                if (stripped == null)
                    //throw new BukkitScrewedUpException("This would only occur if bukkit called the loader on a plugin which does not match the loader's regex.");
                    throw new InvalidPluginException(new Exception("This shouldn't be happening; go tell whoever altered the plugin loading api in bukkit that they're whores."));
                description = new PluginDescriptionFile(stripped, "dev", "plugin.py");
            }
            if (stream != null)
                stream.close();
        } catch (IOException ex) {
            throw new InvalidPluginException(ex);
        } catch (YAMLException ex) {
            throw new InvalidPluginException(ex);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidPluginException(ex);
        }

        File dataFolder = new File(file.getParentFile(), description.getName());

        if (dataFolder.getAbsolutePath().equals(file.getAbsolutePath())) {
            throw new InvalidPluginException(new Exception(String.format("Projected datafolder: '%s' for %s is the same file as the plugin itself (%s)",
                    dataFolder,
                    description.getName(),
                    file)));
        }

        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidPluginException(new Exception(String.format("Projected datafolder: '%s' for %s (%s) exists and is not a directory",
                    dataFolder,
                    description.getName(),
                    file)));
        }

        ArrayList<String> depend;

        try {
            depend = (ArrayList<String>) description.getDepend();
            if (depend == null) {
                depend = new ArrayList<String>();
            }
        } catch (ClassCastException ex) {
            throw new InvalidPluginException(ex);
        }

        for (String pluginName : depend) {
            if (!isPluginLoaded(pluginName)) {
                throw new UnknownDependencyException(pluginName);
            }
        }

        PyList pythonpath = Py.getSystemState().path;
        PyString filepath = new PyString(file.getAbsolutePath());
        if (data.shouldAddPathEntry()) {
            if (pythonpath.__contains__(filepath)) {
                throw new InvalidPluginException(new Exception("path " + filepath
                        + " already on pythonpath!")); //can't imagine how this would happen, but better safe than sorry
            }
            pythonpath.append(filepath);
        }


        String mainfile = description.getMain();
        InputStream instream = null;
        try {
            instream = data.getStream(mainfile);

            if (instream == null) {
                mainfile = "plugin.py";
                instream = data.getStream(mainfile);
            }
            if (instream == null) {
                mainfile = "main.py";
                instream = data.getStream(mainfile);
            }
        } catch (IOException e) {
            throw new InvalidPluginException(e);
        }

        if (instream == null) {
            throw new InvalidPluginException(new FileNotFoundException("Data file does not contain "+mainfile));
        }
        try {
            PythonHooks hook = new PythonHooks(description);

            PythonInterpreter interp = new PythonInterpreter();

            interp.set("hook", hook);
            interp.set("info", description);
            
            // Decorator Enhancements
            interp.exec("import __builtin__");
            interp.exec("__builtin__.hook = hook");
            interp.exec("__builtin__.info = info");
            
            // Hardcoded for now, may be worth thinking about generalizing it as sort of "addons" for the PythonPluginLoader
            // Could be used to extend the capabilities of python plugins the same way the metaclass decorators do, without requiring any changes to the PythonPluginLoader itself
            String[] pre_plugin_scripts = {"imports.py", "meta_decorators.py"};
            String[] post_plugin_scripts = {"meta_loader.py"};
            
            // Run scripts designed to be run before plugin creation
            for (String script : pre_plugin_scripts) {
	            InputStream metastream = this.getClass().getClassLoader().getResourceAsStream("scripts/"+script);
	            interp.execfile(metastream);
	            metastream.close();
            }

            interp.execfile(instream);

            instream.close();

            try {
                if (!hasyml) {
                    Object name = interp.get("__plugin_name__");
                    Object version = interp.get("__plugin_version__");
                    Object website = interp.get("__plugin_website__");
                    Object main = interp.get("__plugin_mainclass__");
                    hassolidmeta = name != null && version != null;
                    if (name != null)
                        ReflectionHelper.setPrivateValue(description, "name", name.toString());
                    if (version != null)
                        ReflectionHelper.setPrivateValue(description, "version", version.toString());
                    if (website != null)
                        ReflectionHelper.setPrivateValue(description, "website", website.toString());
                    if (main != null)
                        ReflectionHelper.setPrivateValue(description, "main", main.toString());
                }
            } catch (Throwable t) {
                Logger.getLogger("Minecraft").log(Level.SEVERE, "Error while setting python-set description values", t);
            }

            String mainclass = description.getMain();
            PyObject pyClass = interp.get(mainclass);
            if (pyClass == null)
                pyClass = interp.get("Plugin");
            if (pyClass == null)
                result = new PythonPlugin();
            else
                result = (PythonPlugin) pyClass.__call__().__tojava__(PythonPlugin.class);
            
            interp.set("pyplugin", result);
            
            result.hooks = hook;
            result.interp = interp;
            
            // Run scripts designed to be run after plugin creation
            for (String script : post_plugin_scripts) {
	            InputStream metastream = this.getClass().getClassLoader().getResourceAsStream("scripts/"+script);
	            interp.execfile(metastream);
	            metastream.close();
            }
            
            result.initialize(this, server, description, dataFolder, file);
            result.setDataFile(data);
            
        } catch (Throwable t) {
            if (data.shouldAddPathEntry() && pythonpath.__contains__(filepath)) {
                pythonpath.remove(filepath);
            }
            throw new InvalidPluginException(t);
        }

        if (data.getNeedsSolidMeta() && !hassolidmeta) {
            throw new InvalidPluginException(new Exception("Released plugins require either a plugin.yml or both __plugin_name__ and __plugin_version__ set in the main python file!"));
        }

        if (!loadedplugins.contains(description.getName()))
            loadedplugins.add(description.getName());
        return result;
    }

    /**
     * Remove the extension for use in an automatic plugin name
     * @param toStrip filename to strip
     * @return stripped file name or null if not changed
     */
    private String stripExtension(String toStrip) {
        for (Pattern p : fileFilters) {
            Matcher m = p.matcher(toStrip);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    private boolean isPluginLoaded(String name) {
        if (loadedplugins.contains(name))
            return true;
        if (ReflectionHelper.isJavaPluginLoaded(server.getPluginManager(), name))
            return true;
        return false;
    }

    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }

    /*public EventExecutor createExecutor(Type type, Listener listener) {
        if (listener.getClass().equals(PythonListener.class)) { // 8 times faster than instanceof \o/
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PythonListener) listener).onEvent(event);
                }
            };
        } else {
            JavaPluginLoader jplugload = ReflectionHelper.getJavaPluginLoader(server.getPluginManager());
            return jplugload.createExecutor(type, listener);
        }
    }*/

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
                server.getLogger().log(Level.SEVERE,
                        "Error occurred while disabling " + plugin.getDescription().getFullName()
                                + " (Is it up to date?): " + ex.getMessage(),
                        ex);
            }

            server.getPluginManager().callEvent(new PluginDisableEvent(plugin));

            String pluginName = pyPlugin.getDescription().getName();
            if (loadedplugins.contains(pluginName))
                loadedplugins.remove(pluginName);
        }
    }

    public void enablePlugin(Plugin plugin) {
        if (!(plugin instanceof PythonPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (!plugin.isEnabled()) {
            PythonPlugin pyPlugin = (PythonPlugin) plugin;

            String pluginName = pyPlugin.getDescription().getName();

            if (!loadedplugins.contains(pluginName))
                loadedplugins.add(pluginName);

            try {
                pyPlugin.setEnabled(true);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE,
                        "Error occurred while enabling " + plugin.getDescription().getFullName()
                                + " (Is it up to date?): " + ex.getMessage(),
                        ex);
            }

            //finally register the listener for the hook events
            server.getPluginManager().registerEvents(pyPlugin.listener, pyPlugin);

            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(
            Listener listener, Plugin plugin) {
        boolean useTimings = server.getPluginManager().useTimings();
        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();

        if(!listener.getClass().equals(PythonListener.class)) {
            throw new IllegalArgumentException("Listener to register is not a PythonListener");
        }

        PythonListener pyListener = (PythonListener)listener;

        for(Map.Entry<Class<? extends Event>, Set<PythonEventHandler>> entry : pyListener.handlers.entrySet()) {
            Set<RegisteredListener> eventSet = new HashSet<RegisteredListener>();

            for(final PythonEventHandler handler : entry.getValue()) {
                EventExecutor executor = new EventExecutor() {

                    @Override
                    public void execute(Listener listener, Event event) throws EventException {
                        if(!listener.getClass().equals(PythonListener.class)) {
                            throw new IllegalArgumentException("No PythonListener passed to EventExecutor! If this happens someone really fucked up something");
                        }
                        ((PythonListener)listener).fireEvent(event, handler);
                    }
                };
                if(useTimings) {
                    eventSet.add(new TimedRegisteredListener(pyListener, executor, handler.priority, plugin, false));
                }
                else {
                    eventSet.add(new RegisteredListener(pyListener, executor, handler.priority, plugin, false));
                }
            }
            ret.put(entry.getKey(), eventSet);
        }
        return ret;
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file)
            throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        InputStream stream = null;
        PluginDataFile data = null;

        if (file.getName().endsWith(".py")) {
            if (file.isDirectory())
                //cause we can't throw InvalidPluginExceptions from here we throw an InvalidDescriptionExecption with the InvalidPlugin as cause
                throw new InvalidDescriptionException(new InvalidPluginException(new Exception("python files cannot be directories! try .py.dir instead.")));
            data = new PluginPythonFile(file);
        } else if (file.getName().endsWith("dir")) {
            if (!file.isDirectory())
                throw new InvalidDescriptionException(new InvalidPluginException(new Exception("python directories cannot be normal files! try .py or .py.zip instead.")));
            data = new PluginPythonDirectory(file);
        } else if (file.getName().endsWith("zip") || file.getName().endsWith("pyp")) {
            if (file.isDirectory())
                throw new InvalidDescriptionException(new InvalidPluginException(new Exception("python zips cannot be directories! try .py.dir instead.")));
            try {
                data = new PluginPythonZip(file);
            } catch (InvalidPluginException ex) {
                throw new InvalidDescriptionException(ex);
            }
        } else {
            throw new InvalidDescriptionException(new InvalidPluginException(new Exception("filename '"+file.getName()+"' does not end in py, dir, zip, or pyp! did you add a regex without altering loadPlugin()?")));
        }

        try {
            stream = data.getStream("plugin.yml");

            if(stream == null) {
                //TODO Does this cause serious problems with plugins which have no plugin.yml file?
                throw new InvalidDescriptionException(new InvalidPluginException(new FileNotFoundException("Jar does not contain plugin.yml")));
            }

            return new PluginDescriptionFile(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {}
            }
        }
        return null;
    }
}
