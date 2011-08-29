Issues in original
------------------

1. python plugins were loaded at a different time than java plugins;
   on initialization, reloaded all already-loaded plugins (meaning all java plugins);
   could be initialized twice (not important with SimplePluginManager, but since it's not true of bukkit-internal initialization, is not acceptable)
2. dependency loading only attempts to work for other python plugins - completely ignores java plugins;
   however, does not work for python plugins, as it does not add python plugins to the map it checks:
   python plugins with dependencies will cause an infinite loop on startup!
3. errors while a plugin is initializing will result in an uninitialized plugin being loaded into bukkit
4. backslashes and python string special characters in the plugin's file name will result in an error while loading the plugin,
   due to altering the pythonpath via interp.exec()
5. the data folder is added to the pythonpath - all plugin code should be /inside/ the plugin
6. a single PythonInterpreter is used for all plugins, which means they share the same globals - all kinds of havoc, hard-to-pin-down errors, plugin-to-plugin sabotage, and other fun can come of this
7. entirely copies JavaPluginLoader's eventexecutor creation - which means it will have to be updated each time an event is added!

fixes currently in mine
-----------------------

1. moved initialization to occur as soon as PluginLoader is instantiated - ie, very early;
   check if initialization has occured before
2. keep a proper list of loaded python plugins; also check java plugin list
3. throw an InvalidPluginException any time the plugin loader has an error
4. manipulate PySystemState path directly (PySystemState is the implementation of the sys module)
5. don't add the data directory to the pythonpath - may break compatibility with a (very) few plugins
6. create a new PythonInterpreter for each plugin - tests show that this causes no significant speed loss
7. use reflection to call the existing java loader's eventexecutor creation

:>
