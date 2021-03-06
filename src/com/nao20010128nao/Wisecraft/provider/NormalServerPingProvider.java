package com.nao20010128nao.Wisecraft.provider;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.nao20010128nao.MCPing.pc.PCQuery;
import com.nao20010128nao.MCPing.pe.PEQuery;
import com.nao20010128nao.Wisecraft.Server;
import com.nao20010128nao.Wisecraft.ServerStatus;
import com.nao20010128nao.Wisecraft.Utils;
import com.nao20010128nao.Wisecraft.misc.SprPair;
import com.nao20010128nao.Wisecraft.pingEngine.UnconnectedPing;

public class NormalServerPingProvider implements ServerPingProvider {
	Queue<Map.Entry<Server, PingHandler>> queue = new LinkedList<>();
	Thread pingThread = new PingThread();

	public void putInQueue(Server server, PingHandler handler) {
		Utils.requireNonNull(server);
		Utils.requireNonNull(handler);
		queue.add(new KVP(server, handler));
		if (!pingThread.isAlive()) {
			pingThread = new PingThread();
			pingThread.start();
		}
	}

	@Override
	public int getQueueRemain() {
		// TODO: Implement this method
		return queue.size();
	}

	private class PingThread extends Thread implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
			Map.Entry<Server, PingHandler> now = null;
			while (!queue.isEmpty()) {
				now = queue.poll();
				ServerStatus stat = new ServerStatus();
				stat.ip = now.getKey().ip;
				stat.port = now.getKey().port;
				stat.isPC = now.getKey().isPC;
				if (now.getKey().isPC) {
					PCQuery query = new PCQuery(stat.ip, stat.port);
					try {
						stat.response = query.fetchReply();
					} catch (IOException e) {
						e.printStackTrace();
						try {
							now.getValue().onPingFailed(now.getKey());
						} catch (Throwable ex) {

						}
						continue;
					}
					stat.ping = query.getLatestPingElapsed();
				} else {
					PEQuery query = new PEQuery(stat.ip, stat.port);
					try {
						stat.response = query.fullStatUni();
						try {
							UnconnectedPing.UnconnectedPingResult res = UnconnectedPing.doPing(stat.ip, stat.port);
							SprPair pair = new SprPair();
							pair.setA(stat.response);
							pair.setB(res);
							stat.response = pair;
						} catch (IOException e) {
						}
						stat.ping = query.getLatestPingElapsed();
					} catch (Throwable e) {
						e.printStackTrace();
						try {
							UnconnectedPing.UnconnectedPingResult res = UnconnectedPing.doPing(stat.ip, stat.port);
							stat.response = res;
							stat.ping = res.getLatestPingElapsed();
						} catch (IOException ex) {
							try {
								now.getValue().onPingFailed(now.getKey());
							} catch (Throwable ex_) {

							}
							continue;
						}
					}
				}
				try {
					now.getValue().onPingArrives(stat);
				} catch (Throwable f) {

				}
			}
		}
	}

	private class KVP implements Map.Entry<Server, PingHandler> {
		Server server;
		PingHandler handler;

		public KVP(Server server, PingHandler handler) {
			this.server = server;
			this.handler = handler;
		}

		@Override
		public NormalServerPingProvider.PingHandler setValue(NormalServerPingProvider.PingHandler p1) {
			// TODO: Implement this method
			PingHandler old = handler;
			handler = p1;
			return old;
		}

		@Override
		public Server getKey() {
			// TODO: Implement this method
			return server;
		}

		@Override
		public NormalServerPingProvider.PingHandler getValue() {
			// TODO: Implement this method
			return handler;
		}
	}
}
