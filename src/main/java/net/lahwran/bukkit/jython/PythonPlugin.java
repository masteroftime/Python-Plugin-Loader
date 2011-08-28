/**
 * 
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Event;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;

/**
 * @author lahwran
 *
 */
public class PythonPlugin implements Plugin {
private boolean isEnabled = false;
    private boolean initialized = false;
    private PluginLoader loader = null;
    private Server server = null;
    private File file = null;
    private PluginDescriptionFile description = null;
    private File dataFolder = null;
    //private ClassLoader classLoader = null;
    private Configuration config = null;
    private boolean naggable = true;
    private EbeanServer ebean = null;

    private PyFunction onEnable;
    private PyFunction onDisable;

    PythonListener listener = new PythonListener();
    ArrayList<PythonEventHandler> eventhandlers = new ArrayList<PythonEventHandler>();
    ArrayList<PythonCommandHandler> commandhandlers = new ArrayList<PythonCommandHandler>();

    boolean delayRegistrations = true;

    //PythonInterpreter interp = new PythonInterpreter();

    /*static class TypeAndPriority {
        final Event.Type type;
        final Event.Priority priority;
        TypeAndPriority(Event.Type type, Event.Priority priority) {
            this.type = type;
            this.priority = priority;
        }

        public int hashCode() {
            
            return type.ordinal() + (priority.ordinal() << 10);
        }
    }*/

    public PythonPlugin() {}

    /**
     * Returns the folder that the plugin data's files are located in. The
     * folder may not yet exist.
     *
     * @return
     */
    public File getDataFolder() {
        return dataFolder;
    }

    /**
     * Gets the associated PluginLoader responsible for this plugin
     *
     * @return PluginLoader that controls this plugin
     */
    public final PluginLoader getPluginLoader() {
        return loader;
    }

    /**
     * Returns the Server instance currently running this plugin
     *
     * @return Server running this plugin
     */
    public final Server getServer() {
        return server;
    }

    /**
     * Returns a value indicating whether or not this plugin is currently enabled
     *
     * @return true if this plugin is enabled, otherwise false
     */
    public final boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Returns the file which contains this plugin
     *
     * @return File containing this plugin
     */
    protected File getFile() {
        return file;
    }

    /**
     * Returns the plugin.yaml file containing the details for this plugin
     *
     * @return Contents of the plugin.yaml file
     */
    public PluginDescriptionFile getDescription() {
        return description;
    }

    /**
     * Returns the main configuration located at
     * <plugin name>/config.yml and loads the file. If the configuration file
     * does not exist and it cannot be loaded, no error will be emitted and
     * the configuration file will have no values.
     *
     * @return
     */
    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Returns the ClassLoader which holds this plugin
     *
     * @return ClassLoader holding this plugin
     */
    //protected ClassLoader getClassLoader() {
    //    return classLoader;
    //}

    /**
     * Sets the enabled state of this plugin
     *
     * @param enabled true if enabled, otherwise false
     */
    protected void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    /**
     * Initializes this plugin with the given variables.
     *
     * This method should never be called manually.
     *
     * @param loader PluginLoader that is responsible for this plugin
     * @param server Server instance that is running this plugin
     * @param description PluginDescriptionFile containing metadata on this plugin
     * @param dataFolder Folder containing the plugin's data
     * @param file File containing this plugin
     * @param classLoader ClassLoader which holds this plugin
     */
    protected final void initialize(PluginLoader loader, Server server,
            PluginDescriptionFile description, File dataFolder, File file ) { //,
            //ClassLoader classLoader) {
        if (!initialized) {
            this.initialized = true;
            this.loader = loader;
            this.server = server;
            this.file = file;
            this.description = description;
            this.dataFolder = dataFolder;
            //this.classLoader = classLoader;
            this.config = new Configuration(new File(dataFolder, "config.yml"));
            this.config.load();

            /*if (description.isDatabaseEnabled()) {
                ServerConfig db = new ServerConfig();

                db.setDefaultServer(false);
                db.setRegister(false);
                db.setClasses(getDatabaseClasses());
                db.setName(description.getName());
                server.configureDbConfig(db);

                DataSourceConfig ds = db.getDataSourceConfig();

                ds.setUrl(replaceDatabaseString(ds.getUrl()));
                getDataFolder().mkdirs();

                ClassLoader previous = Thread.currentThread().getContextClassLoader();

                Thread.currentThread().setContextClassLoader(classLoader);
                ebean = EbeanServerFactory.create(db);
                Thread.currentThread().setContextClassLoader(previous);
            }*/
        }
    }

