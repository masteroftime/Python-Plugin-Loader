/**
 *
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.python.util.PythonInterpreter;

import com.avaje.ebean.EbeanServer;
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
    private FileConfiguration newConfig = null;
    private File configFile = null;
    private PluginLogger logger = null;
    private PluginDataFile dataFile = null; //data file used for retrieving resources
    
    /**
     * Listener to handle all PythonHooks events for this plugin.
     */
    PythonListener listener = new PythonListener();

    /**
     * PythonHooks registered on startup.
     */
    PythonHooks hooks;

    /**
     * interpreter that was used to load this plugin.
     */
    PythonInterpreter interp;

    /**
     * Returns the folder that the plugin data's files are located in. The
     * folder might not yet exist.
     *
     * @return data folder
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
     * @return The configuration.
     * @deprecated See the new
     */
    @Deprecated
    public Configuration getConfiguration() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        return config;
    }

    public FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    /**
     * Sets the enabled state of this plugin
     *
     * @param enabled true if enabled, otherwise false
     */
    protected void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                if (hooks.onEnable != null)
                    hooks.onEnable.__call__();
                hooks.doRegistrations(this);
                onEnable();
            } else {
                if (hooks.onDisable != null)
                    hooks.onDisable.__call__();
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
            this.config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));

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

//    private String replaceDatabaseString(String input) {
//        input = input.replaceAll("\\{DIR\\}", getDataFolder().getPath().replaceAll("\\\\", "/") + "/");
//        input = input.replaceAll("\\{NAME\\}", getDescription().getName().replaceAll("[^\\w_-]", ""));
//        return input;
//    }

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

    /**
     * Overridable, is called after all plugins have been instantiated.
     */
    public void onLoad() {}

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

    public Logger getLogger() {
        if (logger == null) {
            logger = new PluginLogger(this);
        }
        return logger;
    }

    @Override
    public String toString() {
        return getDescription().getFullName();
    }

    public void onEnable() { }

    public void onDisable() { }

    @Override
    public InputStream getResource(String filename) {
        if(filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            return dataFile.getStream(filename);
        } catch (IOException e) {
            //just return null and do not print stack trace as JavaPlugin's getResource does not print it either
            return null;
        }
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        // TODO is this necessary for use with PluginDataFile?
        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + getFile());
        }

        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                Logger.getLogger(JavaPlugin.class.getName()).log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    public void reloadConfig() {
        newConfig = YamlConfiguration.loadConfiguration(configFile);

        InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);

            newConfig.setDefaults(defConfig);
        }
    }

    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(PythonPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void saveDefaultConfig() {
        saveResource("config.yml", false);
    }

    protected void setDataFile(PluginDataFile file) {
        this.dataFile = file;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "PythonPlugin";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
            String alias, String[] args) {
        return null;
    }
}
