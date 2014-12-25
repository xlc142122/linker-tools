package com.linker.tools.performance.engine;

import org.zeromq.ZMQ;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pasenger on 2014/12/25.
 */
public class SendMsgTest {

    private static List<String> msgList = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        readMsg("E:\\temp\\receiver.log");

        String endPoint = "tcp://172.16.3.190:6377";
        ZMQ.Context context = ZMQ.context(1);

        ZMQ.Socket socket = context.socket(ZMQ.PUSH);
        socket.connect(endPoint);

        System.out.println("msg count: " + msgList.size());

        for(String msg : msgList){
            socket.send(msg);
            Thread.sleep(50);
        }

        socket.close();
        context.term();
    }

    private static void readMsg(String msgFile) throws Exception {
        FileReader fr = new FileReader(msgFile);
        BufferedReader br = new BufferedReader(fr);

        String line = null;
        while((line = br.readLine()) != null){
            msgList.add(line);
        }

        br.close();
        fr.close();
    }
}
