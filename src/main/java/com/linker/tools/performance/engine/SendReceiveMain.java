package com.linker.tools.performance.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.linker.tools.common.ParamException;


public class SendReceiveMain {

	private static final String HELPMSG = "\n\nexample: -sh 127.0.0.1 -sp 6371 -sm PUSH -sc 10000\n\n"
			+ "parameter description:\r"
			+ "\t-sh sendZMQHost	发送主机IP\n"
			+ "\t-sp sendZMQPort	发送主机端口\n"
			+ "\t-sm sendZMQModel	发送ZMQ连接模式，可选：PUB、PUSH\n"
			+ "\t-sc sendMsgCount	发送消息总数量\n"
			+ "\t-stn sendThreadNum	发送消息线程数量\n"
			+ "\t-startImsi sendStartIMSI	发送消息开始IMSI号码\n"
			+ "\t-srepeat sendRepeatMessage	发送重复消息， 可选:true:重复, false：不重复\n"
			+ "\t-rh receiveZMQHost	接收消息主机IP\n"
			+ "-rp receiveZMQPort	接收消息主机端口\n"
			+ "\t-rm receiveZMQModel	接收ZMQ连接模式，需与发送连接模式匹配，可选：SUB、PULL\n"
			+ "\t-rt receiveTimeout	接收消息超时时间，单位：ms\n"
			+ "\t-rtn receiveTHreadNum	接收消息线程数量\n" + "-help 显示帮助";

	/**
	 * sendZMQHost sendZMQPort sendZMQModel sendMsgCount sendThreadNum
	 * 
	 * receiveZMQHost receiveZMQPort receiveZMQModel receiveTimeout
	 * receiveTHreadNum
	 * */
	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			System.out.println(HELPMSG);
			return;
		}

		if (args[0].equals("-help")) {
			System.out.println(HELPMSG);
			return;
		}

		Map<String, String> argsMap = new HashMap<String, String>();
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (!args[i].startsWith("-")) {
					i++;
					continue;
				}

				argsMap.put(args[i].trim(), args[i + 1].trim());
				i++;
			}
		}

		System.out.println("correct parameters: ");
		Iterator<String> iter = argsMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			String value = argsMap.get(key);
			System.out.print(key + ": " + value + " ");
		}
		System.out.println("\n");
		
		int sendThreadNum = argsMap.get("-stn") == null ? 1 : Integer
				.parseInt(argsMap.get("-stn"));
		
		int receiverThreadNum = argsMap.get("-rtn") == null ? 1 : Integer
				.parseInt(argsMap.get("-rtn"));
		for (int i = 0; i < receiverThreadNum; i++) {
			try {
				ReceiveThread pull = new ReceiveThread(argsMap.get("-rh"),
						argsMap.get("-rp"), argsMap.get("-rm"),
						"Receiver-" + i, argsMap.get("-rt"), sendThreadNum == 0 ? 0 : Integer.parseInt(argsMap.get("-sc")) + sendThreadNum);
				
				Thread pullThread = new Thread(pull, "Receiver-" + i);
				pullThread.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(HELPMSG);

				return;
			}
			
		}
		
		if(sendThreadNum > 0 && Integer.parseInt(argsMap.get("-sc")) > 0){
			int sendNumPerThread = Integer.parseInt(argsMap.get("-sc")) / sendThreadNum;
			for (int i = 0; i < sendThreadNum; i++) {
				try {
					Thread pushThread = new Thread(new SendThread(argsMap.get("-sh"), argsMap.get("-sp"), argsMap.get("-sm"),
							sendNumPerThread, "Send-" + i, argsMap.get("-startImsi"), argsMap.get("-srepeat")));
					
					pushThread.start();
				} catch (Exception e) {
					
					if(e instanceof ParamException){
						System.out.println(((ParamException)e).toString());
					}else{
						e.printStackTrace();
					}
					
					System.out.println(HELPMSG);

					return;
				}
			}
		}
		
	}
}
