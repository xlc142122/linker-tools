package com.linker.tools.engine.loganalyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.linker.tools.util.ZipUtil;

/**
 * Created by Pasenger on 2014/9/3.
 */
public class EngineMsgGather {
    private static List<String> list = new ArrayList<String>();

    private static  final String flag = "RuleCore";
    private static final int flagLen = flag.length();

    public static void main(String[] args) {
        File file = new File("E:\\temp\\2014-09-13\\fe1\\0650");
        if(!file.exists()){
            System.out.println("日志路径不存在！");
            return;
        }

        analyzeLogList(file);
    }

    private static void output(){
        FileWriter fw = null;
        try {
            fw = new FileWriter("E:\\temp\\msg.txt", true);
            StringBuilder sb = new StringBuilder();
            for(String line: list){
                sb.append(line).append("\n");
            }

            fw.write(sb.toString());
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        list.clear();
    }


    private static void analyzeLogList(File logPathDir){
        ZipUtil zipUtil = new ZipUtil();

        File[] fileList = logPathDir.listFiles();
        for(File logFile : fileList){
            String logFilePath = logFile.getPath();
            if(logFilePath.endsWith(".zip")){
                try {
                    logFilePath = zipUtil.unZipFile(logFile, "E:/temp/msg/");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                gatherLog(logFilePath, true);
            }else if(logFilePath.endsWith(".log")){
                gatherLog(logFilePath, false);
            }else{
                System.out.println("非日志文件:" + logFilePath);
            }

            //System.out.println("分析日志：" + logFilePath + " 完成");

            if(list.size() > 100){
                output();
            }
        }

        output();
    }

    private static void gatherLog(String logFilePath, boolean isDelete){
        System.out.println(logFilePath);
        File file = new File(logFilePath);

        FileReader fr = null;
        BufferedReader br = null;

        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);

            String line = null;
            while((line = br.readLine()) != null){
                if(line.indexOf("DomainRouteActor - 75 - Received message") != -1 && line.indexOf("INITIAL_REQUEST") != -1){
                    line = line.trim();
                    String ret = line.substring(line.indexOf("Received message: [") + 19, line.length() - 1);
                    System.out.println(ret);
                    list.add(ret);
                }
            }

            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(isDelete){
            file.delete();
        }
    }
}
