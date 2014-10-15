package com.linker.tools.performance.engine;

import java.text.DecimalFormat;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.linker.tools.common.ParamException;

public class SendThread implements Runnable {
	private String threadName;
	private int sendNum;
	private long startImsi;
	private boolean repeat;	//true:重复发送， false:循环发送

	private Context context = null;
	private Socket socket = null;
	
	//private static String MESSAGE = "\"signalingDomain\":{\"commonProtocolInfo\":\"9012:272|9018:1|9020:16777238|9015:436210420|9014:436210420|9016:pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org:77|\",\"interfaceIndicator\":\"GX\",\"commandTypeIndicator\":\"CCR\",\"userModel\":{\"subscriptionIds\":[{\"subscriptionIdType\":\"END_USER_E164\",\"subscriptionIdData\":\"18901[E164]\"},{\"subscriptionIdType\":\"END_USER_IMSI\",\"subscriptionIdData\":\"[IMSI]\"}]},\"commandHead\":{\"sessionId\":\"pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org;1410559215;32775051;[SESSIONID]\",\"originHost\":\"pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org\",\"originRealm\":\"epc.mnc011.mcc460.3gppnetwork.org\",\"destinationRealm\":\"epc.mnc011.mcc460.3gppnetwork.Org\",\"destinationHost\":\"pcrf01-B-np.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org\",\"cCRequestType\":\"INITIAL_REQUEST\",\"cCRequestNumber\":0,\"originStateId\":315360000},\"ipCanSession\":{\"iPCANType\":\"_3GPP2\",\"bearer\":{\"networkRequestSupport\":\"NETWORK_REQUEST_NOT_SUPPORTED\"}},\"chargingInfo\":{\"online\":\"DISABLE_ONLINE\",\"offline\":\"ENABLE_OFFLINE\"},\"constraintCondition\":{\"accessInformation\":{\"rATType\":\"HRPD\"},\"locationInformation\":{\"networkElementLocation\":{\"tGPPSGSNAddress\":{\"ipAddressType\":\"IPV4\",\"value\":\"172.29.3.45\"},\"accessNetworkChargingAddress\":{\"ipAddressType\":\"IPV4\",\"value\":\"115.168.14.41\"}},\"userLocation\":{\"framedIPAddress\":{\"ipAddressType\":\"IPV4\",\"value\":\"10.192.135.189\"},\"tGPP2BSID\":{\"sID\":\"376F\",\"nID\":\"B\",\"cellId\":\"70\",\"sector\":\"4\"}}},\"aPNInformation\":{\"apnName\":\"ctwap@mycdma.cn\"}},\"rxCustom\":{\"routeRecords\":[\"pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org\"]}}";
	private static String MESSAGE = "\"signalingDomain\":{\"commandTypeIndicator\":\"CCR\",\"commonProtocolInfo\":\"9012:272|9018:1|9020:16777238|9015:235301302|9014:235301302|9016:pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org:30|\",\"interfaceIndicator\":\"GX\",\"commandHead\":{\"sessionId\":\"pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org;1411341097;37817433;67174407\",\"originHost\":\"pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org\",\"originRealm\":\"epc.mnc011.mcc460.3gppnetwork.org\",\"destinationRealm\":\"epc.mnc011.mcc460.3gppnetwork.Org\",\"cCRequestType\":\"INITIAL_REQUEST\",\"cCRequestNumber\":0,\"destinationHost\":\"pcrf01-B-np.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org\",\"originStateId\":315360000},\"userModel\":{\"subscriptionIds\":[{\"subscriptionIdType\":\"END_USER_E164\",\"subscriptionIdData\":\"8613361617349\"},{\"subscriptionIdType\":\"END_USER_IMSI\",\"subscriptionIdData\":\"460036701072758\"}]},\"ipCanSession\":{\"iPCANType\":\"_3GPP2\",\"bearer\":{\"networkRequestSupport\":\"NETWORK_REQUEST_NOT_SUPPORTED\"}},\"chargingInfo\":{\"online\":\"DISABLE_ONLINE\",\"offline\":\"ENABLE_OFFLINE\"},\"rxCustom\":{\"routeRecords\":[\"pdsn01-B-ze.nc.jx.node.epc.mnc011.mcc460.3gppnetwork.org\"]},\"constraintCondition\":{\"aPNInformation\":{\"apnName\":\"ctwap@mycdma.cn\"},\"accessInformation\":{\"rATType\":\"HRPD\"},\"locationInformation\":{\"userLocation\":{\"framedIPAddress\":{\"ipAddressType\":\"IPV4\",\"value\":\"10.193.16.192\"},\"tGPP2BSID\":{\"sID\":\"376F\",\"nID\":\"B\",\"cellId\":\"A0\",\"sector\":\"E\"}},\"networkElementLocation\":{\"tGPPSGSNAddress\":{\"ipAddressType\":\"IPV4\",\"value\":\"172.29.1.158\"},\"accessNetworkChargingAddress\":{\"ipAddressType\":\"IPV4\",\"value\":\"115.168.14.42\"}}}}}";

	public SendThread(String host, String port, String model, int sendNum, String threadName, String startImsi, String repeat) throws Exception{
		if(host == null || "".equals(host)){
			throw new ParamException("-sp", host);
		}
		
		if(port == null || "".equals(port)){
			throw new ParamException("-sh", port);
		}
		
		if((model == null || "".equals(model)) && ("PUB".equalsIgnoreCase(model) || "PUSH".equalsIgnoreCase(model))){
			throw new ParamException("-sm", model);
		}
		
		this.startImsi = Long.parseLong(startImsi);
		
		this.sendNum = sendNum;
		this.threadName = threadName;
		this.repeat = "true".equalsIgnoreCase(repeat) ? true : false;
		
		String endPoint = "tcp://" + host + ":" + port;
		context = ZMQ.context(1);
		
		try{
			if("PUSH".equalsIgnoreCase(model)){
				socket = context.socket(ZMQ.PUSH);
			}else if("PUB".equalsIgnoreCase(model)){
				socket = context.socket(ZMQ.PUB);
			}
			
			socket.connect(endPoint);
		}catch(Exception e){
			throw e;
		}
	}

	@Override
	public void run() {
		DecimalFormat df = new DecimalFormat("000000");
		DecimalFormat sessionIdFormat = new DecimalFormat(threadName.substring(threadName.length() - 1) + "00000000");
		long start = System.currentTimeMillis();
		System.out.println(threadName + " begin send msg at " + start);
		int count = 0;
		while(true){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			count++;
			
			String sendMsg = MESSAGE.replace("[IMSI]", Long.toString(startImsi));
			sendMsg = sendMsg.replace("[SESSIONID]", sessionIdFormat.format(count));
			sendMsg = sendMsg.replace("[E164]", df.format(count));
			
//			System.out.println(sendMsg);
			
			socket.send(sendMsg);
			
			if(count > sendNum){
				break;
			}
			
			if(!repeat){
				startImsi++;
			}
		}
		
		socket.close();
		
		long end = System.currentTimeMillis();
		
		System.out.println(threadName + " finish send msg at " + end);
		
		long tps = count;
		if((end - start) / 1000L > 0){
			tps = count / ((end - start) / 1000);
		}
		
		System.out.println(threadName + " send " + count + " used time: " + (end - start) + ", tps: " + tps);
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public int getSendNum() {
		return sendNum;
	}

	public void setSendNum(int sendNum) {
		this.sendNum = sendNum;
	}

}
