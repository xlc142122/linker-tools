package com.linker.tools.engine.pcapanalyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.linker.tools.util.ParamAnalyze;

/**
 * PCAP XML Analyze
 * Created by Pasenger on 2014/8/30.
 */
public class PCAPXMLAnalyze {
    private static final String HELPMSG =
            "\nExample:\n" +
                    "\tjava -jar linkerTool.jar -i E:\\temp\\pcap\\1.xml -o E:\\temp\\pcap\\1.xlsx -t 3\n" +
                    "\nParameters description:\n" +
                    "\t-i pcap文件所在路径，支持文件所在路径, .pcap文件名称\n" +
                    "\t-o outputFileName  输出文件名称，包含路径，后缀名(.xlsx)，如：/etc/pcap/pcap.xlsx\n" +
                    "\t-t TPS统计粒度，可选：1：1s; 2: 100ms; 3:10ms\n" +
                    "\t-h 显示帮助\n\n";

    /**
     * key: 数据来源，如pdsn, pgw
     * value:
     *      key: sessionId + CCRequestType
     *      value: 请求时间，回复时间
     * */
    private static Map<String, Map<String, String[]>> statRetMap = new HashMap<String, Map<String, String[]>>();


    /**
     * key: 数据来源，如pdsn, pgw
     * value:
     *      List{sessionId, ccrType, ccrFlag, timeStamp, timeStampValue}
     * */
    private static Map<String, List<String[]>> statRepeatDataMap = new HashMap<String, List<String[]>>();

    //private static Map<String, String[]> noRepeatDataMap = new HashMap<>();    //非重复数据
    //private static List<String[]> repeatDataList = new ArrayList<>();  //重复数据分析

    //TPS统计粒度
    private static int tpsPeriod = 1;

    //输出文件
    private static String outputFile = null;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> argsMap = ParamAnalyze.analyzeParam(args);

