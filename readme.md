Python Plugin Loader
====================

The python plugin loader is a pluginloader for bukkit to load python plugins
via jython (and hopefully via jpype eventually). 


Using the plugin loader
-----------------------

Building
********


1. Get maven.
2. Run mvn clean package
3. Your product will be in target/


Running
*******

0. Ensure you are using a bukkit build that uses
   https://github.com/Bukkit/Bukkit/pull/335 - otherwise, only some of your
   plugins will work.
1. Put PyPluginLoader-0.2.jar in your bukkit/plugins/ dir
2. Put jython.jar in your bukkit/lib/ dir
3. [Re-]Start bukkit

Using plugins
*************

1. Stick the plugin.pyp in your bukkit/plugins/ dir
2. [Re-]Start bukkit

Writing plugins
===============

Writing plugins with PythonLoader is fairly easy. There are two apis, both
of which are pretty simple; The first is the bukkit api, which this loader
lightly wraps; and the other is a decorators-and-functions api.

Basics
------

Your plugins go in either a zip or a directory (known to windows users as "folders");
that zip or directory name must match this regex: \.py\.?(dir|zip|p|pl|plug|plugin)$


Class (bukkit standard) API
---------------------------

To write a plugin with this api is almost identical to writing one in java, so
much so that you can safely use the documentation on how to write a java
plugin; simply translate it into python. the java2py tool may even work on
existing java plugins (though no promises).

See the "Sample plugin using class api" section for a more detailed example.

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
    
    @hook.event("player.PlayerJoinEvent", "normal")
    def playerjoin(event):
        event.getPlayer().sendMessage("Hello from python")
    
    @hook.command
    def example(sender, command, label, args):
        sender.sendMessage("you just used command /example!")

See the "Sample plugin using decorator api" section for a more detailed example.


API Details
===========

The api contains quite a few places where you can do things multiple ways. This
section documents these.

Plugin files
------------

Your plugin may go in:

- A zip whos name ends in either .py.zip or .pyp
- A directory whose name ends in .py.dir or \_py_dir (for windows users)
- A python file (obviously, named .py)

Zips with the .pyp extension are recommended if you release any plugins. When
you use a zip, your must specify your own metadata; it will not allow guessed
metadata.

When using a dir or a zip, your zip or dir must contain a main python file and
optionally a plugin.yml containing metadata (see the following section). Your
python main file normally should be named either plugin.py or main.py.
plugin.py should generally be used when you are using the class api and main.py
when using the decorator api. Under some conditions you may want to change the
name of your main file (such as, other plugins needing to be able to import
it). This is not recommended but is possible with the main field in the
metadata.

When using a single .py file in plugins, your single .py is your main python
file. You cannot have a separate plugin.yml - if you want to have any special
metadata, you will need a directory or zip plugin.

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


Decorator api
-------------

The decorator api preloads an object called "hook" into your interpreter. This
object contains the decorators you can use. After first run, an object called
pyplugin is also inserted into your globals, so that it may be accessed from
functions. Some other stuff is also preloaded for you. The code runs something
like this:

    hook = PythonHooks()
    info = getPluginDescription()
    
    from pythonplugin import PythonPlugin
    
    # Your code happens here
    
    updateInfo()
    pyplugin = PythonPlugin();


Commands
********

Commands are added with the hook.command decorator. It can be used in both
no-arguments mode and in arguments mode. In no-arguments mode, it uses the
function name as the command name. In arguments mode, it takes one positional
argument, which is the command name, and three optional named arguments - desc
or description, usage, and aliases.

The decorator will attempt to register the command as though you had put it in
a plugin.yml file. If you do not provide any command metadata to the decorator
(that is, description, usage and aliases), then it will not be an error if the
command already exists, and it will not overwrite the existing metadata. 
However, if you provide metadata, then it will bail out if the command already
exists.

The decorated function may have one of these "signatures":
def func(sender, command, label, args):
def func(sender, label, args):
def func(sender, args):

