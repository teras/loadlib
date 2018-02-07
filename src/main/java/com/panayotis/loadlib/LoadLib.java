/*
 * Loadlib makes easy to load native libraries for Java packed as JAR files.
 * Copyright (C) 2018  Panayotis Katsaloulis

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.panayotis.loadlib;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * @author teras
 */
public class LoadLib {

    public static boolean load(String resourcepath) {
        return load(resourcepath, (File) null);
    }

    @SuppressWarnings("UseSpecificCatch")
    public static boolean load(Class signatureClass, String resourcepath) {
        if (resourcepath == null || signatureClass == null)
            return load(resourcepath, (File) null);
        File outFile;
        try {
            URL location = signatureClass.getProtectionDomain().getCodeSource().getLocation();
            outFile = new File(System.getProperty("user.home") + File.separator
                    + ".cache" + File.separator + "loadlib"
                    + File.separator + "libs" + File.separator
                    + URLEncoder.encode(location.toString(), "UTF-8")
                    + File.separator + geFileName(resourcepath));
            File origFile = getFileFromURL(location);
            if (origFile != null && outFile.isFile() && origFile.lastModified() > outFile.lastModified())
                outFile.delete();
        } catch (Throwable e) {
            outFile = null;
        }
        return load(resourcepath, outFile);
    }

    private static boolean load(String resourcepath, File destLib) {
        File fileOut = null;
        try {
            if (resourcepath == null)
                return false;
            fileOut = destLib == null ? tempLibLoc(geFileName(resourcepath)) : destLib;
            if (fileOut == null)
                return false;
            fileOut.getParentFile().mkdirs();
            if (!fileOut.isFile())
                dumpLib(fileOut, resourcepath);
            System.load(fileOut.getAbsolutePath());
            if (destLib == null)
                fileOut.deleteOnExit();
            return true;
        } catch (Throwable th) {
            if (fileOut != null)
                fileOut.delete();
            return false;
        }
    }

    private static File tempLibLoc(String name) {
        File temp;
        try {
            temp = File.createTempFile("embed", "lib");
        } catch (IOException e) {
            return null;
        }
        temp.delete();
        return new File(temp, name);
    }

    private static String geFileName(String resourcepath) {
        return resourcepath.substring(resourcepath.lastIndexOf('/') + 1, resourcepath.length());
    }

    private static boolean dumpLib(File fileOut, String resourcepath) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            byte[] buffer = new byte[0x16000];
            out = new BufferedOutputStream(new FileOutputStream(fileOut), buffer.length);
            in = new BufferedInputStream(LoadLib.class.getResourceAsStream(resourcepath), buffer.length);
            int size;
            while ((size = in.read(buffer)) >= 0)
                out.write(buffer, 0, size);
            out.flush();
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException ex) {
                }
            if (out != null)
                try {
                    out.close();
                } catch (IOException ex) {
                }
        }
    }

    private static File getFileFromURL(URL url) {
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f.isFile() ? f : null;
    }

    public static String getLibName(String path, String basename) {
        String bits = System.getProperty("os.arch", "").contains("64") ? "64" : "32";
        String os = System.getProperty("os.name", "").toLowerCase();
        String prefix = os.contains("windows") ? "" : "lib";
        String suffix = os.contains("windows") ? ".dll" : (os.contains("mac") ? ".dylib" : ".so");
        return (path.startsWith("/") ? "" : "/")
                + path
                + (path.endsWith("/") ? "" : "/")
                + prefix
                + basename
                + bits
                + suffix;
    }
}
