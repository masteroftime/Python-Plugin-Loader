package bukkit.python;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import bukkit.python.PythonPluginLoader;

public class PythonLoader extends JavaPlugin
{

	@Override
	public void onDisable() {
		
	}

	@Override
	public void onEnable() {
		System.out.println("Enabling Python Plugin loader.");
		getServer().getPluginManager().registerInterface(PythonPluginLoader.class);
		getServer().getPluginManager().loadPlugins(new File("plugins"));
	}
	
}
