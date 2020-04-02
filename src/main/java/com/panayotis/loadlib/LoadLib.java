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
import java.util.zip.GZIPInputStream;

/**
 * @author teras
 */
@SuppressWarnings("unused")
public class LoadLib {

    public static boolean load(String resourcePath) {
        return load(resourcePath, null);
    }

    @SuppressWarnings("UseSpecificCatch")
    public static boolean load(Class<?> signatureClass, String resourcePath) {
        if (resourcePath == null || signatureClass == null)
            return load(resourcePath, null);
        File outFile;
        try {
            URL location = signatureClass.getProtectionDomain().getCodeSource().getLocation();
            outFile = new File(System.getProperty("user.home") + File.separator
                    + ".cache" + File.separator + "loadlib"
                    + File.separator + "libs" + File.separator
                    + URLEncoder.encode(location.toString(), "UTF-8")
                    + File.separator + geFileName(resourcePath));
            File origFile = getFileFromURL(location);
            if (origFile != null && outFile.isFile() && origFile.lastModified() > outFile.lastModified())
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
        } catch (Throwable e) {
            outFile = null;
        }
        return load(resourcePath, outFile);
    }

    private static boolean load(String resourcepath, File destLib) {
        File fileOut = null;
        try {
            if (resourcepath == null)
                return false;
            fileOut = destLib == null ? tempLibLoc(geFileName(resourcepath)) : destLib;
            if (fileOut == null)
                return false;
            if (!fileOut.getParentFile().mkdirs())
                return false;
            if (!fileOut.isFile())
                if (!dumpLib(fileOut, resourcepath))
                    return false;
            System.load(fileOut.getAbsolutePath());
            if (destLib == null)
                fileOut.deleteOnExit();
            return true;
        } catch (Throwable th) {
            if (fileOut != null)
                //noinspection ResultOfMethodCallIgnored
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
        //noinspection ResultOfMethodCallIgnored
        temp.delete();
        return new File(temp, name);
    }

    private static String geFileName(String resourcePath) {
        return resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
    }

    private static void dumpData(InputStream in, @SuppressWarnings("SameParameterValue") int hm) throws IOException {
        for (int i = 0; i < hm; i++) {
            int read = -1;
            while (read < 0)
                read = in.read();
        }
    }

    private static boolean dumpLib(File fileOut, String resourcePath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            byte[] buffer = new byte[0x16000];
            out = new BufferedOutputStream(new FileOutputStream(fileOut), buffer.length);
            in = new BufferedInputStream(LoadLib.class.getResourceAsStream(resourcePath), buffer.length);
            if (resourcePath.endsWith(".gz"))
                in = new GZIPInputStream(in);
            else if (resourcePath.endsWith(".gb"))
                dumpData(in, 2);
            int readCount;
            int totalSize = 0;
            while ((readCount = in.read(buffer)) >= 0) {
                totalSize += readCount;
                out.write(buffer, 0, readCount);
            }
            out.flush();
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            if (out != null)
                try {
                    out.close();
                } catch (IOException ignored) {
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
