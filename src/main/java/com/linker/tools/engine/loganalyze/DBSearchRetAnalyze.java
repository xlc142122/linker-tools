package com.linker.tools.engine.loganalyze;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created by Pasenger on 2014/9/26.
 */
public class DBSearchRetAnalyze {
    public static void main(String[] args) throws Exception {

        FileReader fr = new FileReader("E:\\temp\\jx\\searchRet.log");
        BufferedReader br = new BufferedReader(fr);
        String line;
        while((line = br.readLine()) != null){
            String timeStamp = line.substring(0, 23);

            int sidStart = line.indexOf("sessionId： ") + 11;
            int sidEnd = line.indexOf(",", sidStart);
            String sessionId = line.substring(sidStart, sidEnd);

            //find SubUser from db start    - 575 -
            //find SubUser from db end      - 577 -

            //find SubUser from db start    - 594 -
            //BusinessInfoService - 598 -


        }
    }


}
