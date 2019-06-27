package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.Configuration;

public class Peer {
	private static Logger log = Logger.getLogger(Peer.class.getName());

	class TcpLocalPeer {
		public int serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
		public String keyInfo = Configuration.getConfigurationValue("authorized_keys");
		public int clientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
		public String peersInfo = Configuration.getConfigurationValue("peers");
		public String advertisedName = Configuration.getConfigurationValue("advertisedName");
		public ArrayList<Document> conPeers = new ArrayList<Document>();
		public ArrayList<Socket> socketList = new ArrayList<Socket>();

		public synchronized void join(Document doc) {
			conPeers.add(doc);
		}

		public synchronized void remove(Integer index) {
			conPeers.remove(index);
			socketList.remove(index);
		}

		public synchronized void join(Socket socket) {
			socketList.add(socket);
		}

	}

	class UdpLocalPeer {
		public int serverPort = Integer.parseInt(Configuration.getConfigurationValue("udpport"));
		public String peersInfo = Configuration.getConfigurationValue("peers");
		public String advertisedName = Configuration.getConfigurationValue("advertisedName");
		public ArrayList<Document> conPeers = new ArrayList<Document>();
		public ArrayList<String> expect = new ArrayList<String>();
		public DatagramSocket ds;
		public ServerMain serverMain;
		public infolist infolist;

		public UdpLocalPeer(DatagramSocket ds, infolist infolist) throws Exception, IOException {
			this.ds = ds;
			this.infolist = infolist;
			serverMain = new ServerMain(ds, infolist);
		}

		public synchronized void join(String str) {
			expect.add(str);
		}

		public synchronized void remove(String str) {
			expect.remove(str);
		}

		public synchronized ArrayList<String> look() {
			return expect;
		}

		public synchronized void join(Document doc) {
			conPeers.add(doc);
		}

		public synchronized void remove(Document doc) {
			conPeers.remove(doc);
		}

	}

	// client
	class TcpClient implements Runnable {
		private TcpLocalPeer lp;
		private boolean decisioner;
		private Document doc;

		public TcpClient(TcpLocalPeer lp, boolean decisioner) {
			this.lp = lp;
			this.decisioner = decisioner;
		}

