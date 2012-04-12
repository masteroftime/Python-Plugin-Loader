/**
 *
 */
package net.lahwran.bukkit.jython;

import java.util.logging.Level;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.PluginManager;
import org.python.core.PyFunction;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyStaticMethod;

/**
 * Class to wrap python functions so they can be used to handle events
 * @author lahwran
 *
 */
class PythonEventHandler {
    /**
     * Python function to call
     */
    final PyObject handler;

    /**
     * Event type this handler is listening for
     */
    final Class<? extends Event> type;

    /**
     * Priority to register the handler at
     */
    final EventPriority priority;

    /**
     * Whether we've registered yet
     */
    boolean currentlyRegistered = false;

    /**
     * @param handler Python function to call
     * @param type Event type this handler is listening for
     * @param priority Priority to register the handler at
     */
    PythonEventHandler(PyObject handler, Class<? extends Event> type, EventPriority priority) {
        if(handler.isCallable())
        {
            this.handler = handler;
        }
        else 
        {
            throw new IllegalArgumentException("Tried to register event handler with an invalid type " + handler.getClass().getName());
        }
        this.type = type;
        this.priority = priority;
    }

    /**
     * Register the handler with the event manager
     * @param pm plugin manager to register with
     * @param plugin plugin to register as
     */
    void register(PluginManager pm, PythonPlugin plugin) {
        try {
            if (currentlyRegistered)
                throw new IllegalStateException("Attempting to register an already registered handler");
            else {
                plugin.listener.addHandler(type, this);
            }
            currentlyRegistered = true;
        } catch (IllegalStateException ex) { //I'm a lazyfuck
            plugin.getServer().getLogger().log(Level.SEVERE, "Error while registering events for PythonPlugin " + plugin.getDescription().getFullName() + " - did you register the same event type twice? " + ex.getMessage(), ex);
        }
    }
}
