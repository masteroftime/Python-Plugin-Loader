/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Type;
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
     * plugin.pydir
     * plugin.pyzip
     * plugin.pyp
     * plugin.pypl
     * plugin.pyplug
     * plugin.pyplugin
     * plugin.py.dir
     * plugin.py.zip
     * plugin.py.p
     * plugin.py.pl
     * plugin.py.plug
     * plugin.py.plugin
     * </pre>
     */
    public static final Pattern[] fileFilters = new Pattern[] {
            Pattern.compile("^(.*)\\.py\\.?(dir|zip|p|pl|plug|plugin)$"),
        };

    private HashSet<String> loadedplugins = new HashSet<String>();

    /**
     * @param server server to initialize with
     */
    public PythonPluginLoader(Server server) {
        this.server = server;
    }

    public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException,
            UnknownDependencyException {
        return loadPlugin(file, false);
    }

    @SuppressWarnings("unchecked")
    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies)
            throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        PythonPlugin result = null;
        PluginDescriptionFile description = null;

        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist",
                    file.getPath())));
        }

        boolean hasyml = true;
        try {
            InputStream stream = null;
            ZipFile zip = null;
            if (file.isDirectory()) { //this code is nearly duplicated below :(
                File pluginyml = new File(file, "plugin.yml");

                if (!pluginyml.exists())
                    hasyml = false;
                else
                    stream = new FileInputStream(pluginyml);
            } else {
                zip = new ZipFile(file);
                ZipEntry entry = zip.getEntry("plugin.yml");

                if (entry == null)
                    hasyml = false;
                else
                    stream = zip.getInputStream(entry);
            }
            if (hasyml) {
                description = new PluginDescriptionFile(stream);

            } else {
                Matcher matcher = fileFilters[0].matcher(file.getName());
                if (!matcher.matches())
                    //throw new BukkitScrewedUpException("This would only occur if bukkit called the loader on a plugin which does not match the loader's regex.");
                    throw new InvalidPluginException(new Exception("This shouldn't be happening; go tell whoever altered the plugin loading api in bukkit that they're whores."));
                description = new PluginDescriptionFile(matcher.group(1), "dev", "plugin.py");
            }
            if (stream != null)
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

        if (!ignoreSoftDependencies) {
            ArrayList<String> softDepend;

            try {
                softDepend = (ArrayList<String>) description.getSoftDepend();
                if (softDepend == null) {
                    softDepend = new ArrayList<String>();
                }
            } catch (ClassCastException ex) {
                throw new InvalidPluginException(ex);
            }

            for (String pluginName : softDepend) {
                if (!isPluginLoaded(pluginName)) {
                    throw new UnknownSoftDependencyException(pluginName);
                }
            }
        }

        PyString filepath = new PyString(file.getAbsolutePath());
        PyList pythonpath = Py.getSystemState().path;
        if (pythonpath.__contains__(filepath)) {
            throw new InvalidPluginException(new Exception("path " + filepath
                    + " already on pythonpath!")); //can't imagine how this would happen, but better safe than sorry
        }
        pythonpath.append(filepath);

        try {
            PythonHooks hook = new PythonHooks(description);

            PythonInterpreter interp = new PythonInterpreter();
            
            InputStream instream = null;
            ZipFile zip = null;
            String mainfile = description.getMain();
            if (file.isDirectory()) {
                File contained = new File(file, mainfile);

                if (!contained.exists()) {
                    mainfile = "plugin.py";
                    contained = new File(file, mainfile);
                }
                if (!contained.exists()) {
                    mainfile = "main.py";
                    contained = new File(file, mainfile);
                }

                if (!contained.exists())
                    throw new InvalidPluginException(new FileNotFoundException("Dir does not contain "+mainfile));

                instream = new FileInputStream(contained);
            } else {
                zip = new ZipFile(file);
                ZipEntry entry = zip.getEntry(mainfile);

                if (entry == null) {
                    mainfile = "plugin.py";
                    entry = zip.getEntry(mainfile);
                }
                if (entry == null) {
                    mainfile = "main.py";
                    entry = zip.getEntry(mainfile);
                }

                if (entry == null) {
                    throw new InvalidPluginException(new FileNotFoundException("Zip does not contain "+mainfile));
                }

                instream = zip.getInputStream(entry);
            }
            
            interp.set("hook", hook);
            interp.set("info", description);
            interp.exec("import net.lahwran.bukkit.jython.PythonPlugin as PythonPlugin");
            interp.execfile(instream);

            instream.close();
            if (zip != null)
                zip.close();

            try {
                if (!hasyml) {
                    Object name = interp.get("__plugin_name__");
                    Object version = interp.get("__plugin_version__");
                    Object website = interp.get("__plugin_website__");
                    Object main = interp.get("__plugin_mainclass__");
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
            result.hooks = hook;
            result.interp = interp;
            result.initialize(this, server, description, dataFolder, file);
            interp.set("pyplugin", result);
        } catch (Throwable t) {
            if (pythonpath.__contains__(filepath)) {
                pythonpath.remove(filepath);
            }
            throw new InvalidPluginException(t);
        }

        if (!loadedplugins.contains(description.getName()))
            loadedplugins.add(description.getName());
        return result;
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

    public EventExecutor createExecutor(Type type, Listener listener) {
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
    }

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

            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

}
