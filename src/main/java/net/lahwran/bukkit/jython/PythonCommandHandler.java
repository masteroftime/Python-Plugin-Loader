package net.lahwran.bukkit.jython;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.python.antlr.ast.alias;
import org.python.core.*;


/**
 * Class to wrap python functions so that they can be used to handle commands
 * @author lahwran
 */
public class PythonCommandHandler implements CommandExecutor, TabCompleter {

    private final PyObject func;
    private final String name;
    private final PyObject tabComplete;
    private int argcount = -1;
    private int tabArgcount = -1;

    /**
     * @param func function to handle
     * @param name name of command to use when registering
     */
    public PythonCommandHandler(PyObject func, String name, PyObject tabComplete) {
        this.func = func;
        this.name = name;
        this.tabComplete = tabComplete;
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

    private PyObject[] pyArgs(Object... args) {
        PyObject[] converted = new PyObject[args.length];

        for(int i = 0; i < args.length; i++) {
            converted[i] = Py.java2py(args[i]);
        }

        return converted;
    }

    private ArrayList<String> toJavaStringList(PyObject pyList) {
        if(pyList.isSequenceType()) {
            ArrayList<String> result = new ArrayList<String>();
            for(PyObject o : pyList.asIterable()) {
                result.add(o.asString());
            }
            return result;
        } else if(pyList.getType().equals(PyString.TYPE)) {
            ArrayList<String> result = new ArrayList<String>(1);
            result.add(pyList.asString());
            return result;
        }
        else if(pyList.equals(Py.None) || pyList.equals(null)) {
            return null;
        }
        else throw new IllegalArgumentException("Tab Completer function must return a list of strings a string or null");
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
                if (e.type == Py.TypeError && (e.value.toString().endsWith("takes exactly 3 arguments (4 given)") || e.value.toString().endsWith("takes exactly 4 arguments (5 given)"))) {
                    result = call(3, sender, command, label, args);
                    argcount = 3;
                } else if (e.type == Py.TypeError && (e.value.toString().endsWith("takes exactly 2 arguments (4 given)") || e.value.toString().endsWith("takes exactly 3 arguments (5 given)"))) {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (tabComplete == null) {
            return null;
        }

        PyObject result = null;
        if (tabArgcount == -1) {
            try {
                result = tabComplete.__call__(pyArgs(sender, command, label, args));
                tabArgcount = 4;
            } catch (PyException e) {
                //this could goof up someone ... they'll probably yell at us and eventually read this code ... fuck them
                if (e.type == Py.TypeError && (e.value.toString().endsWith("takes exactly 3 arguments (4 given)") || e.value.toString().endsWith("takes exactly 4 arguments (5 given)"))) {
                    result = tabComplete.__call__(pyArgs(sender, label, args));
                    tabArgcount = 3;
                } else if (e.type == Py.TypeError && (e.value.toString().endsWith("takes exactly 2 arguments (4 given)") || e.value.toString().endsWith("takes exactly 3 arguments (5 given)"))) {
                    result = tabComplete.__call__(pyArgs(sender, args));
                    tabArgcount = 2;
                } else {
                    throw e;
                }
            }
        } else {
            switch (tabArgcount) {
            case 2: result = tabComplete.__call__(pyArgs(sender, args)); break;
            case 3: result = tabComplete.__call__(pyArgs(sender,  label, args)); break;
            case 4: result = tabComplete.__call__(pyArgs(sender, command, label, args)); break;
            default: throw new IllegalArgumentException("Python Tab Completer needs an invalid number of arguments. This case should be impossible.");
            }
        }

        return toJavaStringList(result);
    }

}
