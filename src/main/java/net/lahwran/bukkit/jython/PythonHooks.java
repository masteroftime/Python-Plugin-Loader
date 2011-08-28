/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.python.core.*;

/**
 * @author lahwran
 *
 */
public class PythonHooks {

    private boolean frozen = false;
    PyFunction onEnable;
    PyFunction onDisable;
    ArrayList<PythonEventHandler> eventhandlers = new ArrayList<PythonEventHandler>();
    ArrayList<PythonCommandHandler> commandhandlers = new ArrayList<PythonCommandHandler>();
    PluginDescriptionFile plugindesc;

    PythonHooks(PluginDescriptionFile plugindesc) {
        this.plugindesc = plugindesc;
    }

    private void addCommandInfo(String name, String usage, String desc, List aliases) {
        Object object = plugindesc.getCommands();
        if (object == null)
            throw new PyException(new PyString("error"), new PyString("Plugin commands list does not exist"));

        Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) object;

        if (map.containsKey(name)) {
            if (desc != null || usage != null || aliases != null)
                throw new PyException(new PyString("error"), new PyString("Plugin already has a command called '"+name+"'"));
            else
                return;
        }

        Map<String, Object> commandmap = new HashMap<String, Object>();
        if (desc != null)
            commandmap.put("description", desc);
        if (usage != null)
            commandmap.put("usage", usage);
        if (aliases != null)
            commandmap.put("aliases", aliases);
        map.put(name, commandmap);
    }

    void doRegistrations(PythonPlugin plugin) {
        frozen = true;
        registerListenerEvents(plugin);
        registerHandlerCommands(plugin);
    }
    private void registerListenerEvents(PythonPlugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        for (int i=0; i<eventhandlers.size(); i++) {
            eventhandlers.get(i).register(pm, plugin);
        }
    }

    private void registerHandlerCommands(PythonPlugin plugin) {
        for (int i=0; i<commandhandlers.size(); i++) {
            commandhandlers.get(i).register(plugin);
        }
    }
    
    private void checkFrozen() {
        if (frozen)
            throw new PyException(new PyString("error"), new PyString("Cannot register handlers when frozen"));
    }
    
    public void registerEvent(PyFunction handler, Event.Type type, Event.Priority priority) {
        checkFrozen();
        PythonEventHandler wrapper = new PythonEventHandler(handler, type, priority);
        eventhandlers.add(wrapper);
    }
    public void registerEvent(PyFunction handler, PyString type, PyString priority) {
        Event.Type realtype = Event.Type.valueOf(type.upper());
        Event.Priority realpriority = Event.Priority.valueOf(priority.capitalize());
        registerEvent(handler, realtype, realpriority);
    }

    public void registerCommand(PyFunction func, String name) {
        registerCommand(func, name, null, null, null);
    }

    public void registerCommand(PyFunction func) {
        registerCommand(func, null);
    }

    public void registerCommand(PyFunction func, String name, String usage, String desc, List aliases) {
        checkFrozen();
        String finalname = name;
        if (finalname == null)
            finalname = func.__name__;
        addCommandInfo(finalname, usage, desc, aliases);
        PythonCommandHandler handler = new PythonCommandHandler(func, finalname);
        commandhandlers.add(handler);
    }

    public PyFunction enable(PyFunction func) {
        onEnable = func;
        return func;
    }

    public PyFunction disable(PyFunction func) {
        onDisable = func;
        return func;
    }

    public PyObject event(final Event.Type type, final Event.Priority priority) {
        return new PyObject() {
            public PyObject __call__(PyObject func) {
                registerEvent((PyFunction)func, type, priority);
                return func;
            }
        };
    }

    public PyObject event(final PyString type, final PyString priority) {
        return new PyObject() {
            public PyObject __call__(PyObject func) {
                registerEvent((PyFunction)func, type, priority);
                return func;
            }
        };
    }

    public PyObject event(final PyString type) {
        return new PyObject() {
            public PyObject __call__(PyObject func) {
                registerEvent((PyFunction)func, type, new PyString("Normal"));
                return func;
            }
        };
    }

    /**
     * command decorator. approximately equivalent python:
     * <pre>
     * def command(arg1, desc=None, usage=None, aliases=None):
     *     if isfunc(arg1):
     *         registerFunc(arg1, arg1.func_name)
     *     else:
     *         def decorate(func):
     *             registerFunc(func, arg1 if arg1 else func.func_name, usage, desc, aliases)
     * </pre>
     * the literally equivalent python looks so similar to the actual code as to not be worth mentioning
     * @param args
     * @param keywords
     * @return
     */
    public PyObject command(PyObject args[], String keywords[]) {
        int kwdelta = args.length - keywords.length;
        if (args.length == 1 && args[0].isCallable()) {
                registerCommand((PyFunction) args[0]);
                return args[0];
        } else if (kwdelta == 1 || kwdelta == 0) {

            String desc = null;
            String usage = null;
            List aliases = null;
            for (int i = kwdelta; i < args.length; i++) {
                String keyword = keywords[i - kwdelta];
                if (keyword.equals("desc") || keyword.equals("description"))
                    desc = args[i].toString();
                else if (keyword.equals("usage"))
                    usage = args[i].toString();
                else if (keyword.equals("aliases"))
                    aliases = new PyList(args[i]);
            }
            final String name;
            if (kwdelta == 1)
                name = args[0].toString();
            else
                name = null;
            final String finaldesc = desc;
            final String finalusage = usage;
            final List finalaliases = aliases;
            return new PyObject() {
                public PyObject __call__(PyObject func) {
                    registerCommand((PyFunction) func, name, finalusage, finaldesc, finalaliases);
                    return func;
                }
            };
        } else {
            throw Py.TypeError("you gave command() bad arguments, but lahwran was tired when he wrote this, so you don't get a helpful error message. sucks to be you.");
        }
    }
}
