package com.linker.tools.util;

import java.io.File;

/**
 * Created by Pasenger on 2014/9/11.
 */
public class FileRename {
    public static void main(String[] args) {
        String path = "E:\\temp\\2014-09-11\\0650\\FE1\\log";
        File file = new File(path);
        File[] fileList = file.listFiles();
        for(File subFile:fileList){
            String name = subFile.getName();
            name = name.replace("pcrf-engine", "pcrf-engine-e0");
            System.out.println(name);
            subFile.renameTo(new File("E:\\temp\\2014-09-11\\0650\\FE1\\log\\" + name));
        }
    }
}
