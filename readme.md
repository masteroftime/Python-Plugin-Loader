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


Bukkit API
----------

To write a plugin with this api is almost identical to writing one in java, so
much so that you can safely use the documentation on how to write a java plugin.

your plugin.yml:

    name: MyHawtPlugin
    main: MyPlugin
    version: 0.1

your plugin.py:

    class MyPlugin(PythonPlugin):
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
