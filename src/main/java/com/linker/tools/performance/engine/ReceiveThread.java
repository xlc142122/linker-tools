package com.linker.tools.performance.engine;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.linker.tools.common.ParamException;


class ReceiveThread implements Runnable {

	private String threadName;
	private Context context = null;
	private Socket socket = null;
	private int receiveCount;
	private int timeout = 0;
	
	public ReceiveThread(String host, String port, String model, String threadName, String timeoutStr, int receiveCount) throws Exception{
		if(host == null || "".equals(host)){
			throw new ParamException("-rp", host);
		}
		
		if(port == null || "".equals(port)){
			throw new ParamException("-rh", port);
		}
		
		if((model == null || "".equals(model)) && ("SUB".equalsIgnoreCase(model) || "PULL".equalsIgnoreCase(model))){
			throw new ParamException("-rm", model);
		}

		if(timeoutStr == null || "".equals(timeoutStr)){
			throw new ParamException("-rt", timeoutStr);
		}else{
			try{
				timeout = Integer.parseInt(timeoutStr);
			}catch(Exception e){
				throw new ParamException("-rt", timeoutStr);
			}
		}
		
		this.threadName = threadName;
		this.receiveCount = receiveCount;
		
		String endPoint = "tcp://" + host + ":" + port;
		context = ZMQ.context(1);
		
		try{
			if("PULL".equalsIgnoreCase(model)){
				socket = context.socket(ZMQ.PULL);
			}else if("SUB".equalsIgnoreCase(model)){
				socket = context.socket(ZMQ.SUB);
			}
			
			socket.connect(endPoint);
			socket.setReceiveTimeOut(timeout);
		}catch(Exception e){
			throw e;
		}
		
	}

	@Override
	public void run() {
		System.out.println(this.threadName + " receive start: " + System.currentTimeMillis());
		int count = 0;
		int nomsg = 0;
		long start = System.currentTimeMillis();
		boolean receiveFinish = false;
		while(true){
			//count++;
			String msg = null;
			try{
				msg = new String(socket.recv());
				if(msg.startsWith("\"signalingDomain\"") && msg.indexOf("\"GX\"") > 0){
					count++;
					nomsg = 0;
				}else{
					System.out.println(msg);
					nomsg++;
				}
				
//				if(count >= receiveCount || nomsg > 10){
//					receiveFinish = true;
//					break;
//				}
			}catch(Exception e){
				//e.printStackTrace();
				break;
			}
			
			
		}
		
		socket.close();
		
		long end = System.currentTimeMillis();
		
		long tps = 0l;
		if(receiveFinish){
			if((end - start) / 1000l == 0){
				tps = count;
			}else{
				tps = count / ((end - start) / 1000l);
			}
		}else{
			if((end - start - timeout) / 1000l == 0){
				tps = count;
			}else{
				tps = count / ((end - start - timeout) / 1000l);
			}
		}
		
		System.out.println(this.threadName + " receive end: " + System.currentTimeMillis());
		System.out.println(this.threadName + " receive count: " + count + ", tps: " + tps);
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public int getReceiveCount() {
		return receiveCount;
	}

	public void setReceiveCount(int receiveCount) {
		this.receiveCount = receiveCount;
	}
}
