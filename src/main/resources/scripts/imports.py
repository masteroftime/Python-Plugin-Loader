import net.lahwran.bukkit.jython.PythonPlugin as PythonPlugin
import net.lahwran.bukkit.jython.PythonCustomEvent as CustomEvent
import sys
import org.bukkit as bukkit
from java.lang import Class
from java.util.logging import Level
from java.io import File

class PyStdoutRedirect(object):
    def write(self, txt):
        if txt.endswith("\n"):
            sys.__stdout__.write(txt[:-1])
            sys.__stdout__.flush()
        else:
            sys.__stdout__.write(txt)

sys.stdout = PyStdoutRedirect()

server = bukkit.Bukkit.getServer()

class Log(object):
    prefix = ""
    logger = server.getLogger()

    @staticmethod
    def info(*text):
        Log.logger.log(Level.INFO,Log.prefix+" ".join(map(unicode,text)))

    @staticmethod
    def severe(*text):
        Log.logger.log(Level.SEVERE,Log.prefix+" ".join(map(unicode,text)))

    @staticmethod
    def msg(player,*text):
        player.sendMessage(Log.prefix+" ".join(map(unicode,text)))

log = Log