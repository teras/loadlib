/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.loadlib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author teras
 */
public class LoadLib {

    @SuppressWarnings("UseSpecificCatch")
    public static boolean load(String resourcepath) {
        byte[] buffer = new byte[0x1000];
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            String name = resourcepath.substring(resourcepath.lastIndexOf('/') + 1, resourcepath.length());
            File temp = File.createTempFile("embed", "lib");
            temp.delete();
            File fout = new File(temp, name);
            fout.getParentFile().mkdirs();

            out = new BufferedOutputStream(new FileOutputStream(fout), buffer.length);
            in = new BufferedInputStream(LoadLib.class.getResourceAsStream(resourcepath), buffer.length);
            int size;
            while ((size = in.read(buffer)) >= 0)
                out.write(buffer, 0, size);
            in.close();
            out.flush();
            out.close();

            System.load(fout.getAbsolutePath());
            fout.deleteOnExit();
            return true;
        } catch (Throwable ex) {
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
}
