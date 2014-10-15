package com.linker.tools.engine.loganalyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.linker.tools.util.ParamAnalyze;
import com.linker.tools.util.ZipUtil;

/**
 * 日志分析工具
 * 功能包含：
 * 1. 消息统计
 * 2. 重复消息记录
 * 3. TPS计算
 * Created by Pasenger on 2014/8/30.
 */
public class EngineLogAnalyze {

    private static final String HELPMSG =
            "\nExample:\n" +
            "\tjava -cp linkerTool.jar com.pasenger.linker.loganalyze.EngineLogAnalyze  -i /var/log/tmp -s \"\" -e \"\" -o \"log-1\" -t 1\n" +
            "\nParameters description:\n" +
            "\t-i logDir log文件所在路径，支持.log, .zip结尾文件\n" +
            "\t-d 按天日志分析开始时间，格式yyyyMMdd\n" +
            "\t-H 按天日志分析时小时数，可选择离散或连续，如1-3,5,8.,10\n" +
            "\t-s startTime 日志分析开始时间，格式yyyy-MM-dd HH:mm:ss.SSS\n" +
            "\t-e endTime  日志分析截止时间，格式yyyy-MM-dd HH:mm:ss.SSS\n" +
            "\t-o outputFilePath  输出文件路径，如：/etc/log\n" +
            "\t-p 时间粒度, 多长时间周期生成一个Excel文件：0：1小时；1：30分钟；2：15分钟，3：10分钟，4：5分钟， 默认为0\n" +
            "\t-t TPS统计粒度，可选：1：1s; 2: 100ms; 3:10ms\n" +
            "\t-h 显示帮助\n\n";

    //recevie flag
    private static final String RECEIVERMSGFLAG = "DomainRouteActor - 85 - Received message";   //接收消息日志标志

    //send back flag
    private static final String SENDMSGFLAG = "DomainSendActor - 100 - Send back message";  //发送消息标识

    //parameters
    //private static String logFileDir = null;
    private static List<String> logDirList = null;
    private static String day = null;
    private static Date startTime = null;
    private static Date endTime = null;
    private static String outputFile = null;
    private static int timePeriod = 1;
    private static int tpsPeriod = 1;
    private static List<Integer> hourList;

    //key: retMap key, value: timeStamp
    private static Map<String, String> sessionTimeStampMap = new HashMap<String, String>();

    //key:时间戳， value: retMap
    private static Map<String, Map<String, String[]>> retMap = new HashMap<String, Map<String, String[]>>();

    //key:是按戳， value: repeatDataList
    private static Map<String, List<String[]>> repeatDataMap = new HashMap<String, List<String[]>>();

    //private static Map<String, String[]> retMap = new HashMap<>(); //结果
    //private static List<String[]> repeatDataList = new ArrayList<>();

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        if (args == null || args.length == 0) {
            System.out.println(HELPMSG);
            return;
        }

        Map<String, String> argsMap = ParamAnalyze.analyzeParam(args);

        if(argsMap == null || argsMap.size() == 0){
            System.out.println("parameter input is null!");
            System.out.println(HELPMSG);
            return;
        }

        if(argsMap.containsKey("-h")){
            System.out.println(HELPMSG);
            return;
        }

        //EngineLogAnalyze engineLogAnalyze = new EngineLogAnalyze();
        logDirList = new ArrayList<String>();
        String logFileDir = argsMap.get("-i");
        if(logFileDir.indexOf(",") != -1){
            String[] array = logFileDir.split(",");
            for(String dir : array){
                logDirList.add(dir);
            }
        }else{
            logDirList.add(logFileDir);
        }

        if (argsMap.containsKey("-d")) {
            day = argsMap.get("-d");
        }

