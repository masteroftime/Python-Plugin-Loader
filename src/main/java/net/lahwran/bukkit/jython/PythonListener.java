/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.util.HashMap;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.python.core.Py;

/**
 * Special listener to handle events that were registered with PythonHooks.
 * @author lahwran
 *
 */
public class PythonListener implements Listener {

    /**
     * handlers registered that this listener needs to handle
     */
    HashMap<Event.Type, PythonEventHandler> handlers = new HashMap<Event.Type, PythonEventHandler>();

    /**
     * Handle an event. Depends on jython being smart enough to cast the event
     * down to whatever it is. Hint: jython is smart enough.
     * @param e event
     */
    void onEvent(Event e) {
        PythonEventHandler handler = handlers.get(e.getType());
        if (handler == null)
            return;
        handler.handler.__call__(Py.java2py(e));
    }

}
