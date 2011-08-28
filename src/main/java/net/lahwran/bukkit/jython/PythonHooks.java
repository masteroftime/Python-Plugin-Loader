/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.util.ArrayList;

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

    PythonHooks(PluginDescriptionFile plugindesc) {
        
    }

    void doRegistrations(PythonPlugin plugin) {
        frozen = true;
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
        checkFrozen();
        PythonCommandHandler handler = new PythonCommandHandler(func, name);
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


    public PyObject command(final String name) {
        return new PyObject() {
            public PyObject __call__(PyObject func) {
                registerCommand((PyFunction) func, name);
                return func;
            }
        };
    }
}
