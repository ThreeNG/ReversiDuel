/**
 * Created by kanari on 2016/7/26.
 */

package NetworkUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MulticastManager {

	/**
	 * groupIP           : IP multicast group address
	 * port              : multicast port
	 * broadcastInterval : time between two consecutive UDP broadcasts
	 */
	private String groupIP = "224.0.0.87";
	private int port = 8263;
	private int broadcastInterval = 1000;    // milliseconds

	public String getGroupIP() {
		return groupIP;
	}

	public void setGroupIP(String groupIP) {
		this.groupIP = groupIP;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getBroadcastInterval() {
		return broadcastInterval;
	}

	public void setBroadcastInterval(int broadcastInterval) {
		this.broadcastInterval = broadcastInterval;
	}

	/**
	 * @param onReceiveData
	 *     Method called when data is received. Should accept a String as parameter.
	 *
	 * @param sendDataProvider
	 *     Method called when preparing data for sending. Should return a String.
	 */
	private MulticastReceiver receiver;
	private MulticastTransceiver transceiver;

	private Thread receiverThread, transceiverThread;

	MulticastManager(Consumer<String> onReceiveData, Callable<String> sendDataProvider) {
//		try {
//			System.out.println(InetAddress.getLocalHost());
//			for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()))
//				Collections.list(networkInterface.getInetAddresses()).forEach(System.out::println);
//		} catch (UnknownHostException | SocketException e) {
//			e.printStackTrace();
//		}

		receiver = new MulticastReceiver(groupIP, port);
		transceiver = new MulticastTransceiver(groupIP, port, broadcastInterval);
		receiver.setOnReceiveData(onReceiveData);
		transceiver.setSendDataProvider(sendDataProvider);

		receiverThread = new Thread(receiver);
		receiverThread.start();
	}

	/**
	 * Broadcast control
	 */
	public void startHost() {
		transceiverThread = new Thread(transceiver);
		transceiverThread.start();
	}

	public void abortHost() {
		transceiverThread.interrupt();
	}

	/**
	 * Thread listening for incoming data, runs constantly
	 */
	class MulticastReceiver implements Runnable {
		private MulticastSocket socket;
		private InetAddress group;
		private int port;

		MulticastReceiver(String groupIP, int port) {
			try {
				socket = new MulticastSocket(port);
				group = InetAddress.getByName(groupIP);
				this.port = port;
				socket.joinGroup(group);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private Consumer<String> onReceiveData;

		public Consumer<String> getOnReceiveData() {
			return onReceiveData;
		}

		public void setOnReceiveData(Consumer<String> onReceiveData) {
			this.onReceiveData = onReceiveData;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[256];
			while (!Thread.interrupted()) {
				try {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
					onReceiveData.accept(received);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				socket.leaveGroup(group);
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket.close();
		}
	}

	/**
	 * Thread sending outgoing data, can be stopped
	 */
	class MulticastTransceiver implements Runnable {
		private MulticastSocket socket;
		private InetAddress group;
		private int port;
		private int broadcastInterval;

		MulticastTransceiver(String groupIP, int port, int broadcastInterval) {
			try {
				socket = new MulticastSocket(port);
				group = InetAddress.getByName(groupIP);
				this.port = port;
				this.broadcastInterval = broadcastInterval;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private Callable<String> sendDataProvider;

		public Callable<String> getSendDataProvider() {
			return sendDataProvider;
		}

		public void setSendDataProvider(Callable<String> sendDataProvider) {
			this.sendDataProvider = sendDataProvider;
		}

		@Override
		public void run() {
			byte[] buffer;
			while (!Thread.interrupted()) {
				try {
					String sendData = sendDataProvider.call();
					buffer = sendData.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
					socket.send(packet);
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(broadcastInterval);
				} catch (InterruptedException ignored) {
				}
			}
			socket.close();
		}
	}
}
