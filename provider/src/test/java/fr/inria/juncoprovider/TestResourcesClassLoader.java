package fr.inria.juncoprovider;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Load classes from the test resources directory
 *
 * Created by marcel on 23/03/14.
 */
public class TestResourcesClassLoader extends ClassLoader {

    /**
     * Resource dir where the classes are
     */
    private String resourceDir;
    public String getResourceDir() { return resourceDir; }
    public void setResourceDir(String resourceDir) { this.resourceDir = resourceDir; }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
            throws ClassNotFoundException {
        try {
            File f = new File(resourceDir + "/" + name + ".class");
            FileInputStream fi = new FileInputStream(f);
            ByteBuffer bf = ByteBuffer.allocate((int)f.length());
            fi.getChannel().read(bf);
            return defineClass(name, bf.array(), 0, bf.array().length);
        } catch ( IOException e ) {
            return super.loadClass(name, resolve);
        }
    }


}
