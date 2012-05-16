class MetaRegister(type):
    handlers = []
    registered = []
    commands = []

    def __new__(cls, name, bases, dct):
        cls = type.__new__(cls, name, bases, dct)
        for method in MetaRegister.getClassMethods(cls):
            MetaRegister.register(method)
        return cls

    def __call__(cls, *args, **kwargs):
        instance = super(MetaRegister, cls).__call__(*args, **kwargs)
        for method in MetaRegister.getInstanceMethods(instance):
            MetaRegister.register(method)
        return instance

    @staticmethod
    def getClassMethods(cls):
        methods = []
        for key,value in cls.__dict__.items():
            if not hasattr(value,"__class__"):
                continue
            if str(type(value)) == "<type 'classmethod'>" or str(type(value)) == "<type 'staticmethod'>":
                methods.append(getattr(cls,key))
        return methods

    @staticmethod
    def getInstanceMethods(instance):
        methods = []
        for name in dir(instance):
            try:
                attr = getattr(instance,name)
            except:
                pass
            if not hasattr(attr,"__class__"):
                continue

            if str(type(attr)) == "<type 'instancemethod'>":
                if hasattr(type(instance),name) and attr == getattr(type(instance),name):
                    continue
                methods.append(attr)
        return methods

    @staticmethod
    def register(func):
        if hasattr(func,'_event_handler'):
            hook.registerEvent(func,*getattr(func,'_event_handler'))
            try:
                MetaRegister.registered.append(func.im_func)
            except:
                MetaRegister.registered.append(func)
        elif hasattr(func,'_command_handler'):
            kwargs = getattr(func,'_command_handler')
            command = kwargs["command"]
            if command in MetaRegister.commands:
                log.severe("Command '%s' was registered more than once, ignoring further registrations"%command)
            else:
                hook.registerCommand(func,command,kwargs['usage'],kwargs['desc'],kwargs['aliases'])
                try:
                    MetaRegister.registered.append(func.im_func)
                except:
                    MetaRegister.registered.append(func)
                MetaRegister.commands.append(command)

    @staticmethod
    def registerPlugin(main):
        if main is not None:
            for method in MetaRegister.getClassMethods(main):
                MetaRegister.register(method)
            for method in MetaRegister.getInstanceMethods(pyplugin):
                MetaRegister.register(method)

    @staticmethod
    def registerStatic():
        for method in MetaRegister.handlers:
            if method not in MetaRegister.registered:
                MetaRegister.register(method)

class Listener(object):
    __metaclass__ = MetaRegister

import functools

def EventHandler(argument = None,priority = 'normal'):
    PRIORITIES = ["highest","high","normal","low","lowest","monitor"]
    def wrapper(func, eventtype, priority):
        if eventtype is None:
            try:
                name = func.__name__
            except AttributeError:
                name = func.__get__(None, int).im_func.__name__
            if name.startswith("on"):
                name = name[2:]
            for category in ["block","enchantment","entity","inventory","painting","player","server","vehicle","weather","world"]:
                temp = "%s.%sEvent"%(category,name)
                try:
                    Class.forName("org.bukkit.event."+temp)
                    eventtype = temp
                    break
                except:
                    pass
            if eventtype is None:
                log.severe("Incorrect @EventHandler usage on %s, could find no matching event"%func)
                return func
        try:
            func._event_handler = (eventtype,priority)
            MetaRegister.handlers.append(func)
        except AttributeError:
            func.__get__(None,int).im_func._event_handler = (eventtype,priority)
            MetaRegister.handlers.append(func.__get__(None,int).im_func)
        return func
    if callable(argument) or str(type(argument)) == "<type 'classmethod'>" or str(type(argument)) == "<type 'staticmethod'>":
        return wrapper(argument,None,priority)
    if argument.lower() in PRIORITIES:
        argument,priority = priority if priority.lower() not in PRIORITIES else None,argument
    return functools.partial(wrapper,eventtype=argument,priority=priority)

def CommandHandler(command = None, desc = None, usage = None, aliases = None):
    def wrapper(func, command, desc, usage, aliases):
        if command is None:
            try:
                command = func.__name__
            except AttributeError:
                command = func.__get__(None, int).im_func.__name__
        try:
            func._command_handler = {'command':command,'desc':desc,'usage':usage,'aliases':aliases}
            MetaRegister.handlers.append(func)
        except AttributeError:
            func.__get__(None,int).im_func._command_handler = {'command':command,'desc':desc,'usage':usage,'aliases':aliases}
            MetaRegister.handlers.append(func.__get__(None,int).im_func)
        return func
    if callable(command) or str(type(command)) == "<type 'classmethod'>" or str(type(command)) == "<type 'staticmethod'>":
        return wrapper(command, None,desc,usage,aliases)
    return functools.partial(wrapper,command = command,desc=desc,usage=usage,aliases=aliases)

__builtin__.Listener = Listener
__builtin__.EventHandler = EventHandler
__builtin__.CommandHandler = CommandHandler