/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.util.logging.Level;

import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.python.core.PyFunction;

/**
 * @author lahwran
 *
 */
class PythonEventHandler {
    final PyFunction handler;
    final Event.Type type;
    final Event.Priority priority;
    boolean currentlyRegistered = false;

    PythonEventHandler(PyFunction handler, Event.Type type, Event.Priority priority) {
        this.handler = handler;
        this.type = type;
        this.priority = priority;
    }

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
