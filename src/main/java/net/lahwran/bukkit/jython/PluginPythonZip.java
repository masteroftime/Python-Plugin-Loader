/**
 *
 */
package net.lahwran.bukkit.jython;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.plugin.InvalidPluginException;

/**
 * @author lahwran
 *
 */
public class PluginPythonZip extends PluginDataFile {

    /**
     * Zipfile we belong to
     */
    public final ZipFile zip;

    /**
     * @param file Zipfile we belong to
     * @throws InvalidPluginException thrown if there is an error opening zip
     */
    public PluginPythonZip(File file) throws InvalidPluginException {
        try {
            zip = new ZipFile(file);
        } catch (IOException e) {
            throw new InvalidPluginException(e);
        }
    }

    public void close() throws IOException {
        zip.close();
    }

    @Override
    public InputStream getStream(String filename) throws IOException {
        ZipEntry entry = zip.getEntry(filename);
        if (entry == null)
            return null;
        return zip.getInputStream(entry);
    }

    @Override
    public boolean shouldAddPathEntry() {
        return true;
    }

    @Override
    public boolean getNeedsSolidMeta() {
        return true;
    }
}
