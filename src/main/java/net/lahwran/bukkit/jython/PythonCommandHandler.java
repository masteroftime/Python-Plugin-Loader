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

    private final PyObject func;
    private final String name;
    private int argcount = -1;

    /**
     * @param func function to handle
     * @param name name of command to use when registering
     */
    public PythonCommandHandler(PyObject func, String name) {
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

    private boolean call(int argcount, CommandSender sender, Command command, String label, String[] args) {
        PyObject[] handlerargs;
        if (argcount == 4) {
            handlerargs = new PyObject[] { Py.java2py(sender), Py.java2py(command), Py.java2py(label), Py.java2py(args)};
        } else if (argcount == 3) {
            handlerargs = new PyObject[] { Py.java2py(sender), Py.java2py(label), Py.java2py(args)};
        } else if (argcount == 2) {
            handlerargs = new PyObject[] { Py.java2py(sender), Py.java2py(args)};
        } else
            throw new IllegalArgumentException("this can't happen unless you stick your fingers in my code, which obviously you did, so howabout you undo it?");
        return func.__call__(handlerargs).__nonzero__();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean result;
        if (argcount == -1) {
            try {
                result = call(4, sender, command, label, args);
                argcount = 4;
            } catch (PyException e) {
              //this could goof up someone ... they'll probably yell at us and eventually read this code ... fuck them
                if (e.type == Py.TypeError && e.value.toString().endsWith("takes exactly 3 arguments (4 given)")) {
                    result = call(3, sender, command, label, args);
                    argcount = 3;
                } else if (e.type == Py.TypeError && e.value.toString().endsWith("takes exactly 2 arguments (4 given)")) {
                    result = call(2, sender, command, label, args);
                    argcount = 2;
                } else {
                    throw e;
                }
            }
        } else {
            result = call(argcount, sender, command, label, args);
        }
        return result;
    }

}
