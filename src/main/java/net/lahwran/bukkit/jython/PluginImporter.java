package net.lahwran.bukkit.jython;

import java.io.File;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.modules._imp;

/**
 * Unfinished importer to separate plugins into their own namespaces, possibly also import
 * java plugins
 *
 * @author lahwran
 *
 */
public class PluginImporter extends PyObject {

    private static final String KEYWORD = "__plugins__";
    private PluginManager manager;

    PluginImporter(PluginManager manager) {
        this.manager = manager;
    }

    public PyObject __call__(PyObject args[], String keywords[]) {
        if(args[0].toString().equals(KEYWORD)){
            return this;
        }
        throw Py.ImportError("unable to handle");
    }

    /**
     * Find the module for the fully qualified name.
     *
     * @param name the fully qualified name of the module
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    public PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    private PyObject retrieveModule(String name) {
        String[] split = name.split("\\.", 2);
        String pluginname = split[0];
        String subname = split[1];

        Plugin plugin = manager.getPlugin(pluginname);
        if (plugin instanceof JavaPlugin) {
            System.err.println("importing java plugins is not yet supported.");
            return Py.None;
        } else if (plugin instanceof PythonPlugin) {
            PythonPlugin pyplugin = (PythonPlugin) plugin;
            File pluginpath = pyplugin.getFile();
            PyString pypath = new PyString(pluginpath.getAbsolutePath());
            PyObject[] elements = new PyObject[] {pypath};
            PyList paths = new PyList(elements);
            PyObject result = _imp.find_module(subname, paths);
            if (result != null) {
                PyTuple info = (PyTuple) result;

            }
        }
        return Py.None;
    }

    /**
     * Find the module for the fully qualified name.
     *
     * @param name the fully qualified name of the module
     * @param path if installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    public PyObject find_module(String name, PyObject path) {
        Py.writeDebug("import", "trying " + name
                + " in PluginManager for path " + path);

        return Py.None;
    }

    public PyObject load_module(String name) {
        return null;//PySystemState.packageManager.lookupName(name.intern());
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return this.getType().toString();
    }
}