Sender is the originator of the command; this might be a player, the console,
or something plugin-created. Command is the object representing this command;
this contains the metadata you set about the command. Label a string of the
command that the player actually called; this will usually be either the
command name or an alias. Args is the list of args to the command; normally
bukkit space-splits the arguments.

The command function must return a value that evaluates to true when it has
handled the command; if it does not, any other handlers that might have been
attached to the same command name will be executed. This includes the usage
printer.

some examples:

    @hook.command
    def samplecommand(sender, args):
        sender.sendMessage("You just used the sample command!")
        return True
    
    @hook.command
    def samplecommand2(sender, label, args):
        sender.sendMessage(label + " args: " + " ".join(args))
        return True
    
    @hook.command
    def samplecommand3(sender, command, label, args):
        sender.sendMessage("what would you EVER use command for? I'm sure there is something...")
        return True
    
    @hook.command("samplecommand4", desc="sexeh command", usage="/<command>",
                      aliases=["samplecommand5", "samplecommand6"])
    def samplecommand4(sender, label, args):
        sender.sendMessage("You just used teh sexeh command! "+label)
        return True

Note that you cannot do @hook.command():

    @hook.command()
    def thisWillError(sender, args):
        print "this plugin will not load."



Events
******

Events are registered with the hook.event decorator. This decorator may only
be used with arguments. It takes two arguments: the event type and priority.
Both are strings. Priority may be omitted. The event type has to be the class
name of the event plus the package which contains the class (e.g. player.PlayerChatEvent).
For a list of available events look into the org.bukkit.event package. Also 
note that the class name is case sensitive. If you want to register an event
that is not contained in the org.bukkit.event package you have to specify the
full path to the event (e.g. net.anotherplugin.events.CustomEvent).

The priority is one of the org.bukkit.event.EventPriority enumeration, this time case insensitive.

> Note:
> 
> Previously the event type was one of the event types in 
> org.bukkit.event.Event.Type, in any upper/lower case mix you liked and the
> priority was one of org.bukkit.event.Event.Priority, also in any case mix.
> However due to the introduction of the new EventHandling API Event.Type and
> Event.Priority are now deprecated and should not be used any more. Instead
> use the class name of the desired event and the EventPriority enumeration.

The decorated function must take exactly one argument: the event to handle.

It is worth noting that Bukkit handles priority in reverse: the highest
priority event handler is called last. Apparently they think this makes sense
because the highest priority handler should have the last say in whether the
event is cancelled ... well, in most of our world, we aren't wanting to cancel
the event, but to act on it, so what they think is forward is really reverse.


examples:

    @hook.event("player.PlayerJoinEvent", "normal")
    def onPlayerJoin(event):
        event.getPlayer().sendMessage("hello from python!")
    
    @hook.event("player.PlayerChatEvent", "monitor")
    def onPlayerChat(event):
        event.getPlayer().sendMessage("u r gey")

Enable and Disable
******************

Functions decorated with hook.enable and hook.disable are called when your
plugin is activated and deactivated, respectively. if you want your plugin to
be properly reloadable, you should clean up all your objects in your
hook.disable function.

examples:

    @hook.enable
    def onEnable():
        print "enabled!"
    
    @hook.disable
    def onDisable():
        print "disabled!"

Accessing the plugin object
***************************

The plugin instance is loaded into your globals as pyplugin.

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

main.py
*******


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

plugin.yml
**********

    name: SamplePlugin
    main: SampleClass
    version: 0.1-dev
    commands:
        samplecommand:
            description: send a sample message
            usage: /<command>

plugin.py
*********

    from org.bukkit.event.player import PlayerListener
    from org.bukkit.event.Event import Type, Priority
    
    class SampleClass(PythonPlugin):
        def __init__(self):
            self.listener = SampleListener(self)
            print "sample plugin main class instantiated"
    
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
