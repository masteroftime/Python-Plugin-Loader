/**
 *
 */
package net.lahwran.bukkit.jython;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

import com.master.bukkit.python.ReflectionHelper;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

/**
 * Python decorators and handler registration
 * @author lahwran
 */
public class PythonHooks {

    /**
     * Once set, no more hooks can be added
     */
    private boolean frozen = false;

    /**
     * Function to call when plugin is being enabled
     */
    PyFunction onEnable;

    /**
     * Function to call when plugin is being disabled
     */
    PyFunction onDisable;

    /**
     * List of handlers to register
     */
    ArrayList<PythonEventHandler> eventhandlers = new ArrayList<PythonEventHandler>();

    /**
     * List of handlers to register
     */
    ArrayList<PythonCommandHandler> commandhandlers = new ArrayList<PythonCommandHandler>();

    /**
     * Plugin description to modify when registering commands
     */
    PluginDescriptionFile plugindesc;

    /**
     * @param plugindesc Plugin description to modify when registering commands
     */
    PythonHooks(PluginDescriptionFile plugindesc) {
        this.plugindesc = plugindesc;
    }

    @SuppressWarnings("unchecked")
    private void addCommandInfo(String name, String usage, String desc, List aliases) {
        Object object = plugindesc.getCommands();
        if (object == null) {
            object = new HashMap<String, HashMap<String, Object>>();
            try {
                ReflectionHelper.setPrivateValue(plugindesc, "commands", object);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new PyException(new PyString("error"), new PyString("Plugin commands list does not exist"));
            }
        }

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

    /**
     * Register everything with bukkit
     * @param plugin plugin to do registration as
     */
    void doRegistrations(PythonPlugin plugin) {
        frozen = true;
        PluginManager pm = plugin.getServer().getPluginManager();
        for (int i=0; i<eventhandlers.size(); i++) {
            eventhandlers.get(i).register(pm, plugin);
        }
        for (int i=0; i<commandhandlers.size(); i++) {
            commandhandlers.get(i).register(plugin);
        }
    }

    /**
     * Check if we're allowed to register stuff, and if not, throw an exception
     */
    private void checkFrozen() {
        if (frozen)
            throw new PyException(new PyString("error"), new PyString("Cannot register handlers when frozen"));
    }

    /**
     * Register an event. python-facing.
     * @param handler Function handler
     * @param type event type
     * @param priority event priority
     * @deprecated Bukkit 1.1 introduced a new EventHandling API
     */
    @Deprecated
    public void registerEvent(PyFunction handler, Event.Type type, Event.Priority priority) {
        checkFrozen();
        PythonEventHandler wrapper = new PythonEventHandler(handler, type, priority);
        eventhandlers.add(wrapper);
    }

    public void registerEvent(PyFunction handler, Class<? extends Event> type, EventPriority priority) {
        checkFrozen();
        PythonEventHandler wrapper = new PythonEventHandler(handler, type, priority);
        eventhandlers.add(wrapper);
    }

    /**
     * Register an event. python-facing. this version converts from string info.
     * @param handler function handler
     * @param type Event type string
     * @param priority Event priority string
     */
    @SuppressWarnings("deprecation")
    public void registerEvent(PyFunction handler, PyString type, PyString priority) {
        try {
            String clazz = type.asString();
            Class<?> event = null;

            if(clazz.startsWith("org.bukkit.event")) {
                //the whole name was specified, just use it
                event = Class.forName(clazz);
            }
            else if(clazz.contains(".")) {
                //assume the subpackage and class name was given and append org.bukkit.event
                event = Class.forName("org.bukkit.event." + clazz);
            }
            else {
                //assume this is an old name
                Event.Type realtype = Event.Type.valueOf(type.upper());
                Event.Priority realpriority = Event.Priority.valueOf(priority.capitalize());
                registerEvent(handler, realtype, realpriority);
                Bukkit.getLogger().log(Level.WARNING, "Registered deprecated event " + realtype + "! Since Bukkit 1.1 the event type should be specified in a new format");
                return;
            }

            if(!event.getClass().isInstance(event)) {
                throw new IllegalArgumentException(type.asString().toUpperCase() + " is not of type Event");
            }
            Class<? extends Event> realtype = (Class<? extends Event>)event;
            EventPriority realpriority = EventPriority.valueOf(priority.upper());
            registerEvent(handler, realtype, realpriority);

        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not register event " + type +" because the event could not be found", e);
        }
    }

    /**
     * Register a command with no extra metadata
     * @param func function to set as handler
     * @param name name to register
     */
    public void registerCommand(PyFunction func, String name) {
        registerCommand(func, name, null, null, null);
    }

    /**
     * Register a command with no extra metadata
     * @param func function to set as handler; function's name is used as command name
     */
    public void registerCommand(PyFunction func) {
        registerCommand(func, null);
    }


    /**
     * Register a command with no extra metadata
     * @param func function to set as handler
     * @param name name to use
     * @param usage metadata
     * @param desc metadata
     * @param aliases metadata
     */
    public void registerCommand(PyFunction func, String name, String usage, String desc, List<?> aliases) {
        checkFrozen();
        String finalname = name;
        if (finalname == null)
            finalname = func.__name__;
        addCommandInfo(finalname, usage, desc, aliases);
        PythonCommandHandler handler = new PythonCommandHandler(func, finalname);
        commandhandlers.add(handler);
    }

    /**
     * Python decorator. functions decorated with this are called on enable
     * <pre>
     * @hook.enable
     * def enable():
     *     print "enabled!"
     * </pre>
     * @param func function to decorate
     * @return decorated function
     */
    public PyFunction enable(PyFunction func) {
        onEnable = func;
        return func;
    }

    /**
     * Python decorator. functions decorated with this are called on disable
     * <pre>
     * @hook.disable
     * def disable():
     *     print "enabled!"
     * </pre>
     * @param func function to decorate
     * @return decorated function
     */
    public PyFunction disable(PyFunction func) {
        onDisable = func;
        return func;
    }

    /**
     * Python decorator. functions decorated with this are called as event handlers
     * @param type event type
     * @param priority event priority
     * @return decorated function
     */
    public PyObject event(final Event.Type type, final Event.Priority priority) {
        return new PyObject() {
            public PyObject __call__(PyObject func) {
                registerEvent((PyFunction)func, type, priority);
                return func;
            }
        };
    }


    /**
     * Python decorator. functions decorated with this are called as event handlers
     * @param type event type
     * @param priority event priority
     * @return decorated function
     */
    public PyObject event(final PyString type, final PyString priority) {
        return new PyObject() {
            public PyObject __call__(PyObject func) {
                registerEvent((PyFunction)func, type, priority);
                return func;
            }
        };
    }


    /**
     * Python decorator. functions decorated with this are called as event handlers. uses normal priority.
     * @param type event type
     * @return decorated function
     */
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
     * @param args jython magic
     * @param keywords jython magic
     * @return jython magic
     */
    public PyObject command(PyObject args[], String keywords[]) {
        int kwdelta = args.length - keywords.length;
        if (args.length == 1 && args[0].isCallable()) {
                registerCommand((PyFunction) args[0]);
                return args[0];
        } else if (kwdelta == 1 || kwdelta == 0) {

            String desc = null;
            String usage = null;
            List<?> aliases = null;
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
            final List<?> finalaliases = aliases;
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
