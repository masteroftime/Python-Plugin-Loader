writing plugins
===============

Writing plugins with PythonLoader is fairly easy. There are two apis, both
of which are pretty simple; The first is the bukkit api, which this loader
lightly wraps; and the other is a decorators-and-functions api.

Basics
------

Your plugins go in either a zip or a directory (known to windows users as "folders");
that zip or directory name must match this regex: \.py\.?(dir|zip|p|pl|plug|plugin)$

if you need to use more than one python file for your plugin, either use a
directory or use a zip and name it .py.zip. for internal reasons (see: zipimport),
strongly recommend using .py.zip for all zips.


Class (bukkit standard) API
---------------------------

To write a plugin with this api is almost identical to writing one in java, so
much so that you can safely use the documentation on how to write a java
plugin; simply translate it into python. the java2py tool may even work on
existing java plugins (though no promises).

your plugin.yml:

    name: MyHawtPlugin
    main: MyPlugin
    version: 0.1

your plugin.py:

    class SampleClass(PythonPlugin):
        def onEnable():
            print "enabled!"
        def onDisable():
            print "disabled!"

Decorator API
-------------

Writing a plugin with this api is much more concise, as you need to declare no
classes:

your plugin.yml:

    name: MyHawtPlugin
    main: main.py
    version: 0.1

your main.py:

    print "main.py run"
    
    @hook.enable
    def onenable():
        print "main.py enabled"
    
    @hook.disable
    def ondisable():
        print "main.py disabled"
    
    @hook.event("player_join", "normal")
    def playerjoin(event):
        event.getPlayer().sendMessage("Hello from python")
    
    @hook.command
    def example(sender, command, label, args):
        sender.sendMessage("you just used comand /example!")


API Details
===========

The api contains quite a few places where you can do things multiple ways. This
section documents these.

Plugin files
------------

Your plugin belongs in either a zip or a directory (known to windows lusers as
"folders"). This zip or directory goes in the bukkit plugins/ directory along
with the java plugins and config directories. It must end with one of the
allowed extensions; see the appropriate section for a list, or just use one of
.py.dir, .py.zip, and .pyp (pyp should be used when releasing plugins).

Your plugin file must contain a main python file, and optionally a plugin.yml
containing metadata (see the following section). Your python main file normally
should be named either plugin.py or main.py. plugin.py should generally be used
when you are using the class api and main.py when using the decorator api.

Plugin metadata
---------------

Plugins require metadata. The absolute minimum metadata is a name and a version.
The location of your main file/class is also required, if you don't like
defaults. The 'main' field of plugin metadata has special behavior:

- if the main is set in plugin.yml, it searches for the value set in main as
   the main file before searching for the default file names. see "Main files".
- the main is used to search for a main class before searching the default
   class name.

There are three places you can put this metadata. In order of quality:

- plugin.yml
- your main python file
- your plugin filename

plugin.yml is the best as you are able to set all metadata fields that exist
in bukkit, and should be used for all plugins that you release. plugin.yml is
used in all java plugins (as it is the only option for java plugins). as such,
opening up java plugin jars is a good way to learn what can go in it. Here is
an example of plugin.yml:

    name: SamplePlugin
    main: SampleClass
    version: 0.1-dev
    commands:
        samplecommand:
            description: send a sample message
            usage: /<command>

The plugin filename is automatically used if no plugin.yml is found. The
extension is removed from the filename and used as the "name" field.
The version field is set to "dev" (as this case should only occur when first
creating a plugin). the main field is set to a default value that has no
effect.

The plugin main python file can be used if (and only if) you do not have a
plugin.yml file, so that you can override the defaults set by the plugin
filename case. It is recommended that you set these values at the top of your
main python file. None of these values are required. These are the values you
can set:

    __plugin_name__ = "SamplePlugin"
    __plugin_version__ = "0.1-dev"
    __plugin_mainclass__ = "SampleClass"
    __plugin_website__ = "http://example.com/sampleplugin"

note that plugin_mainclass can only be used to set the main class; it
cannot be used to set the main python file, as it must be contained in the
main python file. if you want to change the main python file, you must have a
plugin.yml.

Summary of fields:

- "main" - name of main python file or name of main class
- "name" - name of plugin to show in /plugins list and such. used to name the
   config directory. for this reason it must not equal the full name of the
   plugin file.
- "version" - version of plugin. shown in errors, and other plugins can access it
- "website" - mainly for people reading the code


Specifications for sample plugin
--------------------------------

- name is "SamplePlugin"
- main class, if applicable, should be "SampleClass"
- version is "0.1-dev"
- should print "sample plugin main file run" when loaded by the interpreter
- should print "sample plugin main class instantiated" when the main class is
   instantiated, if applicable
- should print "sample plugin enabled" when enabled
- should print "sample plugin disabled" when disabled
- should print and reply "sample plugin command" when the sample command
   "/samplecommand", is used
- sample command should have usage "/<command>" and should have description
   "send a sample message"
- should print and send message "welcome from the sample plugin, %s" % username
   when a player joins the server

Sample plugin using decorator api
---------------------------------

main.py:

    __plugin_name__ = "SamplePlugin"
    __plugin_version__ = "0.1-dev"
    
    @hook.enable
    def onEnable():
        print "sample plugin enabled"
    
    @hook.disable
    def onDisable():
        print "sample plugin disabled"
    
    @hook.event("player_join", "normal")
    def onPlayerJoin(event):
        msg = "welcome from the sample plugin, %s" % event.getPlayer().getName()
        print msg
        event.getPlayer().sendMessage(msg)
    
    @hook.command("samplecommand", usage="/<command>", 
                    desc="send a sample message")
    def onSampleCommand(sender, command, label, args):
        msg = "sample plugin command"
        print msg
        sender.sendMessage(msg)
        return True
    
    print "sample plugin main file run"

Sample plugin using class api
-----------------------------

plugin.yml:

    name: SamplePlugin
    main: SampleClass
    version: 0.1-dev
    commands:
        samplecommand:
            description: send a sample message
            usage: /<command>

plugin.py:

    from org.bukkit.event.player import PlayerListener
    from org.bukkit.event.Event import Type, Priority
    
    class SampleClass(PythonPlugin):
        def __init__(self):
            self.listener = SampleListener(self)
    
        def onEnable(self):
            pm = self.getServer().getPluginManager()
            pm.registerEvent(Type.PLAYER_PICKUP_ITEM, listener, Priority.Normal, self)
            pm.registerEvent(Type.PLAYER_RESPAWN, listener, Priority.Normal, self)
            
            print "sample plugin enabled"
        
        def onDisable(self):
            print "sample plugin disabled"
        
        def onCommand(self, sender, command, label, args):
            msg = "sample plugin command"
            print msg
            sender.sendMessage(msg)
            return True
    
    class SampleListener(PlayerListener):
        def __init__(self, plugin):
            self.plugin = plugin
        
        def onPlayerJoin(self, event):
            msg = "welcome from the sample plugin, %s" % event.getPlayer().getName()
            print msg
            event.getPlayer().sendMessage(msg)
    
    print "sample plugin main file run"