        if(argsMap == null || argsMap.size() == 0){
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
                e.printStackTrace();
                tpsPeriod = 1;
            }
        }

        if(!argsMap.containsKey("-o")){
            System.out.println("need -o params");
            System.out.println(HELPMSG);

            return;
        }

        outputFile = argsMap.get("-o");

        //pcap到处的xml文件
        File file = new File(argsMap.get("-i"));
        if(!file.exists()){
            System.out.println("file not exist: " + argsMap.get("-i"));
            return;
        }

        if(file.isDirectory()){ //目录
            File[] fileList = file.listFiles();
            for(File subFile : fileList){
                if(subFile.getName().endsWith(".xml")){
                    analyzePcapXML(subFile);
                }else{
                    System.out.println("非xml文件: " + subFile.getName());
                }
            }
        }else{
            if(file.getName().endsWith(".xml")){
                analyzePcapXML(file);
            }else{
                System.out.println("非xml文件: " + file.getName());
            }
        }


        if(statRetMap != null && statRetMap.size() > 0){
            outputToXLSX();
        }else{
            System.out.println("分析结果为空...");
        }


        //结果文件
        //writeXLSX(argsMap.get("-o"), tpsPeriod);
        long end = System.currentTimeMillis();
        System.out.println("处理完成，用时：" + (end - start) / 1000 + "s");
    }

    private static void outputToXLSX(){
        if(statRetMap == null || statRetMap.size() == 0){
            System.out.println("统计结果为空！");

            return;
        }

        //TPS统计
        Map<String, Map<String, Long[]>> tpsStatMap = new HashMap<String, Map<String, Long[]>>();

        SXSSFWorkbook workbook = new SXSSFWorkbook(1000);
        workbook.setCompressTempFiles(true);

        Iterator<String> iter = statRetMap.keySet().iterator();
        while(iter.hasNext()){
            String pgw = iter.next();
            Map<String, String[]> dataMap = statRetMap.get(pgw);
            if(dataMap == null || dataMap.size() == 0){
                continue;
            }

            Sheet sheet = workbook.createSheet(pgw + "-数据统计");

            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("SessionId");

            cell = row.createCell(1);
            cell.setCellValue("CCRequestType");

            cell = row.createCell(2);
            cell.setCellValue("CCRequestNumber");

            cell = row.createCell(3);
            cell.setCellValue("interfaceIndicator");

            cell = row.createCell(4);
            cell.setCellValue("RequestTimeStamp");

            cell = row.createCell(5);
            cell.setCellValue("RequestTimeStampValue");

            cell = row.createCell(6);
            cell.setCellValue("AnswerTimeStamp");

            cell = row.createCell(7);
            cell.setCellValue("AnswerTimeStampValue");

            cell = row.createCell(8);
            cell.setCellValue("UsedTime(ms)");

            int index = 1;
            Iterator<String> dataIter = dataMap.keySet().iterator();
            while (dataIter.hasNext()) {

                String key = dataIter.next();
                String[] array = dataMap.get(key);

                String[] keyArr = key.split("\\$");
                row = sheet.createRow(index);
                cell = row.createCell(0);
                cell.setCellValue(keyArr[3]);

                cell = row.createCell(1);
                cell.setCellValue(keyArr[0]);

                cell = row.createCell(2);
                cell.setCellValue(keyArr[1]);

                cell = row.createCell(3);
                cell.setCellValue(keyArr[2]);

                cell = row.createCell(4);
                cell.setCellValue(array[0]);

                cell = row.createCell(5);
                cell.setCellValue(array[1]);

                cell = row.createCell(6);
                cell.setCellValue(array[2]);

                cell = row.createCell(7);
                cell.setCellValue(array[3]);

                double usedTime;

                try {
                    usedTime = (Double.parseDouble(array[3]) - Double.parseDouble(array[1])) * 1000;
                } catch (Exception e) {
                    usedTime = 0.0;
                }

                cell = row.createCell(8);
                cell.setCellValue(usedTime);

                //TPS统计
                if(array[0] != null){
                    String requestTime;
                    if(tpsPeriod == 2){ //100ms
                        requestTime = array[0].substring(array[0].indexOf(",") + 1, array[0].indexOf(".") + 2) + "00";
                    }else if(tpsPeriod == 3){//10ms
                        requestTime = array[0].substring(array[0].indexOf(",") + 1, array[0].indexOf(".") + 3) + "0";
                    }else{
                        requestTime = array[0].substring(array[0].indexOf(",") + 1, array[0].indexOf("."));
                    }

                    if(tpsStatMap.containsKey(pgw)){
                        Map<String, Long[]> tpsMap = tpsStatMap.get(pgw);
                        if(tpsMap.containsKey(requestTime)){
                            Long[] rtpArr = tpsMap.get(requestTime);
                            rtpArr[0]++;
                            tpsMap.put(requestTime, rtpArr);
                        }else{
                            Long[] rtpArr = new Long[]{1l, 0l};
                            tpsMap.put(requestTime, rtpArr);
                        }
                    }else{
                        Map<String, Long[]> tpsMap = new HashMap<String, Long[]>();
                        if(tpsMap.containsKey(requestTime)){
                            Long[] rtpArr = tpsMap.get(requestTime);
                            rtpArr[0]++;
                            tpsMap.put(requestTime, rtpArr);
                        }else{
                            Long[] rtpArr = new Long[]{1l, 0l};
                            tpsMap.put(requestTime, rtpArr);
                        }

                        tpsStatMap.put(pgw, tpsMap);
                    }
                }

                //TPS统计
                if(array[2] != null){
                    String answerTime;
                    if(tpsPeriod == 2){ //100ms
                        answerTime = array[2].substring(array[2].indexOf(",") + 1, array[2].indexOf(".") + 2).trim() + "00";
                    }else if(tpsPeriod == 3){//10ms
                        answerTime = array[2].substring(array[2].indexOf(",") + 1, array[2].indexOf(".") + 3).trim() + "0";
                    }else{
                        answerTime = array[2].substring(array[2].indexOf(",") + 1, array[2].indexOf("."));
                    }

                    if(tpsStatMap.containsKey(pgw)){
                        Map<String, Long[]> tpsMap = tpsStatMap.get(pgw);
                        if(tpsMap.containsKey(answerTime)){
                            Long[] rtpArr = tpsMap.get(answerTime);
                            rtpArr[1]++;
                            tpsMap.put(answerTime, rtpArr);
                        }else{
                            Long[] rtpArr = new Long[]{0l, 1l};
                            tpsMap.put(answerTime, rtpArr);
                        }
                    }else{
                        Map<String, Long[]> tpsMap = new HashMap<String, Long[]>();
                        if(tpsMap.containsKey(answerTime)){
                            Long[] rtpArr = tpsMap.get(answerTime);
                            rtpArr[1]++;
                            tpsMap.put(answerTime, rtpArr);
                        }else{
                            Long[] rtpArr = new Long[]{0l, 1l};
                            tpsMap.put(answerTime, rtpArr);
                        }

                        tpsStatMap.put(pgw, tpsMap);
                    }
                }

                index++;
            }

        }

        //输出TPS统计结果
        Iterator<String> tpsStatIter = tpsStatMap.keySet().iterator();
        while(tpsStatIter.hasNext()){
            String pgw = tpsStatIter.next();
            Map<String, Long[]> dataMap = tpsStatMap.get(pgw);

            Sheet tpsSheet = workbook.createSheet(pgw + "-TPS统计");
            Row row = tpsSheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("时间");

            cell = row.createCell(1);
            cell.setCellValue("Request TPS");

            cell = row.createCell(2);
            cell.setCellValue("Answer TPS");

            cell = row.createCell(3);
            cell.setCellValue("Answer - Request");

            Object[] tpsKeyArr = dataMap.keySet().toArray();
            Arrays.sort(tpsKeyArr);
            for(int i = 0; i < tpsKeyArr.length; i++){
                Object time = tpsKeyArr[i];
                Long[] array = dataMap.get(time);
                row = tpsSheet.createRow(i + 1);
                cell = row.createCell(0);
                cell.setCellValue(time.toString());

                cell = row.createCell(1);
                cell.setCellValue(array[0]);

                cell = row.createCell(2);
                cell.setCellValue(array[1]);

                cell = row.createCell(3);
                cell.setCellValue(array[1] - array[0]);
            }
        }

        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            workbook.write(out);
            out.flush();

            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 分析数据
     * @param xmlFile 需要分析的文件
     * */
    private static void analyzePcapXML(File xmlFile) {
        System.out.println("开始分析：" + xmlFile.getName());
        System.out.println("文件大小：" + xmlFile.length());

        String line = null;
        try {
            FileReader fr = new FileReader(xmlFile);
            BufferedReader br = new BufferedReader(fr);
            line = br.readLine();

            /**
             * key: sessionId
             * value:
             * {
             *     INITIAL_REQUEST (1)|Answer|Aug 30, 2014 06:51:00.043898000 中国标准时间|1409352660.043898000
             *     ccrType + "|" + ccrFlag + "|" + timeStamp + "|" + timeStampValue
             *     ccrType: INITIAL/TERMINATER
             *     ccrFlag: REQUEST/ANSWER
             *     RequestTimeStamp 0
             *     RequestTimeStampValue    1
             *     AnswerTimeStamp  2
             *     AnswerTimeStampValue 3
             *     repeatNum    4   重复请求数量
             * }
             *
             */

            while (true) {
                if(line == null){
                    break;
                }


                String sessionId = null;
                String ccrType = null;
                String ccrFlag = null;  //Request/Answer
                String timeStamp = null;
                String timeStampValue = null;
                String interfaceName = null;
                String requestNumber = null;

                while (true) {
                    if (line == null || "</packet>".equals(line.trim())) {
                        line = br.readLine();

                        break;
                    } else {
                        if(line.indexOf("proto name=\"geninfo\"") != -1){
                            while(true){
                                if(line.indexOf("</proto>") != -1){
                                    //line = br.readLine();
                                    break;
                                }

                                if (line.indexOf("timestamp") != -1) {
                                    int begin = line.indexOf("show=\"") + 6;
                                    int end = line.indexOf("\"", begin);
                                    timeStamp = line.substring(begin, end).trim();

                                    begin = line.indexOf("value=\"") + 7;
                                    end = line.indexOf("\"", begin);
                                    timeStampValue = line.substring(begin, end).trim();
                                }

                                line = br.readLine();
                            }
                        }

                        if(line.indexOf("proto name=\"diameter\"") != -1) {
                            while (true) {
                                if (line.indexOf("</proto>") != -1) {
                                    //line = br.readLine();
                                    break;
                                }

                                if (line.indexOf("field name=\"diameter.Session-Id\"") != -1) {
                                    int begin = line.indexOf("Session-Id: ") + 12;
                                    int end = line.indexOf("\"", begin);
                                    sessionId = line.substring(begin, end).trim();
                                    if (sessionId == null) {
                                        System.out.println(line);
                                    }
                                }

                                if (line.indexOf("diameter.CC-Request-Type") != -1) {

                                    int begin = line.indexOf("CC-Request-Type: ") + 17;
                                    int end = line.indexOf("\"", begin);
                                    ccrType = line.substring(begin, end).trim();
                                }

                                if (line.indexOf("diameter.flags.request") != -1) {
                                    if (line.indexOf("Not set") != -1) {
                                        ccrFlag = "Answer";
                                    } else {
                                        ccrFlag = "Request";
                                    }
                                }

                                if (line.indexOf("diameter.CC-Request-Number") != -1) {
                                    int begin = line.indexOf("value=\"") + 7;
                                    int end = line.indexOf("\"", begin);
                                    requestNumber = line.substring(begin, end).trim();
                                }

                                //showname="ApplicationId: 3GPP Gx (16777238)"
                                if (line.indexOf("name=\"diameter.applicationId\"") > 0) {
                                    int begin = line.indexOf("showname=\"") + 10;
                                    int end = line.indexOf("\"", begin);
                                    String interfaceIndicator = line.substring(begin, end).trim();
                                    if (interfaceIndicator.indexOf("Gxa") != -1) {
                                        interfaceName = "GXA";
                                    } else if (interfaceIndicator.indexOf("Gx") != -1) {
                                        interfaceName = "Gx";
                                    } else {
                                        //System.out.println(sessionId);
                                        //System.out.println(line);
                                        interfaceName = "";
                                    }
                                }

                                line = br.readLine();
                            }


                        }

                        line = br.readLine();
                    }

                    //line = br.readLine();
                }

                if (sessionId != null) {
                    String pgw = sessionId.substring(0, sessionId.indexOf("-"));

                    String key = ccrType + "$" + requestNumber + "$" + interfaceName + "$" + sessionId;

                    String[] array;

                    if (statRetMap.containsKey(pgw)) {
                        Map<String, String[]> noRepeatDataMap = statRetMap.get(pgw);
                        if (noRepeatDataMap.containsKey(key)) {
                            boolean repeat = false;
                            array = noRepeatDataMap.get(key);
                            if ("Request".equals(ccrFlag)) {
                                if (array[0] != null) {   //repeat data
                                    repeat = true;
                                } else {
                                    array[0] = timeStamp;
                                    array[1] = timeStampValue;
                                }
                            } else {
                                if (array[2] != null) {   //repeat data
                                    repeat = true;
                                } else {
                                    array[2] = timeStamp;
                                    array[3] = timeStampValue;
                                }

                            }

                            if (!repeat) {
                                noRepeatDataMap.put(key, array);
                            } else {
                                if (statRepeatDataMap.containsKey(pgw)) {
                                    List<String[]> repeatList = statRepeatDataMap.get(pgw);
                                    repeatList.add(new String[]{sessionId, ccrType, ccrFlag, timeStamp, timeStampValue});
                                } else {
                                    List<String[]> repeatList = new ArrayList<String[]>();
                                    repeatList.add(new String[]{sessionId, ccrType, ccrFlag, timeStamp, timeStampValue});
                                    statRepeatDataMap.put(pgw, repeatList);
                                }
                            }
                        } else {
                            array = new String[4];
                            if ("Request".equals(ccrFlag)) {
                                array[0] = timeStamp;
                                array[1] = timeStampValue;
                            } else {
                                array[2] = timeStamp;
                                array[3] = timeStampValue;
                            }

                            noRepeatDataMap.put(key, array);
                        }
                    } else {
                        Map<String, String[]> noRepeatDataMap = new HashMap<String, String[]>();
                        array = new String[4];
                        if ("Request".equals(ccrFlag)) {
                            array[0] = timeStamp;
                            array[1] = timeStampValue;
                        } else {
                            array[2] = timeStamp;
                            array[3] = timeStampValue;
                        }

                        noRepeatDataMap.put(key, array);

                        statRetMap.put(pgw, noRepeatDataMap);
                    }
                }

                //line = br.readLine();
            }

            br.close();
            fr.close();
        } catch (Exception e) {
            System.out.println(line);
            e.printStackTrace();
        }
    }
}
