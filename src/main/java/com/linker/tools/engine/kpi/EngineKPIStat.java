package com.linker.tools.engine.kpi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.linker.tools.util.ParamAnalyze;
import com.linker.tools.util.ZipUtil;

/**
 * Engine KPI统计
 * 功能：
 *      1. 统计粒度：15分钟
 *      2. 统计消息：CCRI， CCRU， CCRT， RAR
 *      3. 统计数量单位：条
 *      4. 可分单个Engine和多个Engien统计
 *      5. 统计结果格式：CSV文件
 * Created by Pasenger on 2014/9/26.
 */
public class EngineKPIStat {

    private static final String HELPMSG =
            "\nExample:\n" +
                    "\tjava -cp linkerTool.jar com.pasenger.linker.kpi.EngineKPIStat -i \"/var/log/engine-0,/var/log/engine-1\" -o \"log-1\" -t 1\n -s \"2014-09-26 00\" -e \"2014-09-26 10\"" +
                    "\nParameters description:\n" +
                    "\t-i logDir log文件所在路径，支持.log, .zip结尾文件\n" +
                    "\t-o outputFile  输出文件名称,包含路径，如：/etc/log/kpi-20140926.xlsx\n" +
                    "\t-start startTime 日志分析开始时间，格式yyyyMMddHH\n" +
                    "\t-end endTime  日志分析截止时间，格式yyyyMMddHH\n" +
                    "\t-h 显示帮助\n\n";

    //接收到的消息标识
    private static final String RECEIVEDFLAG = "DomainRouteActor - 85 - Received message";
    //发送消息标识
    private static final String SENDFLAG = "DomainSendActor - 100 - Send back message";

    //CCRI消息标识
    private static final String CCRIFLAG = "\"cCRequestType\":\"INITIAL_REQUEST\"";
    //CCRU消息标识
    private static final String CCRUFLAG = "\"cCRequestType\":\"UPDATE_REQUEST\"";
    //CCRT消息标识
    private static final String CCRTFLAG = "\"cCRequestType\":\"TERMINATION_REQUEST\"";
    //RAR消息标识
    private static final String RARFLAG = "";

    //统计开始时间
    private static Date startTime;
    //统计结束时间
    private static Date endTime;

    //log文件路径
    private static String[] logPathList;

    //输出文件
    private static String outputFile;


    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");

    public static void main(String[] args) {
        if(args == null || args.length == 0){
            System.out.println("input parameter is null");
            System.out.println(HELPMSG);
            return;
        }

        Map<String,String> argsMap = ParamAnalyze.analyzeParam(args);
        if(argsMap == null || argsMap.size() == 0){
            System.out.println("input parameter error!");
            System.out.println(HELPMSG);
            return;
        }

        String logPaths = argsMap.get("-i");
        if(logPaths == null || "".equals(logPaths)){
            System.out.println("input parameter -i error!");
            System.out.println(HELPMSG);
            return;
        }

        logPathList = logPaths.split(",");

        if(!argsMap.containsKey("-o")){
            System.out.println("input parameter -o error!");
            System.out.println(HELPMSG);
            return;
        }

        outputFile = argsMap.get("-o");

        if(argsMap.containsKey("-start")){
            try {
                startTime = sdf.parse(argsMap.get("-start"));
            } catch (ParseException e) {
                System.out.println("[ERROR]param -start error");
                System.out.println(HELPMSG);
                return;
            }
        }else{
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            startTime = calendar.getTime();
        }

        if(argsMap.containsKey("-end")){
            try {
                endTime = sdf.parse(argsMap.get("-end"));
            } catch (ParseException e) {
                System.out.println("[ERROR]param -end error");
                System.out.println(HELPMSG);

                return;
            }
        }else{
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            endTime = calendar.getTime();
        }

        statKpi();
    }

