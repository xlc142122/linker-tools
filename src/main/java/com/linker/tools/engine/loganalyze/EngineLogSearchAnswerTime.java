package com.linker.tools.engine.loganalyze;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.linker.tools.util.ParamAnalyze;
import com.linker.tools.util.ZipUtil;

/**
 * Log搜索工具
 * 支持zip压缩包, .log结尾的日志文件
 * Created by Pasenger on 2014/8/28.
 */
public class EngineLogSearchAnswerTime {
    private static final String HELPMSG =
            "\nExample:\n" +
                    "\tjava -jar logSearchTool.jar -i /var/log/tmp -s \"abc,123\"\n" +
                    "\nParameters description:\n" +
                    "\t-i logDir log文件所在路径，支持.log, .zip结尾文件\n" +
                    "\t-s 需要搜索的字符串，多个之间用\",\"分割\n" +
                    "\t-o outputFileName  输出文件名称,包含路径，如：/etc/log/searchResult.log\n" +
                    "\t-h 显示帮助\n\n";

    public static void main(String[] args) throws ParseException {
        if(args == null || args.length == 0){
            System.out.println(HELPMSG);
            return;
        }

        Map<String, String> argsMap = ParamAnalyze.analyzeParam(args);
        if(argsMap == null || argsMap.size() == 0){
            System.out.println(HELPMSG);
            return;
        }

        String logDir = argsMap.get("-i");
        //File logDir = new File(argsMap.get("-i"));

        if(logDir == null){
            System.out.println("logPath is not exists!");
            return;
        }

        if(!argsMap.containsKey("-s")){
            System.out.println(HELPMSG);

            return;
        }

        EngineLogSearchAnswerTime search = new EngineLogSearchAnswerTime();
        List<String> searchStringList = new ArrayList<String>();
        String searchStr = argsMap.get("-s");
        //searchStr = "pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org;1407959416;3714792;10069562";
        if(searchStr.indexOf(",") != -1){
            String[] array = searchStr.split(",");

            for(String s:array){
                searchStringList.add(s);
            }
        }else{
            searchStringList.add(searchStr);
        }


        ZipUtil zipUtil = new ZipUtil();

        String[] dirs = logDir.split(",");
        for(String dir : dirs){
            File dirFile = new File(dir);
            if(!dirFile.exists()){
                continue;
            }
            File[] files = dirFile.listFiles();

            List<File> fileList = new ArrayList<File>();
            for(File subFile:files){
                fileList.add(subFile);
            }

            Collections.sort(fileList, new Comparator<File>() {
                public int compare(File o1, File o2) {
                    if (o1.isDirectory() && o2.isFile())
                        return -1;
                    if (o1.isFile() && o2.isDirectory())
                        return 1;
                    return o1.getName().compareTo(o2.getName());
                }
            });

            System.out.println("文件数量：" + fileList.size());

            //File[] fileList = logDir.listFiles();
            for(File logFile : fileList){
                String logFilePath = logFile.getPath();
                if(logFilePath.endsWith(".zip")){
                    try {
                        logFilePath = zipUtil.unZipFile(logFile, logFile.getParent());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    search.searchLog(logFilePath, searchStringList, true);
                }else if(logFilePath.endsWith(".log")){
                    search.searchLog(logFilePath, searchStringList, false);
                }
            }
        }
    }

    private void searchLog(String filePath, List<String> aimStringList, boolean deleteOnFinish){
        boolean isExist = false;

        Vector<String> searchRetList = new Vector<String>();
        File file = new File(filePath);
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while((line = br.readLine()) != null){
                for(String s : aimStringList){
                    if(line.indexOf("DomainSendActor - 100 - Send back message") != -1 && line.indexOf(s) != -1){
                        String sessionId;
                        int sidStart = line.indexOf("\"sessionId\":\"") + 13;
                        int sidEnd = line.indexOf("\",", sidStart);
                        sessionId = line.substring(sidStart, sidEnd);

                        String timeStamp = line.substring(0,23);
                        System.out.println(sessionId + "\t" + timeStamp);

                        isExist = true;
                        break;
                    }
                }
            }

            fr.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(isExist){
            System.out.println(filePath);
        }

        if(deleteOnFinish){
            file.delete();
        }

        //输出
//        if(searchRetList != null && searchRetList.size() > 0){
//            output(searchRetList, outputFile);
//        }
    }

    private void output(Vector<String> outputList, String outputFile){
        File file = new File(outputFile);

        StringBuilder sb = new StringBuilder();
        for(String str : outputList){
            sb.append(str).append("\n");
        }

        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(sb.toString());

            bw.flush();

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