        if(day == null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            if (argsMap.containsKey("-s")) {
                try{
                    startTime = sdf.parse(argsMap.get("-s"));
                }catch(Exception e){
                    System.out.println("parameter: -s error: " + argsMap.get("-s"));
                    System.out.println(HELPMSG);

                    return;
                }
            }

            if (argsMap.containsKey("-e")) {
                try{
                    endTime = sdf.parse(argsMap.get("-e"));
                }catch(Exception e){
                    System.out.println("parameter: -s error: " + argsMap.get("-e"));
                    System.out.println(HELPMSG);

                    return;
                }
            }
        }else{
            if(argsMap.containsKey("-H")){
                String hours = argsMap.get("-H");
                String[] hourArray = hours.split(",");

                hourList = new ArrayList<Integer>();

                for(String hour : hourArray){
                    if(hour.indexOf("-") != -1){
                        String[] array = hour.split("-");
                        int sh = Integer.parseInt(array[0]);
                        int eh = Integer.parseInt(array[1]);

                        for(int i = sh; i <= eh; i++){
                            hourList.add(i);
                        }
                    }else{
                        hourList.add(Integer.parseInt(hour));
                    }
                }
            }
        }

        if(argsMap.containsKey("-p")){
            timePeriod = Integer.parseInt(argsMap.get("-p"));
        }

        if(hourList != null && hourList.size() > 0){
            Collections.sort(hourList);
        }

        if(argsMap.containsKey("-o")){
            outputFile = argsMap.get("-o");
        }else{
            System.out.println("parameter -o is null");
            System.out.println(HELPMSG);

            return;
        }

        if(argsMap.containsKey("-t")){
            String tpsPeriodStr = argsMap.get("-t");
            try{
                if(Integer.parseInt(tpsPeriodStr) > 0 && Integer.parseInt(tpsPeriodStr) < 4){
                    tpsPeriod = Integer.parseInt(tpsPeriodStr);
                }
            }catch (Exception e){

            }
        }

        if (day != null) {
            analyzeLogDayByHour();
        }else{
            List<File> fileList = new ArrayList<File>();
            for(String dir : logDirList){
                File file = new File(dir);
                File[] files = file.listFiles();

                for(File subFile : files){
                    fileList.add(subFile);
                }
            }

            analyzeLogList(fileList);
        }

        deleteTmpFiles();

