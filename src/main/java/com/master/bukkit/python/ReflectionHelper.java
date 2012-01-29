/**
 *
 */
package com.master.bukkit.python;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPluginLoader;

/**
 * @author lahwran
 *
 */
public class ReflectionHelper {

    private static Map<Pattern, PluginLoader> fileAssociations = null;
    private static JavaPluginLoader javapluginloader = null;
    private static Map<String, ?> javaLoaders = null;


    /**
     * Retrieve SimplePluginManager.fileAssociations. inform the user if they're too cool for us
     * (ie, they're using a different plugin manager)
     * @param pm PluginManager to attempt to retrieve fileAssociations from
     * @param errorstr string to print if we fail when we print reason we failed
     * @return fileAssociations map
     */
    @SuppressWarnings("unchecked")
    public static Map<Pattern, PluginLoader> getFileAssociations(PluginManager pm, String errorstr) {
        if (fileAssociations != null)
            return fileAssociations;
        Class<?> pmclass = null;
        try {
            pmclass = Class.forName("org.bukkit.plugin.SimplePluginManager");
        } catch (ClassNotFoundException e) {
            printerr("Did not find SimplePluginManager", errorstr);
        } catch (Throwable t) {
            printerr("Error while checking for SimplePluginManager", errorstr);
            t.printStackTrace();
        }

        Field fieldFileAssociations = null;
        if (pmclass != null) {
            try {
                fieldFileAssociations = pmclass.getDeclaredField("fileAssociations");
            } catch (SecurityException e) {
                printerr("SecurityException while checking for fileAssociations field in SimplePluginManager", errorstr);
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                printerr("SimplePluginManager does not have fileAssociations field", errorstr);
            } catch (Throwable t) {
                printerr("Error while checking for fileAssociations field in SimplePluginManager", errorstr);
                t.printStackTrace();
            }
        }

        if (fieldFileAssociations != null) {
            try {
                fieldFileAssociations.setAccessible(true);
                fileAssociations = (Map<Pattern, PluginLoader>) fieldFileAssociations.get(pm);
            } catch (ClassCastException e) {
                printerr("fileAssociations is not of type Map<Pattern, PluginLoader>", errorstr);

            } catch (Throwable t) {
                printerr("Error while getting fileAssociations from PluginManager", errorstr);
                t.printStackTrace();
            }
        }
        return fileAssociations;
    }

    /**
     * Retrieve JavaPluginLoader from SimplePluginManager file associations
     * @param pm plugin manager
     * @return java plugin loader if found
     */
    public static JavaPluginLoader getJavaPluginLoader(PluginManager pm) {
        if (javapluginloader != null)
            return javapluginloader;

        getFileAssociations(pm, null);

        for (Entry<Pattern, PluginLoader> entry : fileAssociations.entrySet()) {
            if (entry.getKey().pattern().equals("\\.jar$"))
                javapluginloader = (JavaPluginLoader) entry.getValue();
        }

        return javapluginloader;
    }

    /**
     * Retrieve loaders field from JavaPluginLoader instance
     * @param pm plugin manager to search for JavaPluginLoader in (if necessary)
     * @return loaders field retrieved
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ?> getJavaLoaders(PluginManager pm) {
        if (javaLoaders != null)
            return javaLoaders;

        getJavaLoaders(pm);
        if (javapluginloader == null)
            return null;

        try {
            Field fieldLoaders = JavaPluginLoader.class.getDeclaredField("loaders");
            fieldLoaders.setAccessible(true);

            javaLoaders = (Map<String, ?>) fieldLoaders.get(javapluginloader);
            return javaLoaders;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Use JavaPluginLoader.loaders field to determine if JavaPluginLoader has loaded a plugin (false if unable to determine)
     * @param pm plugin manager to retrieve JavaPluginLoader instance from, if necessary
     * @param name name of plugin to search for
     * @return whether plugin is loaded
     */
    public static boolean isJavaPluginLoaded(PluginManager pm, String name) {
        getJavaLoaders(pm);
        if (javaLoaders == null)
            return false;
        return javaLoaders.containsKey(name);
    }


    /**
     * Set a private value. This version guesses the class with getClass().
     * @param obj object to set the field in
     * @param fieldName name of field to set
     * @param newValue new value to set the field to
     * @throws IllegalArgumentException can be thrown when setting the field
     * @throws IllegalAccessException can be thrown when setting the field
     * @throws SecurityException can be thrown when retrieving the field object
     * @throws NoSuchFieldException can be thrown when retrieving the field object
     */
    public static void setPrivateValue(Object obj, String fieldName, Object newValue) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
        setPrivateValue(obj.getClass(), obj, fieldName, newValue);
    }

    /**
     * Set a private value.
     * @param containingClass class containing the field
     * @param obj object to set the field in
     * @param fieldName name of field to set
     * @param newValue new value to set the field to
     * @throws IllegalArgumentException can be thrown when setting the field
     * @throws IllegalAccessException can be thrown when setting the field
     * @throws SecurityException can be thrown when retrieving the field object
     * @throws NoSuchFieldException can be thrown when retrieving the field object
     */
    public static void setPrivateValue(Class<?> containingClass, Object obj, String fieldName, Object newValue) throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException {
        Field field = containingClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, newValue);
    }

    private static void printerr(String cause, String issue) {
        if (issue != null)
            System.err.println("PythonLoader: "+cause+", "+issue);
    }
}
