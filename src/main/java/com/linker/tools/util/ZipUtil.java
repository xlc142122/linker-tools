package com.linker.tools.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * ZIP压缩工具类
 */
public class ZipUtil {
    public void doZip(String sourceFileName, String zipFileName)
            throws IOException {
        System.out.println("开始压缩文件：" + sourceFileName);
        File file = new File(sourceFileName);
        File zipFile = new File(zipFileName);
        ZipOutputStream zos = null;
        try {
            // 创建写出流操作
            OutputStream os = new FileOutputStream(zipFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            zos = new ZipOutputStream(bos);

            String basePath = null;

            // 获取目录
            if (file.isDirectory()) {
                basePath = file.getPath();
            } else {
                basePath = file.getParent();
            }

            zipFile(file, basePath, zos);
        } finally {
            if (zos != null) {
                zos.closeEntry();
                zos.close();
            }
        }

        System.out.println("压缩文件完成：" + sourceFileName);
    }

    /**
     * @param source   源文件
     * @param basePath
     * @param zos
     */
    private void zipFile(File source, String basePath, ZipOutputStream zos)
            throws IOException {
        File[] files = null;
        if (source.isDirectory()) {
            files = source.listFiles();
        } else {
            files = new File[1];
            files[0] = source;
        }

        InputStream is = null;
        String pathName;
        byte[] buf = new byte[1024];
        int length = 0;
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    pathName = file.getPath().substring(basePath.length() + 1)
                            + "/";
                    zos.putNextEntry(new ZipEntry(pathName));
                    zipFile(file, basePath, zos);
                } else {
                    pathName = file.getPath().substring(basePath.length() + 1);
                    is = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    zos.putNextEntry(new ZipEntry(pathName));
                    while ((length = bis.read(buf)) > 0) {
                        zos.write(buf, 0, length);
                    }

                    bis.close();
                }
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }

    /**
     * 解压到指定目录
     *
     * @param zipPath
     * @param descDir
     * @author isea533
     */
    public String unZipFile(String zipPath, String descDir) throws IOException {
        return unZipFile(new File(zipPath), descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param zipFile
     * @param descDir
     * @author isea533
     */
    @SuppressWarnings("rawtypes")
    public String unZipFile(File zipFile, String descDir) throws IOException {
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }

        String outPath = null;
        ZipFile zip = new ZipFile(zipFile);
        for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String zipEntryName = entry.getName();
            InputStream in = zip.getInputStream(entry);
            outPath = pathFile.getPath() + File.separator + zipEntryName;

            if (new File(outPath).isDirectory()) {
                continue;
            }

            OutputStream out = new FileOutputStream(outPath);
            byte[] buf1 = new byte[1024];
            int len;
            while ((len = in.read(buf1)) > 0) {
                out.write(buf1, 0, len);
            }
            out.flush();
            in.close();
            out.close();
        }

        //System.out.println("******************解压完毕********************");

        return outPath;
    }

    public static void main(String[] args) {
        ZipUtil zipUtil = new ZipUtil();
        try {
            String unzipName = zipUtil.unZipFile("E:\\temp\\engine-jx\\fe1\\092106\\e1\\pcrf-engine_2014092107_96.log.zip", "E:\\temp\\engine-jx\\fe1\\092106\\e1");
            System.out.println(unzipName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
