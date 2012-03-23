package net.lahwran.bukkit.jython;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Superclass for custom events in pyhton
 *  @author masteroftime
 */
public abstract class PythonCustomEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
