package net.lahwran.bukkit.jython;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.python.core.*;


/**
 * Class to wrap python functions so that they can be used to handle commands
 * @author lahwran
 */
public class PythonCommandHandler implements CommandExecutor {

    private final PyFunction func;
    private final String name;

    /**
     * @param func function to handle
     * @param name name of command to use when registering
     */
    public PythonCommandHandler(PyFunction func, String name) {
        this.func = func;
        this.name = name;
    }

    /**
     * @param pythonPlugin plugin to register command to
     */
    void register(PythonPlugin pythonPlugin) {
        PluginCommand command = pythonPlugin.getCommand(name);
        if (command == null)
            throw new IllegalArgumentException("Command '"+name+"' not found in plugin " + pythonPlugin.getDescription().getName());
        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        func.__call__(new PyObject[] { Py.java2py(sender), Py.java2py(command), Py.java2py(label), Py.java2py(args)});
        return false;
    }

}
