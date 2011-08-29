package org.bukkit.plugin.python;

/**
 * Placeholder for backwards compatibility with masteroftime's existing plugins.
 * @author lahwran
 *
 */
public class PythonPlugin extends net.lahwran.bukkit.jython.PythonPlugin {

    /**
     * give the user a nice warning that they are using deprecated code
     */
    @Deprecated
    public PythonPlugin() {
        System.err.println("WARNING: you are using deprecated (and unnecessary) code in your python plugin! please remove 'import org.bukkit.plugin.python.PythonPlugin'. PythonPlugin is automatically imported now.");
    }

}
