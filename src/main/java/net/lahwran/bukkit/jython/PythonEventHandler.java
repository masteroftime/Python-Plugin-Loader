/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.util.logging.Level;

import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.python.core.PyFunction;

/**
 * Class to wrap python functions so they can be used to handle events
 * @author lahwran
 *
 */
class PythonEventHandler {
    /**
     * Python function to call
     */
    final PyFunction handler;

    /**
     * Event type this handler is listening for
     */
    final Event.Type type;

    /**
     * Priority to register the handler at
     */
    final Event.Priority priority;

    /**
     * Whether we've registered yet
     */
    boolean currentlyRegistered = false;

    /**
     * @param handler Python function to call
     * @param type Event type this handler is listening for
     * @param priority Priority to register the handler at
     */
    PythonEventHandler(PyFunction handler, Event.Type type, Event.Priority priority) {
        this.handler = handler;
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
            if (plugin.listener.handlers.get(type) != null)
                throw new IllegalStateException("Attempting to register event type '"+type+"' on top of another handler");
            pm.registerEvent(type, plugin.listener, priority, plugin);
            plugin.listener.handlers.put(type, this);
            currentlyRegistered = true;
        } catch (IllegalStateException ex) { //I'm a lazyfuck
            plugin.getServer().getLogger().log(Level.SEVERE, "Error while registering events for PythonPlugin " + plugin.getDescription().getFullName() + " - did you register the same event type twice? " + ex.getMessage(), ex);
        }
    }
}
