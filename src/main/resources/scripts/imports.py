import net.lahwran.bukkit.jython.PythonPlugin as PythonPlugin
import net.lahwran.bukkit.jython.PythonCustomEvent as CustomEvent
import sys
import org.bukkit as bukkit
from java.lang import Class
from java.util.logging import Level
from java.io import File

class PyStdoutRedirect:
    def write(self, txt):
        if txt.endswith("\n"):
            sys.__stdout__.write(txt[:-1])
            sys.__stdout__.flush()
        else:
            sys.__stdout__.write(txt)

sys.stdout = PyStdoutRedirect()

server = bukkit.Bukkit.getServer()

class log:
    prefix = ""
    logger = server.getLogger()

    @staticmethod
    def info(*text):
        log.logger.log(Level.INFO,log.prefix+" ".join(map(unicode,text)))

    @staticmethod
    def severe(*text):
        log.logger.log(Level.SEVERE,log.prefix+" ".join(map(unicode,text)))

    @staticmethod
    def msg(player,*text):
        player.sendMessage(log.prefix+" ".join(map(unicode,text)))