		public TcpClient(TcpLocalPeer lp, Document doc, boolean decisioner) {
			this.doc = doc;
			this.lp = lp;
			this.decisioner = decisioner;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			String[] peers;
			if (!decisioner) {
				String tryHost = doc.getString("host");
				String tryPort = Long.toString(doc.getLong("port"));

				String tryPeer = tryHost + ":" + tryPort;
				peers = new String[] { tryPeer };
			} else {
				peers = lp.peersInfo.split(",");
			}

			for (int i = 0; i < peers.length; i++) {
				String[] peerInfo = peers[i].split(":");
				String peerIp = peerInfo[0];
				int peerPort = Integer.parseInt(peerInfo[1]);
				// HANDSHAKE_REQUEST doc
				try {

					Socket socket = new Socket(peerIp, peerPort);
					// one peer join!!!!
					Document handshake = new Document();
					Document portinfo = new Document();
					portinfo.append("host", socket.getLocalAddress().getHostAddress());
					portinfo.append("port", socket.getPort());
					handshake.append("command", "HANDSHAKE_REQUEST");
					handshake.append("hostPort", portinfo);

					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
					out.write(handshake.toJson() + "\n");
					out.flush();
					Thread thread = new Thread(new ServerThread(socket, lp));
					thread.start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//
	class UdpClient implements Runnable {

		private UdpLocalPeer lp;
		private boolean decisioner;
		private Document doc;

		public UdpClient(UdpLocalPeer lp, boolean decisioner) {
			this.lp = lp;
			this.decisioner = decisioner;
		}

		public UdpClient(UdpLocalPeer lp, Document doc, boolean decisioner) {
			this.doc = doc;
			this.lp = lp;
			this.decisioner = decisioner;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {
				String[] peers;
				if (!decisioner) {
					String tryHost = doc.getString("host");
					String tryPort = Integer.toString(doc.getInteger("port"));

					String tryPeer = tryHost + ":" + tryPort;
					peers = new String[] { tryPeer };
				} else {
					peers = lp.peersInfo.split(",");
				}

				for (int i = 0; i < peers.length; i++) {
					String[] peerInfo = peers[i].split(":");
					String peerIp = peerInfo[0];
					int peerPort = Integer.parseInt(peerInfo[1]);
					// HANDSHAKE_REQUEST doc

					// one peer join!!!!
					Document handshake = new Document();
					Document portinfo = new Document();
					portinfo.append("host", InetAddress.getLocalHost().getHostAddress());
					portinfo.append("port", lp.serverPort);
					handshake.append("command", "HANDSHAKE_REQUEST");
					handshake.append("hostPort", portinfo);
					byte[] data = handshake.toJson().getBytes();
					InetAddress address = InetAddress.getByName(peerIp);
					DatagramPacket packet = new DatagramPacket(data, data.length, address, peerPort);
					lp.ds.send(packet);
					String expectation = "HANDSHAKE_RESPONSE";
					lp.join(address.getHostAddress() + "," + Integer.toString(peerPort) + "," + expectation);
					Thread thread = new Thread(new checkList(lp, address, peerPort, expectation, handshake));
					thread.start();

				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	class TcpServer implements Runnable {
		private TcpLocalPeer lp;

		public TcpServer(TcpLocalPeer lp) {
			this.lp = lp;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {

				ServerSocket serverSocket = new ServerSocket(lp.serverPort);
				int count = 0;
				Socket Socket = null;
				while (true) {
					Socket = serverSocket.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(Socket.getInputStream(), "UTF8"));
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(Socket.getOutputStream(), "UTF8"));
					Document doc = Document.parse(in.readLine());
					if ("HANDSHAKE_REQUEST".equals(doc.get("command"))) {
						if (count >= Integer
								.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"))) {
							// CONNECTION_REFUSED
							Document handshakeRuf = new Document();
							handshakeRuf.append("command", "CONNECTION_REFUSED");
							handshakeRuf.append("message", "connection limit reached");
							handshakeRuf.append("peers", lp.conPeers);
							out.write(handshakeRuf.toJson() + "\n");
							out.flush();
							Socket.close();
						} else {
							// HANDSHAKE_RESPONSE
							Document handshakeRes = new Document();
							Document hostPort = new Document();
							handshakeRes.append("command", "HANDSHAKE_RESPONSE");
							// response with server host and port
							hostPort.append("host", Socket.getLocalAddress().getHostAddress());
							hostPort.append("port", Socket.getLocalPort());
							handshakeRes.append("hostPost", hostPort);
							out.write(handshakeRes.toJson() + "\n");
							out.flush();
							Document conInfo = new Document();
							conInfo.append("host", Socket.getInetAddress().getHostAddress());
							conInfo.append("port", Socket.getPort());
							lp.join(conInfo);
							lp.join(Socket);
							Thread thread = new Thread(new ServerThread(Socket, lp));
							thread.start();
							count++;
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// Connection thread
	class ServerThread implements Runnable {
		private Socket socket = null;
		Document doc;
		String command;
		ServerMain servermain;
		TcpLocalPeer lp;

		public ServerThread(Socket socket, TcpLocalPeer lp) {
			this.socket = socket;
			this.lp = lp;
		}

		public void run() {

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
				servermain = new ServerMain(out);
				Thread sycn = new Thread(new selfcheck(servermain));
				sycn.start();
				while (true) {
					String receive = in.readLine();
					if (receive == null) {
						socket.close();
						break;
					} else {
						doc = Document.parse(receive);
						if ("HANDSHAKE_RESPONSE".equals(doc.get("command"))) {
							Document conInfo = new Document();
							conInfo.append("host", socket.getInetAddress().getHostAddress());
							conInfo.append("port", socket.getPort());
							lp.join(conInfo);
							lp.join(socket);
						} else if ("CONNECTION_REFUSED".equals(doc.get("command"))) {
							ArrayList<Document> tryPeers = new ArrayList<Document>();
							tryPeers = (ArrayList<Document>) doc.get("peers");
							for (Document d : tryPeers) {
								try {
									TcpClient tryClient = new TcpClient(lp, d, false);
									Thread tryT1 = new Thread(tryClient);
									tryT1.start();
								} catch (Exception e) {

								}
							}
						}
						servermain.event_opeartion(doc);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	class UdpServer implements Runnable {
		private UdpLocalPeer lp;

		public UdpServer(UdpLocalPeer lp) {
			this.lp = lp;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				byte[] data = new byte[65535];
				DatagramPacket packet_rec = new DatagramPacket(data, data.length);
				while (true) {
					packet_rec.setLength(65535);
					lp.ds.receive(packet_rec);
					// create a thread to communicate with client
					String rec = new String(packet_rec.getData(), 0, packet_rec.getLength());
					Document recDoc = Document.parse(rec);
					String cmd = recDoc.getString("command");
					if ("HANDSHAKE_REQUEST".equals(cmd)) {
						if (lp.infolist.peerlook().size() <= Integer
								.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"))) {
							Document handshakeRes = new Document();
							Document hostPort = new Document();
							handshakeRes.append("command", "HANDSHAKE_RESPONSE");
							// response with server host and port
							hostPort.append("host", InetAddress.getLocalHost().getHostAddress());
							hostPort.append("port", lp.serverPort);
							handshakeRes.append("hostPost", hostPort);
							byte[] dataRes = handshakeRes.toJson().getBytes();
							InetAddress address = packet_rec.getAddress();
							int port = packet_rec.getPort();
							DatagramPacket packet = new DatagramPacket(dataRes, dataRes.length, address, port);
							lp.ds.send(packet);
							lp.infolist.peerjoin(address.getHostAddress() + "," + Integer.toString(port));
							selfcheck selfcheck = new selfcheck(lp.serverMain);
							Thread t3 = new Thread(selfcheck);
							t3.start();
						}
					} else if ("HANDSHAKE_RESPONSE".equals(cmd)) {
						InetAddress address = packet_rec.getAddress();
						int port = packet_rec.getPort();
						String exp = address.getHostAddress() + "," + Integer.toString(port) + ","
								+ "HANDSHAKE_RESPONSE";
						lp.remove(exp);
						lp.infolist.peerjoin(address.getHostAddress() + "," + Integer.toString(port));
						selfcheck selfcheck = new selfcheck(lp.serverMain);
						Thread t3 = new Thread(selfcheck);
						t3.start();
					} else {
						InetAddress address = packet_rec.getAddress();
						int port = packet_rec.getPort();

						lp.serverMain.event_opeartion(recDoc, address.getHostAddress(), port);
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	class checkList implements Runnable {

		private UdpLocalPeer lp;
		private String expectation;
		DatagramSocket udpsocket1;
		private InetAddress address;
		private int peerPort;
		Document doc;
		boolean flag = true;
		int i = 0;

		public checkList(UdpLocalPeer lp, InetAddress address, int peerPort, String expectation, Document doc) {
			this.lp = lp;
			this.expectation = expectation;
			this.address = address;
			this.peerPort = peerPort;
			this.doc = doc;
		}

		@Override
		public void run() {
			String expectStr = address.getHostAddress() + "," + Integer.toString(peerPort) + "," + expectation;
			while (flag && i <= Integer.parseInt(Configuration.getConfigurationValue("retry"))) {
				try {
					Thread.sleep(10 * 10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int count = 0;
				while (count < lp.expect.size()) {
					if (lp.expect.get(count).equals(expectStr)) {
						byte[] buffer = doc.toJson().getBytes();
						DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
						sendPacket.setAddress(address);
						sendPacket.setPort(peerPort);
						try {
							lp.ds.send(sendPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						flag = false;
						break;
					}
				}

				if (flag == false) {
					break;
				}
				i = i + 1;
			}
			if (flag == true) {
				int count = 0;
				while (count < lp.expect.size()) {
					if (lp.expect.get(count).equals(expectStr)) {
						lp.expect.remove(count);
						break;
					}
					count++;
				}
			}
		}
	}

	class selfcheck implements Runnable {
		public ServerMain serverMain;
		public int index = 0;
		BufferedWriter out;
		FileSystemEvent filesystemevent;

		public selfcheck(ServerMain serverMain) {
			this.serverMain = serverMain;
		}

		public void run() {

			ArrayList<FileSystemEvent> filesystemeventlist = new ArrayList<FileSystemEvent>();
			while (true) {
				try {
					filesystemeventlist = serverMain.fileSystemManager.generateSyncEvents();
					for (FileSystemEvent i : filesystemeventlist) {
						serverMain.processFileSystemEvent(i);
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block

				}
				try {
					Thread.sleep(6 * 10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	class listenClient implements Runnable {
		private TcpLocalPeer lp;

		public listenClient(TcpLocalPeer lp) {
			this.lp = lp;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				ServerSocket serverSocket = new ServerSocket(lp.clientPort);
				Socket socket = null;
				String clientPubKey = null;
				KeyGenerator keyGen = null;
				while (true) {
					socket = serverSocket.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
					Document doc = Document.parse(in.readLine());
					String[] clientInfo = lp.keyInfo.split("\\s|,\\s");
					if ("AUTH_REQUEST".equals(doc.getString("command"))) {
						for (int i = 0; i < clientInfo.length; i++) {
							if (clientInfo[i].equals(doc.getString("identity"))) {
								clientPubKey = clientInfo[i - 1];
							}
						}
						if (clientPubKey == null) {
							// AUTH_RESPONSE fail
							Document authRef = new Document();
							authRef.append("command", "AUTH_RESPONSE");
							authRef.append("status", false);
							authRef.append("message", "public key not found");
							out.write(authRef.toJson() + "\n");
							out.flush();
							socket.close();
						} else {
							keyGen = KeyGenerator.getInstance("AES");
							keyGen.init(128);
							SecretKey secretKey = keyGen.generateKey();
							// get secretKey string, this is the share key
							String encodedSecretKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
							// get public key
							byte[] keyBytes;
							keyBytes = Base64.getDecoder().decode(clientPubKey.getBytes());
							X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
							KeyFactory keyFactory = KeyFactory.getInstance("RSA");
							PublicKey publicKey = keyFactory.generatePublic(keySpec);
							// generate cipher class
							Cipher cipher = Cipher.getInstance("RSA");
							cipher.init(Cipher.ENCRYPT_MODE, publicKey);
							// get cipher
							byte[] enBytes = cipher.doFinal(encodedSecretKey.getBytes());
							String enconedKey = Base64.getEncoder().encodeToString(enBytes);

							Thread thread = new Thread(new ClientThread(socket, lp, secretKey));
							thread.start();

							Document authRef = new Document();
							authRef.append("command", "AUTH_RESPONSE");
							authRef.append("AES128", enconedKey);
							authRef.append("message", "public key found");
							authRef.append("status", true);
							out.write(authRef.toJson() + "\n");
							out.flush();

						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	class ClientThread implements Runnable {
		Socket socket = null;
		TcpLocalPeer lp;
		Document doc;
		Document sent;
		SecretKey secretKey;

		public ClientThread(Socket socket, TcpLocalPeer lp, SecretKey secretKey) {
			this.socket = socket;
			this.lp = lp;
			this.secretKey = secretKey;
		}

		@Override
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
				while (true) {
					String receive = in.readLine();
					// in case client is down
					if (receive == null) {
						socket.close();
						break;
					} else {
						// decrypt
						Document sent = new Document();
						doc = Document.parse(receive);
						String commStr = doc.getString("payload");
						Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						cipher.init(Cipher.DECRYPT_MODE, secretKey);
						byte[] encodeReceive = Base64.getDecoder().decode(commStr.getBytes());
						byte[] decodeReceive = cipher.doFinal(encodeReceive);
						String finalReceive = new String(decodeReceive);
						finalReceive.trim();
						// receive to document
						Document operation = new Document();
						operation = Document.parse(finalReceive);

						if ("LIST_PEERS_REQUEST".equals(operation.getString("command"))) {
							sent.append("command", "LIST_PEERS_RESPONSE");
							sent.append("peers", lp.conPeers);
						} else if ("CONNECT_PEER_REQUEST".equals(operation.getString("command"))) {
							sent.append("command", "CONNECT_PEER_RESPONSE");
							sent.append("host", operation.getString("host"));
							sent.append("port", operation.getLong("port"));
							sent.append("status", true);
							sent.append("message", "connected to peer");
							// new peer commmunication
							TcpClient client = new TcpClient(lp, operation, false);
							Thread clientT = new Thread(client);
							clientT.start();
						} else if ("DISCONNECT_PEER_REQUEST".equals(operation.get("command"))) {
							int index = -1;
							for (int i = 0; i < lp.conPeers.size(); i++) {
								System.out.println(lp.conPeers.get(i).toJson());
								if (operation.getString("host").equals(lp.conPeers.get(i).get("host"))) {
									index = i;
								}
							}
							System.out.println(lp.socketList.size());
							if (index != -1) {
								sent.append("command", "DISCONNECT_PEER_RESPONSE");
								sent.append("host", operation.getString("host"));
								sent.append("port", operation.getLong("port"));
								sent.append("status", true);
								sent.append("message", "disconnected from peer");
								Socket closeSocket = lp.socketList.get(index);
								closeSocket.close();
								lp.remove(index);

							}
						}
						String sentStr = sent.toJson() + "\n";
						Cipher cipher1 = Cipher.getInstance("AES/ECB/PKCS5Padding");
						cipher1.init(Cipher.ENCRYPT_MODE, secretKey);
						byte[] enBytes = cipher1.doFinal(sentStr.getBytes());
						String encodeSent = Base64.getEncoder().encodeToString(enBytes);
						// generate sent document
						Document sentDoc = new Document();
						sentDoc.append("payload", encodeSent);
						out.write(sentDoc.toJson() + "\n");
						out.flush();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public void useThread() {
		String mode = Configuration.getConfigurationValue("mode");
		if (mode.equals("udp")) {
			infolist infolist = new infolist();
			int serverPort = Integer.parseInt(Configuration.getConfigurationValue("udpport"));
			DatagramSocket ds;
			try {
				ds = new DatagramSocket(serverPort);
				UdpLocalPeer lp = new UdpLocalPeer(ds, infolist);
				UdpClient client = new UdpClient(lp, true);
				Thread t1 = new Thread(client);
				t1.start();
				UdpServer server = new UdpServer(lp);
				Thread t2 = new Thread(server);
				t2.start();

			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (mode.equals("tcp")) {
			TcpLocalPeer lp = new TcpLocalPeer();
			TcpClient client = new TcpClient(lp, true);
			Thread t1 = new Thread(client);
			t1.start();
			TcpServer server = new TcpServer(lp);
			Thread t2 = new Thread(server);
			t2.start();
			listenClient lc = new listenClient(lp);
			Thread t3 = new Thread(lc);
			t3.start();
		}
	}

	public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
		log.info("BitBox Peer starting...");
		Configuration.getConfiguration();
		Peer p = new Peer();

		p.useThread();
	}
}