    /**
     * Provides a list of all classes that should be persisted in the database
     *
     * @return List of Classes that are Ebeans
     */
    public List<Class<?>> getDatabaseClasses() {
        return new ArrayList<Class<?>>();
    }

    private String replaceDatabaseString(String input) {
        input = input.replaceAll("\\{DIR\\}", getDataFolder().getPath().replaceAll("\\\\", "/") + "/");
        input = input.replaceAll("\\{NAME\\}", getDescription().getName().replaceAll("[^\\w_-]", ""));
        return input;
    }

    /**
     * Gets the initialization status of this plugin
     *
     * @return true if this plugin is initialized, otherwise false
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * {@inheritDoc}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    /**
     * Gets the command with the given name, specific to this plugin
     *
     * @param name Name or alias of the command
     * @return PluginCommand if found, otherwise null
     */
    public PluginCommand getCommand(String name) {
        String alias = name.toLowerCase();
        PluginCommand command = getServer().getPluginCommand(alias);

        if ((command != null) && (command.getPlugin() != this)) {
            command = getServer().getPluginCommand(getDescription().getName().toLowerCase() + ":" + alias);
        }

        if ((command != null) && (command.getPlugin() == this)) {
            return command;
        } else {
            return null;
        }
    }

    public void onLoad() {} // Empty!

    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        getServer().getLogger().severe("Plugin " + getDescription().getFullName() + " does not contain any generators that may be used in the default world!");
        return null;
    }

    public final boolean isNaggable() {
        return naggable;
    }

    public final void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }

    public EbeanServer getDatabase() {
        return ebean;
    }

    protected void installDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(false, gen.generateCreateDdl());
    }

    protected void removeDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(true, gen.generateDropDdl());
    }

    @Override
    public String toString() {
        return getDescription().getFullName();
    }

    public void onEnable() {
        onEnable.__call__();
        delayRegistrations = false;
        registerListenerEvents();
        registerHandlerCommands();
    }

    public void onDisable() {
        onDisable.__call__();
        delayRegistrations = true;
    }


    private void registerListenerEvents() {
        PluginManager pm = getServer().getPluginManager();
        for (int i=0; i<eventhandlers.size(); i++) {
            eventhandlers.get(i).register(pm, this);
        }
    }

    private void registerHandlerCommands() {
        for (int i=0; i<commandhandlers.size(); i++) {
            commandhandlers.get(i).register(this);
        }
    }

    public void registerEvent(PyFunction handler, Event.Type type, Event.Priority priority) {
        PythonEventHandler wrapper = new PythonEventHandler(handler, type, priority);
        eventhandlers.add(wrapper);
        if (!delayRegistrations) {
            wrapper.register(getServer().getPluginManager(), this);
        }
    }

    public void registerEvent(PyFunction handler, PyString type, PyString priority) {
        Event.Type realtype = Event.Type.valueOf(type.upper());
        Event.Priority realpriority = Event.Priority.valueOf(priority.capitalize());
        registerEvent(handler, realtype, realpriority);
    }

    public void registerCommand(PyFunction func, String name) {
        PythonCommandHandler handler = new PythonCommandHandler(func, name);
        commandhandlers.add(handler);
        if (!delayRegistrations) {
            handler.register(this);
        }
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
