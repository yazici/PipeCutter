package com.kz.pipeCutter.BBB;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.SwingUtilities;

import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.kz.pipeCutter.ui.Settings;

import pb.Message;
import pb.Message.Container;
import pb.Types.ContainerType;

public class BBBStatus {
	ServiceListener bonjourServiceListener;
	ArrayList<ServiceInfo> services;
	static Socket statusSocket = null;
	static BBBStatus instance = null;
	public static ZMQ.Poller items = null;

	ByteArrayInputStream is;
	private JSch jsch;
	public ChannelExec channelExec = null;
	private Session session;
	private PrintStream ps;

	private static int ticket = 5000;
	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public BBBStatus() {
		Runnable errorReporter = new Runnable() {
			@Override
			public void run() {
				try {
					if (!Settings.instance.isVisible())
						return;

					Container contReturned;
					ZMsg receivedMessage = ZMsg.recvMsg(getStatusSocket());
					int i = 0;
					// System.out.println("loop: " + i);
					for (ZFrame f : receivedMessage) {
						byte[] returnedBytes = f.getData();
						String messageType = new String(returnedBytes);
						if (!messageType.equals("motion")) {
							contReturned = Message.Container.parseFrom(returnedBytes);
							if (contReturned.getType().equals(ContainerType.MT_EMCSTAT_FULL_UPDATE)
									|| contReturned.getType().equals(ContainerType.MT_EMCSTAT_INCREMENTAL_UPDATE)) {
								final double x = contReturned.getEmcStatusMotion().getActualPosition().getX();
								final double y = contReturned.getEmcStatusMotion().getActualPosition().getY();
								final double z = contReturned.getEmcStatusMotion().getActualPosition().getZ();
								final double a = contReturned.getEmcStatusMotion().getActualPosition().getA();
								final double b = contReturned.getEmcStatusMotion().getActualPosition().getB();
								final double c = contReturned.getEmcStatusMotion().getActualPosition().getC();

								// Settings.instance.setSetting("position_x", x);
								// Settings.instance.setSetting("position_y", y);
								// Settings.instance.setSetting("position_z", z);
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										Settings.instance.setSetting("position_a", a);
										Settings.instance.setSetting("position_b", b);
										Settings.instance.setSetting("position_c", c);
									}
								});
							}
						}
					}
				} catch (Exception e) {
					if (!e.getMessage().equals("Unknown message type."))
						e.printStackTrace();
				}
			}
		};
		scheduler.scheduleAtFixedRate(errorReporter, 0, 10, TimeUnit.MILLISECONDS);
		// scheduler.schedule(errorReporter, 0, TimeUnit.SECONDS);
		statusSocket = null;
		instance = this;

	}

	public static void main(String[] args) {

		Settings sett = new Settings();
		sett.setVisible(true);

		BBBStatus status = new BBBStatus();

	}

	private static Socket getStatusSocket() {

		if (statusSocket != null)
			return statusSocket;

		String statusUrl = Settings.getInstance().getSetting("machinekit_statusService_url");

		Context con = ZMQ.context(1);
		statusSocket = con.socket(ZMQ.SUB);
		statusSocket.setReceiveTimeOut(10000);
		statusSocket.setLinger(10);
		statusSocket.connect(statusUrl);

		statusSocket.subscribe("motion".getBytes());

		return statusSocket;
	}

	// public void ping() throws Exception {
	// System.out.println(new Object() {
	// }.getClass().getEnclosingMethod().getName());
	//
	// byte[] buff;
	// Container container =
	// Container.newBuilder().setType(Types.ContainerType.MT_PING).setTicket(ticket++).build();
	// buff = container.toByteArray();
	// String hexOutput = javax.xml.bind.DatatypeConverter.printHexBinary(buff);
	// System.out.println("Mesage: " + hexOutput);
	// getCommandSocket().send(buff);
	//
	// parseAndOutput();
	// }

}