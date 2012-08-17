/*******************************************************************************
 * Copyright (c) 2009,2011 Tecnalia Research and Innovation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Raul Otaolea (Tecnalia Research and Innovation - Software Systems Engineering) - initial API, implementation and documentation
 *    Fran Ruiz (Tecnalia Research and Innovation - Software Systems Engineering) - initial API, implementation and documentation
 *******************************************************************************/ 

package eu.sofia.adk.gateway.tcpip.core;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.sofia.adk.gateway.tcpip.aynch.TCPIPGatewayThreadFactory;


/**
 * SocketChannelWriter.java
 * 
 * A thread pool with an incoming BlockingQueue of ClientMessage.
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class SocketChannelWriter extends Thread {
	
	/** Incoming message queue */
	protected ArrayBlockingQueue<SocketChannelWriteOperation> queue;

	/** To indicate if the writer is running. **/
	protected boolean running = false;

	/** our pool of worker threads */
	private ExecutorService pool;
	
	/** Configuration */
	private TCPIPGatewayConfiguration config;

	public SocketChannelWriter(ThreadGroup tg, TCPIPGatewayConfiguration config) {
		super(tg, "SocketChannelWriter");
		this.config = config;
		
		queue = new ArrayBlockingQueue<SocketChannelWriteOperation>(config.getInt(TCPIPGatewayConfiguration.WRITE_QUEUE_SIZE));	
		TCPIPGatewayThreadFactory tf = new TCPIPGatewayThreadFactory("SocketChannelWriter Worker", tg);
		pool = Executors.newFixedThreadPool(
				config.getInt(TCPIPGatewayConfiguration.EVENT_WRITER_WORKERS), tf);
	}

	/**
	 * Shutdown the pool threads.
	 */
	public void shutdown() {
		// TODO: Do it smarter. Close the queue, wait until all workers are done, and then shutdown the pool.
		running = false;
		this.interrupt();
		pool.shutdown();
	}

	/**
	 * Queue the event for later processing by worker threads.
	 */
	public void sendMessage(ClientMessage message, SocketChannel channel) {
		try {
			SocketChannelWriteOperation wo = new SocketChannelWriteOperation(channel, message);
			wo.setChannelWriteSleep(config.getLong(TCPIPGatewayConfiguration.CHANNEL_WRITE_SLEEP));
			queue.put(wo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * retrieve events from the queue and process.
	 */
	public void run() {
		System.out.println("SocketChannelWriter started");
		SocketChannelWriteOperation operation;
		running = true;
		while (running) {
			try {
				operation = queue.take();
				pool.execute(operation);
			} catch (InterruptedException e) {
				// Nothing to do.
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("SocketChannelWriter finished");
	}

}
