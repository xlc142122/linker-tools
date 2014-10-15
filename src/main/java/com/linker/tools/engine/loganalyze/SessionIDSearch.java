package com.linker.tools.engine.loganalyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Pasenger on 2014/9/26.
 */
public class SessionIDSearch {

    public static void main(String[] args) {
        Map<String, String> allMap = readFile("C:\\Users\\Pasenger\\Desktop\\all.txt");
        Map<String, String> timeoutMap = readFile("C:\\Users\\Pasenger\\Desktop\\timeout.txt");


        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = timeoutMap.keySet().iterator();
        while (iter.hasNext()){
            String key = iter.next();

            if(allMap.containsKey(key)){
                System.out.println(key + ", pcap: " + timeoutMap.get(key) + ", engine: " + allMap.get(key));
            }else{
                sb.append(key).append("\n");
            }
        }

        System.out.println("-----------------");
        System.out.println(sb.toString());

    }

    private static Map<String, String> readFile(String path){
        Map<String, String> map = new HashMap<String, String>();

        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(new File(path ));
            br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] array = line.split(" ");
                String key = array[0] + "$" + array[1];
                if(map.containsKey(key)){
                    System.out.println("repeat: " + line);
                }else{
                    map.put(key, array[2]);
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


        return map;
    }
}
