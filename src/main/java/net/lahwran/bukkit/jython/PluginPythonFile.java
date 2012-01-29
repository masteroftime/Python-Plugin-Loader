/**
 *
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author lahwran
 *
 */
public class PluginPythonFile extends PluginDataFile {

    /**
     * .py file we represent
     */
    private final File file;

    /**
     * @param file .py file
     */
    public PluginPythonFile(File file) {
        this.file = file;
    }

    @Override
    public InputStream getStream(String filename) {
        if (filename.equals("plugin.py"))
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return null;
            }
        return null;
    }

    @Override
    public boolean shouldAddPathEntry() {
        return false;
    }

    @Override
    public boolean getNeedsSolidMeta() {
        return false;
    }

}
