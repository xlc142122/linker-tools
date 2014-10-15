package com.linker.tools.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 输入参数解析
 * Created by Pasenger on 2014/9/21.
 */
public class ParamAnalyze {
    public static Map<String, String> analyzeParam(String[] args){
        Map<String, String> argsMap = new HashMap<String, String>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (!args[i].startsWith("-")) {
                    i++;
                    continue;
                }

                if("".endsWith(args[i + 1].trim())){
                    System.out.println("[warn] parameter " + args[i] + " is empty!");
                }else{
                    argsMap.put(args[i].trim(), args[i + 1].trim());
                }

                i++;
            }
        }

        System.out.println("correct parameters: ");
        System.out.print("\t");
        Iterator<String> iter = argsMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String value = argsMap.get(key);
            System.out.print(key + ": " + value + "\t");
        }
        System.out.println("\n");

        return argsMap;
    }
}