        long end = System.currentTimeMillis();
        System.out.println("日志分析完成, 用时：" + (end - start) + "ms");
    }

    private static void writeXLSX(Map<String, String[]> analyzeRetMap, List<String[]> repeatDataList, String outputFileName) {
        System.out.println("开始写入文件：" + outputFileName);

        Map<String, Long[]> tpsMap = new HashMap<String, Long[]>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        SXSSFWorkbook workbook = new SXSSFWorkbook(200);
        workbook.setCompressTempFiles(true);

        Sheet sheet = workbook.createSheet("数据记录");

        int totalCount = analyzeRetMap.size() * 2;
        int noRequestNum = 0;   //未请求数量
        int noAnswerNum = 0;    //未回复数量

        //ccrRequestType + "$" + cCRequestNumber + "$" + interfaceIndicator + "$" + sessionId;
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("SessionId");

        cell = row.createCell(1);
        cell.setCellValue("CCRequestType");

        cell = row.createCell(2);
        cell.setCellValue("cCRequestNumber");

        cell = row.createCell(3);
        cell.setCellValue("interfaceIndicator");

        cell = row.createCell(4);
        cell.setCellValue("RequestTimeStamp");

        cell = row.createCell(5);
        cell.setCellValue("AnswerTimeStamp");

        cell = row.createCell(6);
        cell.setCellValue("UsedTime(ms)");

//        cell = row.createCell(7);
//        cell.setCellValue("subscriptionIds");

        int index = 1;
        Iterator<String> iter = analyzeRetMap.keySet().iterator();
        while (iter.hasNext()) {

            String key = iter.next();
            String[] array = analyzeRetMap.get(key);

            double usedTime = 0.0;
            if (array[0] != null && array[1] != null) {
                try {
                    Date start = sdf.parse(array[0]);
                    Date end = sdf.parse(array[1]);

                    usedTime = end.getTime() - start.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if(usedTime < 500.0){
                continue;
            }

            String[] keyArr = key.split("\\$");
            row = sheet.createRow(index);

            ////SessionId
            cell = row.createCell(0);
            cell.setCellValue(keyArr[3]);

            //CCRequestType
            cell = row.createCell(1);
            cell.setCellValue(keyArr[0]);

            //cCRequestNumber
            cell = row.createCell(2);
            cell.setCellValue(keyArr[1]);

            //interfaceIndicator
            cell = row.createCell(3);
            cell.setCellValue(keyArr[2]);

            //RequestTimeStamp
            cell = row.createCell(4);
            cell.setCellValue(array[0]);

            //AnswerTimeStamp
            cell = row.createCell(5);
            cell.setCellValue(array[1]);

            if(array[0] == null){
                noRequestNum++;
            }

            if(array[1] == null){
                noAnswerNum++;
            }

            //subscriptionIds
//            cell = row.createCell(7);
//            cell.setCellValue(array[2]);

            //TPS统计
            if (array[0] != null) {
                String requestTime = null;
                if(tpsPeriod == 2){ //100ms
                    requestTime = array[0].substring(0, array[0].indexOf(".") + 2) + "00";
                }else if(tpsPeriod == 3){//10ms
                    requestTime = array[0].substring(0, array[0].indexOf(".") + 3) + "0";
                }else{
                    requestTime = array[0].substring(0, array[0].indexOf("."));
                }

                if (tpsMap.containsKey(requestTime)) {
                    Long[] rtpArr = tpsMap.get(requestTime);
                    rtpArr[0]++;
                    tpsMap.put(requestTime, rtpArr);
                } else {
                    Long[] rtpArr = new Long[]{1l, 0l};
                    tpsMap.put(requestTime, rtpArr);
                }
            }

            //UsedTime
            cell = row.createCell(6);
            cell.setCellValue(usedTime);


            //TPS统计
            if (array[1] != null) {
                String answerTime = null;
                if(tpsPeriod == 2){ //100ms
                    answerTime = array[1].substring(0, array[1].indexOf(".") + 2) + "00";
                }else if(tpsPeriod == 3){//10ms
                    answerTime = array[1].substring(0, array[1].indexOf(".") + 3) + "0";
                }else{
                    answerTime = array[1].substring(0, array[1].indexOf("."));
                }

                if (tpsMap.containsKey(answerTime)) {
                    Long[] rtpArr = tpsMap.get(answerTime);
                    rtpArr[1]++;
                    tpsMap.put(answerTime, rtpArr);
                } else {
                    Long[] rtpArr = new Long[]{0l, 1l};
                    tpsMap.put(answerTime, rtpArr);
                }
            }

            if (array[0] == null) {
                noRequestNum++;
            }

            if (array[1] == null) {
                noAnswerNum++;
            }

            index++;
        }

        //重复消息统计
        Sheet repeatSheet = workbook.createSheet("重复消息记录");
        row = repeatSheet.createRow(0);
        cell = row.createCell(0);
        cell.setCellValue("SessionId");

        cell = row.createCell(1);
        cell.setCellValue("interfaceIndicator");

        cell = row.createCell(2);
        cell.setCellValue("MsgType");

        cell = row.createCell(3);
        cell.setCellValue("CCRequestType");

        cell = row.createCell(4);
        cell.setCellValue("cCRequestNumber");

        cell = row.createCell(5);
        cell.setCellValue("RequestTimeStamp");

        cell = row.createCell(6);
        cell.setCellValue("AnwserTimeStamp");

        if(repeatDataList != null && repeatDataList.size() > 0){
            Collections.sort(repeatDataList, new Comparator<String[]>() {
                public int compare(String[] array1, String[] array2) {
                    return array1[5].compareTo(array2[5]);
                }
            });

            index = 1;
            Iterator<String[]> repeatIter = repeatDataList.iterator();
            while (repeatIter.hasNext()) {
                String[] dataArr = repeatIter.next();

                row = repeatSheet.createRow(index);
                for (int i = 0; i < dataArr.length - 1; i++) {
                    cell = row.createCell(i);
                    cell.setCellValue(dataArr[i]);
                }

                if ("CCR".equals(dataArr[2])) {
                    cell = row.createCell(5);
                    cell.setCellValue(dataArr[5]);
                } else {
                    cell = row.createCell(6);
                    cell.setCellValue(dataArr[5]);
                }

                if(dataArr[5] != null){
                    String timeStamp = null;
                    if(tpsPeriod == 2){ //100ms
                        timeStamp = dataArr[5].substring(0, dataArr[5].indexOf(".") + 2) + "00";
                    }else if(tpsPeriod == 3){//10ms
                        timeStamp = dataArr[5].substring(0, dataArr[5].indexOf(".") + 3) + "0";
                    }else{
                        timeStamp = dataArr[5].substring(0, dataArr[5].indexOf("."));
                    }

                    if (tpsMap.containsKey(timeStamp)) {
                        Long[] rtpArr = tpsMap.get(timeStamp);
                        if ("CCR".equals(dataArr[2])) {
                            rtpArr[0]++;
                            tpsMap.put(timeStamp, rtpArr);
                        } else {
                            rtpArr[1]++;
                            tpsMap.put(timeStamp, rtpArr);
                        }

                    } else {
                        if ("CCR".equals(dataArr[2])) {
                            tpsMap.put(timeStamp, new Long[]{1l, 0l});
                        } else {
                            tpsMap.put(timeStamp, new Long[]{0l, 1l});
                        }

                    }

                }

                index++;
            }
        }

        //数量统计
        Sheet statSheet = workbook.createSheet("消息数量统计");
        row = statSheet.createRow(0);
        cell = row.createCell(0);
        cell.setCellValue("总记录数");
        cell = row.createCell(1);
        cell.setCellValue(totalCount);

        row = statSheet.createRow(1);
        cell = row.createCell(0);
        cell.setCellValue("未抓到请求数量");
        cell = row.createCell(1);
        cell.setCellValue(noRequestNum);

        row = statSheet.createRow(2);
        cell = row.createCell(0);
        cell.setCellValue("未抓到回复数量");
        cell = row.createCell(1);
        cell.setCellValue(noAnswerNum);

        //tps
        Sheet tpsSheet = workbook.createSheet("TPS统计结果");
        row = tpsSheet.createRow(0);
        cell = row.createCell(0);
        cell.setCellValue("时间");

        cell = row.createCell(1);
        cell.setCellValue("Request TPS");

        cell = row.createCell(2);
        cell.setCellValue("Answer TPS");

        cell = row.createCell(3);
        cell.setCellValue("Answer - Request");

        cell = row.createCell(4);
        cell.setCellValue("Request - Answer");

        Object[] tpsKeyArr = tpsMap.keySet().toArray();
        Arrays.sort(tpsKeyArr);
        for (int i = 0; i < tpsKeyArr.length; i++) {
            Object time = tpsKeyArr[i];
            Long[] array = tpsMap.get(time);
            row = tpsSheet.createRow(i + 1);
            cell = row.createCell(0);
            cell.setCellValue(time.toString());

            cell = row.createCell(1);
            cell.setCellValue(array[0]);

            cell = row.createCell(2);
            cell.setCellValue(array[1]);


            cell = row.createCell(3);
            cell.setCellValue(array[1] - array[0]);


            cell = row.createCell(4);
            cell.setCellValue(array[0] - array[1]);
        }

        try {
            FileOutputStream out = new FileOutputStream(outputFileName);
            workbook.write(out);

            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 按照天统计每小时
     * */
    private static void analyzeLogDayByHour(){
        DecimalFormat df = new DecimalFormat("00");

        for(int i : hourList){
            List<File> fileList = new ArrayList<File>();

            String timeKey = day + df.format(i);

            for(String dir : logDirList){
                File dirFile = new File(dir);
                if(!dirFile.exists()){
                    continue;
                }
                File[] files = dirFile.listFiles();
                for(File subFile : files){
                    if(subFile.getName().indexOf(timeKey) != -1){
                        fileList.add(subFile);
                    }
                }
            }

            if(fileList.size() > 0){
                System.out.println("开始分析 " + timeKey + " 日志， 日志数量： " + fileList.size());
                analyzeLogList(fileList);
            }

        }
    }

    private static void analyzeLogList(List<File> fileList) {
        ZipUtil zipUtil = new ZipUtil();

        Collections.sort(fileList, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (File logFile : fileList) {
            String logFilePath = logFile.getPath();
            if (logFilePath.endsWith(".zip")) {
                try {
                    logFilePath = zipUtil.unZipFile(logFile, logFile.getParent());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                analyzeLog(logFilePath, true);
            } else if (logFilePath.endsWith(".log")) {
                analyzeLog(logFilePath, false);
            } else {
                System.out.println("非日志文件:" + logFilePath);
            }
        }

        Iterator<String> iter = retMap.keySet().iterator();
        while(iter.hasNext()){
            String timeStamp = iter.next();
            Map<String, String[]> eleMap = retMap.get(timeStamp);
            List<String[]> repeatList = repeatDataMap.get(timeStamp);

            String[] tsArray = timeStamp.split("\\$");

            System.out.println(tsArray[0] + " data count: " + eleMap.size());
            writeXLSX(eleMap, repeatList, outputFile + File.separator + "Engine-log_" + tsArray[0].replace(" ", "_").replace(":", "_") + ".xlsx");
        }


        retMap.clear();
        repeatDataMap.clear();
    }

    private static void analyzeLog(String fileName, boolean isDeleteOnFinish) {
        File file = new File(fileName);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    if(line.indexOf("interfaceIndicator") == -1){
                        continue;
                    }

                    if(line.indexOf("\"commandTypeIndicator\":\"RAR\"") != -1){
                        continue;
                    }

                    if (line.indexOf(RECEIVERMSGFLAG) != -1) {   //received

                        String timeStamp = line.substring(0, 23);
                        Date date = sdf.parse(timeStamp);
                        if (startTime != null && date.before(startTime)) {
                            continue;
                        }

                        if (endTime != null && date.after(endTime)) {
                            continue;
                        }

                        String tstamp = getTimeStamp(timeStamp);

                        String sessionId;
                        int sidStart = line.indexOf("\"sessionId\":\"") + 13;
                        int sidEnd = line.indexOf("\",", sidStart);
                        sessionId = line.substring(sidStart, sidEnd);

                        if(line.indexOf("\"interfaceIndicator\":\"") == -1){
                            System.out.println("[ERROR DATA] " + line);
                            continue;
                        }

                        String interfaceIndicator;
                        int ifiStart = line.indexOf("\"interfaceIndicator\":\"") + 22;
                        int ifiEnd = line.indexOf("\",", ifiStart);
                        interfaceIndicator = line.substring(ifiStart, ifiEnd);

                        int ccrTypeStart = line.indexOf("\"cCRequestType\":\"") + 17;
                        int ccrTypeEnd = line.indexOf("\",", ccrTypeStart);
                        String ccrRequestType = line.substring(ccrTypeStart, ccrTypeEnd);

                        //"cCRequestNumber":3,
                        int cCRequestNumberStart = line.indexOf("\"cCRequestNumber\"") + 18;
                        int cCRequestNumberEnd = line.indexOf(",", cCRequestNumberStart);
                        if(cCRequestNumberEnd == -1){
                            cCRequestNumberEnd = line.indexOf("}", cCRequestNumberStart);
                        }

                        String cCRequestNumber = line.substring(cCRequestNumberStart, cCRequestNumberEnd);
                        if(cCRequestNumber.indexOf("}") != -1){
                            cCRequestNumber = cCRequestNumber.substring(0, cCRequestNumber.indexOf("}"));
                        }

                        //
//                        String subscriptionIds;
//                        int subscriStart = line.indexOf("\"subscriptionIds\":") + 20;
//                        int subscriEnd = line.indexOf("},", subscriStart) - 2;
//                        subscriptionIds = line.substring(subscriStart, subscriEnd);

                        String key = ccrRequestType + "$" + cCRequestNumber + "$" + interfaceIndicator + "$" + sessionId;

                        String timeStampKey = tstamp;
                        if(sessionTimeStampMap.containsKey(key)){
                            timeStampKey = sessionTimeStampMap.get(key);
                        }else{
                            sessionTimeStampMap.put(key, tstamp);
                        }

                        if(retMap.containsKey(timeStampKey)){
                            Map<String, String[]> eleMap = retMap.get(timeStampKey);
                            if (eleMap.containsKey(key)) {
                                String[] array = eleMap.get(key);
                                if (array[0] != null) {   //重复数据
                                    //System.out.println("request: " + timeStamp + "=" + key);
                                    int criStart = line.indexOf("commandTypeIndicator") + 23;
                                    int criEnd = line.indexOf("\",", criStart);
                                    String cri = line.substring(criStart, criEnd);

                                    if(repeatDataMap.containsKey(timeStampKey)){
                                        List<String[]> repeatDataList = repeatDataMap.get(timeStampKey);
                                        repeatDataList.add(new String[]{sessionId, interfaceIndicator, cri, ccrRequestType, cCRequestNumber, timeStamp});

                                    }else{
                                        List<String[]> repeatDataList = new ArrayList<String[]>();
                                        repeatDataList.add(new String[]{sessionId, interfaceIndicator, cri, ccrRequestType, cCRequestNumber, timeStamp});
                                        repeatDataMap.put(timeStampKey, repeatDataList);
                                    }
                                } else {
                                    array[0] = timeStamp;

                                    //array[2] = subscriptionIds;

                                    eleMap.put(key, array);
                                }
                            } else {
                                eleMap.put(key, new String[]{timeStamp, null});
                            }
                        }else{
                            Map<String, String[]> eleMap = new HashMap<String, String[]>();
                            eleMap.put(key, new String[]{timeStamp, null});
                            retMap.put(timeStampKey, eleMap);
                        }


                        //System.out.println(key +": request: " + retMap.get(key)[0] +", answer:" + retMap.get(key)[1]);
                    } else if (line.indexOf(SENDMSGFLAG) != -1) {   //send
                        String timeStamp = line.substring(0, 23);
                        Date date = sdf.parse(timeStamp);
                        if (startTime != null && date.before(startTime)) {
                            continue;
                        }

                        if (endTime != null && date.after(endTime)) {
                            continue;
                        }

                        String tstamp = getTimeStamp(timeStamp);

                        int sidStart = line.indexOf("\"sessionId\":\"") + 13;
                        int sidEnd = line.indexOf("\",", sidStart);
                        String sessionId = line.substring(sidStart, sidEnd);

                        int ccrTypeStart = line.indexOf("\"cCRequestType\":\"") + 17;
                        int ccrTypeEnd = line.indexOf("\",", ccrTypeStart);
                        String ccrRequestType = line.substring(ccrTypeStart, ccrTypeEnd);

                        if(line.indexOf("\"interfaceIndicator\":\"") == -1){
                            System.out.println("[ERROR DATA] " + line);
                            continue;
                        }

                        String interfaceIndicator;
                        int ifiStart = line.indexOf("\"interfaceIndicator\":\"") + 22;
                        int ifiEnd = line.indexOf("\",", ifiStart);
                        interfaceIndicator = line.substring(ifiStart, ifiEnd);


                        //"cCRequestNumber":3,
                        int cCRequestNumberStart = line.indexOf("\"cCRequestNumber\":") + 18;
                        int cCRequestNumberEnd = line.indexOf(",", cCRequestNumberStart);
                        if(cCRequestNumberEnd == -1){
                            cCRequestNumberEnd = line.indexOf("}", cCRequestNumberStart);
                        }
                        String cCRequestNumber = line.substring(cCRequestNumberStart, cCRequestNumberEnd);
                        if(cCRequestNumber.indexOf("}") != -1){
                            cCRequestNumber = cCRequestNumber.substring(0, cCRequestNumber.indexOf("}"));
                        }


                        String key = ccrRequestType + "$" + cCRequestNumber + "$" + interfaceIndicator + "$" + sessionId;

                        String timeStampKey = tstamp;
                        if(sessionTimeStampMap.containsKey(key)){
                            timeStampKey = sessionTimeStampMap.get(key);
                        }else{
                            sessionTimeStampMap.put(key, tstamp);
                        }


                        if(retMap.containsKey(timeStampKey)){
                            Map<String, String[]> eleMap = retMap.get(timeStampKey);
                            if (eleMap.containsKey(key)) {
                                String[] array = eleMap.get(key);
                                if (array[1] != null) {   //重复数据
                                    //System.out.println("request: " + timeStamp + "=" + key);
                                    int criStart = line.indexOf("commandTypeIndicator") + 23;
                                    int criEnd = line.indexOf("\",", criStart);
                                    String cri = line.substring(criStart, criEnd);

                                    if(repeatDataMap.containsKey(timeStampKey)){
                                        List<String[]> repeatDataList = repeatDataMap.get(timeStampKey);
                                        repeatDataList.add(new String[]{sessionId, interfaceIndicator, cri, ccrRequestType, cCRequestNumber, timeStamp});

                                    }else{
                                        List<String[]> repeatDataList = new ArrayList<String[]>();
                                        repeatDataList.add(new String[]{sessionId, interfaceIndicator, cri, ccrRequestType, cCRequestNumber, timeStamp});
                                        repeatDataMap.put(timeStampKey, repeatDataList);
                                    }


                                } else {
                                    array[1] = timeStamp;

                                    eleMap.put(key, array);
                                }
                            } else {
                                eleMap.put(key, new String[]{null, timeStamp});
                            }
                        }else{
                            Map<String, String[]> eleMap = new HashMap<String, String[]>();
                            eleMap.put(key, new String[]{timeStamp, null});
                            retMap.put(timeStampKey, eleMap);
                        }


                        //System.out.println(key + ":request: " + retMap.get(key)[0] +", answer:" + retMap.get(key)[1]);
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + line);
                    e.printStackTrace();
                }

            }

            fr.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isDeleteOnFinish) {
            file.delete();
        }
    }

    /**
     * 获取时间戳
     * */
    private static String getTimeStamp(String time){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(time));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int minute = cal.get(Calendar.MINUTE);

        if(timePeriod == 0){    //小时
            cal.set(Calendar.MINUTE, 0);
        }else if(timePeriod == 1){  //30分钟
           if(minute < 30){
                cal.set(Calendar.MINUTE, 0);
            }else {
                cal.set(Calendar.MINUTE, 30);
            }
        }else if(timePeriod == 2){  //15分钟
            if(minute < 15){
                cal.set(Calendar.MINUTE, 0);
            }else if(minute < 30){
                cal.set(Calendar.MINUTE, 15);
            }else if(minute < 45){
                cal.set(Calendar.MINUTE, 30);
            }else {
                cal.set(Calendar.MINUTE, 45);
            }
        }else if(timePeriod == 3){  //10分钟
            int i = minute / 10;
            cal.set(Calendar.MINUTE, i * 10);
        }else if(timePeriod == 4){  //5分钟
            int i = minute / 5;
            cal.set(Calendar.MINUTE, i * 5);
        }

        cal.set(Calendar.SECOND, 0);

        String start = sdf.format(cal.getTime());

        if(timePeriod == 0){    //小时
            cal.add(Calendar.MINUTE, 60);
            cal.set(Calendar.SECOND, 0);
        }else if(timePeriod == 1){  //30分钟
            cal.add(Calendar.MINUTE, 30);
            cal.set(Calendar.SECOND, 0);
        }else if(timePeriod == 2){  //15分钟
            cal.add(Calendar.MINUTE, 15);
            cal.set(Calendar.SECOND, 0);
        }

        String end = sdf.format(cal.getTime());


        return start + "$" + end;
    }

    /**
     * 判断文件是否在指定时间范围
     * */
    private boolean validasteFileTimeStamp(String fileName){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
        try {
            Date fileDate = sdf.parse(fileName.substring(fileName.indexOf("_") + 1, fileName.lastIndexOf("_")));

            if(fileDate.before(startTime) || fileDate.after(endTime)){
                return false;
            }

            return true;
        } catch (ParseException e) {
            System.out.println("[ERROR] 文件名错误：" + fileName);

            return false;
        }
    }

    private static void deleteTmpFiles(){
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
        if("Linux".equalsIgnoreCase(osName)){
            File file = new File("/tmp");
            File[] files = file.listFiles();

            List<File> delList = new ArrayList<File>();
            for(File subFile : files){
                if(subFile.isFile() && subFile.getName().startsWith("poi-")){
                    delList.add(subFile);
                }
            }

            if(delList.size() > 0){
                Iterator<File> iter = delList.iterator();
                while (iter.hasNext()){
                    File subFile = iter.next();
                    subFile.delete();
                }
            }
        }

    }


    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public int getTpsPeriod() {
        return tpsPeriod;
    }

    public void setTpsPeriod(int tpsPeriod) {
        this.tpsPeriod = tpsPeriod;
    }
}