    /**
     * 分析日志
     * */
    private static void statKpi(){
        /**
         * key: startTimeStamp + "$" + endTimeStamp
         * value: {requestCCRI, requestCCRU, requestCCRT, answerCCRI, answerCCRU, answerCCRT}
         * */
        Map<String, Integer[]> retMap = new HashMap<String, Integer[]>();
        for(String logPath : logPathList){
            File subLogPath = new File(logPath);
            if(!subLogPath.exists()){
                System.out.println("[ERROR] " + logPath + " 不存在！");
                continue;
            }

            File[] files = subLogPath.listFiles();

            if(files == null || files.length == 0){
                System.out.println("[WARN] " + subLogPath + " 为空！");
                continue;
            }

            List<File> fileList = new ArrayList<File>();
            for(File subFile:files){
                if(validasteFileTimeStamp(subFile.getName())){
                    fileList.add(subFile);
                }
            }

            System.out.println(logPath + " 文件数量：" + fileList.size());
            Collections.sort(fileList, new Comparator<File>() {
                public int compare(File o1, File o2) {
                    if (o1.isDirectory() && o2.isFile())
                        return -1;
                    if (o1.isFile() && o2.isDirectory())
                        return 1;
                    return o1.getName().compareTo(o2.getName());
                }
            });

            ZipUtil zipUtil = new ZipUtil();
            for(File logFile : fileList){
                if(logFile.getName().endsWith(".zip")){
                    try {
                        String logFilePath = zipUtil.unZipFile(logFile, logFile.getParent());
                        analyzeLog(logFilePath, retMap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }
        }

        System.out.println("统计完成，开始输出文件， 记录行数：" + retMap.size());
        outputToXLSX(retMap);
        System.out.println("输出文件完成!");
    }

    /**
     * 分析单个日志文件
     * */
    private static void analyzeLog(String logFilePath, Map<String, Integer[]> retMap){
        File logFile = new File(logFilePath);
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(logFile);
            br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null) {
                /**
                 * key: startTimeStamp + "$" + endTimeStamp
                 * value: {requestCCRI, requestCCRU, requestCCRT, answerCCRI, answerCCRU, answerCCRT}
                 * */

                if(line.indexOf("interfaceIndicator") == -1){
                    continue;
                }

                if(line.indexOf("\"commandTypeIndicator\":\"RAR\"") != -1){
                    continue;
                }

                if(line.indexOf(RECEIVEDFLAG) >= 0){    //request
                    String timeStamp = line.substring(0, 20);
                    String timeRange = getTimeStamp(timeStamp); //start$end
                    if(line.indexOf(CCRIFLAG) > 0){
                        if(retMap.containsKey(timeRange)){
                            Integer[] array = retMap.get(timeRange);
                            array[0] += 1;
                        }else{
                            retMap.put(timeRange, new Integer[]{1, 0, 0, 0, 0, 0});
                        }
                    }else if(line.indexOf(CCRUFLAG) > 0){
                        if(retMap.containsKey(timeRange)){
                            Integer[] array = retMap.get(timeRange);
                            array[1] += 1;
                        }else{
                            retMap.put(timeRange, new Integer[]{0, 1, 0, 0, 0, 0});
                        }
                    }else if(line.indexOf(CCRTFLAG) > 0){
                        if(retMap.containsKey(timeRange)){
                            Integer[] array = retMap.get(timeRange);
                            array[2] += 1;
                        }else{
                            retMap.put(timeRange, new Integer[]{0, 0, 1, 0, 0, 0});
                        }
                    }
                }else if(line.indexOf(SENDFLAG) >= 0) {   //answer
                    String timeStamp = line.substring(0, 20);
                    String timeRange = getTimeStamp(timeStamp); //start$end
                    if(line.indexOf(CCRIFLAG) > 0){
                        if(retMap.containsKey(timeRange)){
                            Integer[] array = retMap.get(timeRange);
                            array[3] += 1;
                        }else{
                            retMap.put(timeRange, new Integer[]{0, 0, 0, 1, 0, 0});
                        }
                    }else if(line.indexOf(CCRUFLAG) > 0){
                        if(retMap.containsKey(timeRange)){
                            Integer[] array = retMap.get(timeRange);
                            array[4] += 1;
                        }else{
                            retMap.put(timeRange, new Integer[]{0, 0, 0, 0, 1, 0});
                        }
                    }else if(line.indexOf(CCRTFLAG) > 0){
                        if(retMap.containsKey(timeRange)){
                            Integer[] array = retMap.get(timeRange);
                            array[5] += 1;
                        }else{
                            retMap.put(timeRange, new Integer[]{0, 0, 0, 0, 0, 1});
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void outputToXLSX(Map<String, Integer[]> retMap){
        if(retMap == null || retMap.size() == 0){
            return;
        }

        SXSSFWorkbook workbook = new SXSSFWorkbook(200);
        workbook.setCompressTempFiles(true);

        Sheet sheet = workbook.createSheet("KPI统计");

        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("统计周期");

        cell = row.createCell(1);
        cell.setCellValue("15分钟");

        row = sheet.createRow(1);
        cell = row.createCell(0);
        cell.setCellValue("开始时间");

        cell = row.createCell(1);
        cell.setCellValue(sdf.format(startTime));

        row = sheet.createRow(2);
        cell = row.createCell(0);
        cell.setCellValue("结束时间");

        cell = row.createCell(1);
        cell.setCellValue(sdf.format(endTime));

        row = sheet.createRow(3);

        row = sheet.createRow(4);
        cell = row.createCell(0);
        cell.setCellValue("开始时间");

        cell = row.createCell(1);
        cell.setCellValue("结束时间");

        //requestCCRI, requestCCRU, requestCCRT, answerCCRI, answerCCRU, answerCCRT

        cell = row.createCell(2);
        cell.setCellValue("CCRI请求数量");

        cell = row.createCell(3);
        cell.setCellValue("CCRI回复数量");

        cell = row.createCell(4);
        cell.setCellValue("CCRI回复 - CCRI请求");

        cell = row.createCell(5);
        cell.setCellValue("CCRU请求数量");

        cell = row.createCell(6);
        cell.setCellValue("CCRU回复数量");

        cell = row.createCell(7);
        cell.setCellValue("CCRU回复 - CCRU请求");

        cell = row.createCell(8);
        cell.setCellValue("CCRT请求数量");

        cell = row.createCell(9);
        cell.setCellValue("CCRT回复数量");

        cell = row.createCell(10);
        cell.setCellValue("CCRT回复 - CCRT请求");


        TreeMap<String, Integer[]> treeMap = new TreeMap<String, Integer[]>(retMap);

        int rowIndex = 5;
        Iterator<String> iter = treeMap.keySet().iterator();
        while(iter.hasNext()){
            String key = iter.next();
            Integer[] value = treeMap.get(key);

            String[] keyArr = key.split("\\$");

            row = sheet.createRow(rowIndex);
            cell = row.createCell(0);
            cell.setCellValue(keyArr[0]);

            cell = row.createCell(1);
            cell.setCellValue(keyArr[1]);

            //requestCCRI, requestCCRU, requestCCRT, answerCCRI, answerCCRU, answerCCRT

            cell = row.createCell(2);
            cell.setCellValue(value[0]);

            cell = row.createCell(3);
            cell.setCellValue(value[3]);

            cell = row.createCell(4);
            cell.setCellValue(value[3] - value[0]);

            cell = row.createCell(5);
            cell.setCellValue(value[1]);

            cell = row.createCell(6);
            cell.setCellValue(value[4]);

            cell = row.createCell(7);
            cell.setCellValue(value[4] - value[1]);

            cell = row.createCell(8);
            cell.setCellValue(value[2]);

            cell = row.createCell(9);
            cell.setCellValue(value[5]);

            cell = row.createCell(10);
            cell.setCellValue(value[5] - value[2]);

            rowIndex++;
        }

        try {
            FileOutputStream out = new FileOutputStream(outputFile);
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
     * 判断文件是否在指定时间范围
     * */
    private static boolean validasteFileTimeStamp(String fileName){
        try {
            Date fileDate = sdf.parse(fileName.substring(fileName.indexOf("_") + 1, fileName.lastIndexOf("_")));

            if(fileDate.before(startTime) || fileDate.after(endTime)){
                return false;
            }

            return true;
        } catch (Exception e) {
            System.out.println("[ERROR] 文件名错误：" + fileName);

            return false;
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
        if(minute < 15){
            cal.set(Calendar.MINUTE, 0);
        }else if(minute < 30){
            cal.set(Calendar.MINUTE, 15);
        }else if(minute < 45){
            cal.set(Calendar.MINUTE, 30);
        }else {
            cal.set(Calendar.MINUTE, 45);
        }

        cal.set(Calendar.SECOND, 0);


        String start = sdf.format(cal.getTime());

        cal.add(Calendar.MINUTE, 15);
        cal.set(Calendar.SECOND, 0);

        String end = sdf.format(cal.getTime());


        return start + "$" + end;
    }
}
