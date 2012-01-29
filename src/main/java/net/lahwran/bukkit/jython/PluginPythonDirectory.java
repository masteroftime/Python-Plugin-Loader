/**
 *
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author lahwran
 *
 */
public class PluginPythonDirectory extends PluginDataFile {

    /**
     * directory we represent
     */
    private final File dir;

    /**
     * @param dir directory we represent
     */
    public PluginPythonDirectory(File dir) {
        this.dir = dir;
    }

    @Override
    public InputStream getStream(String filename) throws IOException {
        File f = new File(dir, filename);
        if (!f.exists())
            return null;
        return new FileInputStream(f);
    }

    @Override
    public boolean shouldAddPathEntry() {
        return true;
    }

    @Override
    public boolean getNeedsSolidMeta() {
        return false;
    }

}
