/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.util.HashMap;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.python.core.Py;

/**
 * @author lahwran
 *
 */
public class PythonListener implements Listener {

    HashMap<Event.Type, PythonEventHandler> handlers = new HashMap<Event.Type, PythonEventHandler>();

    void onEvent(Event e) {
        PythonEventHandler handler = handlers.get(e.getType());
        if (handler == null)
            return;
        handler.handler.__call__(Py.java2py(e));
    }

}
