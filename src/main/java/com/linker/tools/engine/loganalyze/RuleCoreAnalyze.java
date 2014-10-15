package com.linker.tools.engine.loganalyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Pasenger on 2014/9/9.
 */
public class RuleCoreAnalyze {


    public static void main(String[] args) {
        File file = new File("E:\\temp\\log.txt");

        analyze(file);
    }

    private static void analyze(File file){

        FileReader fr = null;
        BufferedReader br = null;

        String fireAndExeStartFlag = "fireAndExecute start ";
        String fireAndExeEndFlag = "fireAndExecute end ";
        String fireStart = "RuleCore:Fire ------------- Enter in. ";
        String fireEnd = "Fire--------------------end.";

        Map<String, String[]> map = new HashMap<String, String[]>();
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);

            String line = null;
            while((line = br.readLine()) != null){
                String timeStamp = line.substring(0, 23);
                int flag = 0;
                String uuid = null;

                if(line.indexOf(fireAndExeStartFlag) != -1){
                    flag = 0;
                    uuid = "FE|" + line.substring(line.indexOf(fireAndExeStartFlag) + fireAndExeStartFlag.length());
                }else if(line.indexOf(fireAndExeEndFlag) != -1){
                    flag = 1;
                    uuid = "FE|" + line.substring(line.indexOf(fireAndExeEndFlag) + fireAndExeEndFlag.length());
                }else if(line.indexOf(fireStart) != -1){
                    flag = 0;
                    uuid = "F|" + line.substring(line.indexOf(fireStart) + fireStart.length());
                }else if(line.indexOf(fireEnd) != -1){
                    flag = 1;
                    uuid = "F|" + line.substring(line.indexOf(fireEnd) + fireEnd.length());
                }

                if(uuid == null || uuid.endsWith("\\|")){
                    continue;
                }

                if(map.containsKey(uuid)){
                    String[] array = map.get(uuid);

                    if(flag == 0){
                        array[0] = timeStamp;
                    }else{
                        array[1] = timeStamp;
                    }
                }else{
                    String[] array = new String[2];

                    if(flag == 0){
                        array[0] = timeStamp;
                    }else{
                        array[1] = timeStamp;
                    }

                    map.put(uuid, array);
                }
            }

            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(map.size());
        output(map);

    }

    private static void output(Map<String, String[]> map){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        FileWriter fw = null;
        try {
            fw = new FileWriter("E:\\temp\\log.csv", true);
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = map.keySet().iterator();
            while(iter.hasNext()){
                String uuid = iter.next();
                String[] array = map.get(uuid);

                String[] uuidArr = uuid.split("\\|");

                long usedTime = 0l;
                try{
                    Date start = sdf.parse(array[0]);
                    Date end = sdf.parse(array[1]);
                    usedTime = end.getTime() - start.getTime();
                }catch (Exception e) {
                    e.printStackTrace();
                }


                sb.append(uuidArr[0]).append(",").append(uuidArr[1]).append(",").append(array[0]).append(",").append(array[1]).append(",")
                        .append(usedTime).append("\n");
            }

            fw.write(sb.toString());
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
