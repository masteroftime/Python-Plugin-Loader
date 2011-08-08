package com.master.bukkit.python;

import java.io.File;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.python.PythonPlugin;
import org.bukkit.plugin.python.PythonPluginLoader;

public class PythonLoader extends JavaPlugin
{

	@Override
	public void onDisable() {
		
	}

	@Override
	public void onEnable() {
		System.out.println("Enabling Python Plugin loader.");
		
		//load all plugin classes that they are managed by the same plugin loader
		try {
			Class.forName("org.bukkit.plugin.python.PythonPlugin");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			int i = 1;
			while(true)
			{
				Class.forName("org.bukkit.plugin.python.PythonPluginLoader$"+i);
				i++;
			}
		} catch (ClassNotFoundException e) {
			
		}
		
		File pluginDir = new File("plugins/python");
		if(!pluginDir.exists()) pluginDir.mkdir();
		
		getServer().getPluginManager().registerInterface(PythonPluginLoader.class);
		getServer().getPluginManager().loadPlugins(pluginDir);
		
		for(Plugin p : getServer().getPluginManager().getPlugins())
		{
			if(p instanceof PythonPlugin)
			{
				getServer().getPluginManager().enablePlugin(p);
			}
		}
	}
	
}
