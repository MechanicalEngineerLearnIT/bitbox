package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.EVENT;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	BufferedWriter out;
	Boolean Sync;
	private String mode;
	DatagramSocket udpsocket;
	infolist infolist;

	public ServerMain(BufferedWriter out) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		this.out = out;
		this.mode = "tcp";
	}

	public ServerMain(DatagramSocket udpsocket, infolist infolist)
			throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		this.udpsocket = udpsocket;
		this.mode = "udp";
		this.infolist = infolist;
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO Auto-generated method stub
		if (this.mode.equals("udp")) {
			Document doc1 = new Document();
			for (String peer : infolist.peerlist) {
				InetAddress address = null;
				try {
					address = InetAddress.getByName(peer.split(",")[0]);
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					if (fileSystemEvent.event.equals(EVENT.DIRECTORY_CREATE)) {
						doc1.append("command", "DIRECTORY_CREATE_REQUEST");
						doc1.append("pathName", fileSystemEvent.pathName);
						if (mode.equals("udp")) {
							byte[] buffer = doc1.toJson().getBytes();
							DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
							sendPacket.setAddress(address);
							sendPacket.setPort(Integer.parseInt(peer.split(",")[1]));
							String expection = new String();
							expection = expection + peer.split(",")[0] + ",";
							expection = expection + peer.split(",")[1] + ",";
							expection = expection + "DIRECTORY_CREATE_RESPONSE";
							infolist.exceptjoin(expection);
							udpsocket.send(sendPacket);
							Thread thread = new Thread(new checkList(doc1, expection, udpsocket));
							thread.start();
						} else {
							out.write(doc1.toJson() + "\r\n");
							out.flush();
						}
					} else if (fileSystemEvent.event.equals(EVENT.DIRECTORY_DELETE)) {
						doc1.append("command", "DIRECTORY_DELETE_REQUEST");
						doc1.append("pathName", fileSystemEvent.pathName);
						if (mode.equals("udp")) {
							byte[] buffer = doc1.toJson().getBytes();
							DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
							sendPacket.setAddress(address);
							sendPacket.setPort(Integer.parseInt(peer.split(",")[1]));
							udpsocket.send(sendPacket);
							String expection = new String();
							expection = expection + peer.split(",")[0] + ",";
							expection = expection + peer.split(",")[1] + ",";
							expection = expection + "DIRECTORY_DELETE_RESPONSE";
							infolist.exceptjoin(expection);
							Thread thread = new Thread(new checkList(doc1, expection, udpsocket));
							thread.start();
						} else {
							out.write(doc1.toJson() + "\r\n");
							out.flush();
						}
					} else if (fileSystemEvent.event.equals(EVENT.FILE_MODIFY)) {
						doc1.append("command", "FILE_MODIFY_REQUEST");
						Document doc2 = new Document();
						ArrayList<String> fileDescriptor = new ArrayList<String>();
						doc2.append("md5", fileSystemEvent.fileDescriptor.md5);
						doc2.append("lastModified", fileSystemEvent.fileDescriptor.lastModified);
						doc2.append("fileSize", fileSystemEvent.fileDescriptor.fileSize);

						doc1.append("fileDescriptor", doc2);
						doc1.append("pathName", fileSystemEvent.pathName);
						// ("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						if (mode.equals("udp")) {
							byte[] buffer = doc1.toJson().getBytes();
							DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
							sendPacket.setAddress(address);
							sendPacket.setPort(Integer.parseInt(peer.split(",")[1]));
							udpsocket.send(sendPacket);
							String expection = new String();
							expection = expection + peer.split(",")[0] + ",";
							expection = expection + peer.split(",")[1] + ",";
							expection = expection + "FILE_BYTES_REQUEST";
							infolist.exceptjoin(expection);
							Thread thread = new Thread(new checkList(doc1, expection, udpsocket));
							thread.start();

						} else {
							out.write(doc1.toJson() + "\r\n");
							out.flush();
						}
					} else if (fileSystemEvent.event.equals(EVENT.FILE_CREATE)) {
						doc1.append("command", "FILE_CREATE_REQUEST");
						Document doc2 = new Document();
						doc2.append("md5", fileSystemEvent.fileDescriptor.md5);
						doc2.append("lastModified", fileSystemEvent.fileDescriptor.lastModified);
						doc2.append("fileSize", fileSystemEvent.fileDescriptor.fileSize);

						doc1.append("fileDescriptor", doc2);
						doc1.append("pathName", fileSystemEvent.pathName);
						if (mode.equals("udp")) {
							byte[] buffer = doc1.toJson().getBytes();
							DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
							sendPacket.setAddress(address);
							sendPacket.setPort(Integer.parseInt(peer.split(",")[1]));
							udpsocket.send(sendPacket);
							String expection = new String();
							expection = expection + peer.split(",")[0] + ",";
							expection = expection + peer.split(",")[1] + ",";
							expection = expection + "FILE_BYTES_REQUEST";
							infolist.exceptjoin(expection);
							Thread thread = new Thread(new checkList(doc1, expection, udpsocket));
							thread.start();

						} else {
							out.write(doc1.toJson() + "\r\n");
							out.flush();

						}
					} else if (fileSystemEvent.event.equals(EVENT.FILE_DELETE)) {
						doc1.append("command", "FILE_DELETE_REQUEST");
						Document doc2 = new Document();
						ArrayList<String> fileDescriptor = new ArrayList<String>();
						doc2.append("md5", fileSystemEvent.fileDescriptor.md5);
						doc2.append("lastModified", fileSystemEvent.fileDescriptor.lastModified);
						doc2.append("fileSize", fileSystemEvent.fileDescriptor.fileSize);

						doc1.append("fileDescriptor", doc2);
						doc1.append("pathName", fileSystemEvent.pathName);
						if (mode.equals("udp")) {
							byte[] buffer = doc1.toJson().getBytes();
							DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
							sendPacket.setAddress(address);
							sendPacket.setPort(Integer.parseInt(peer.split(",")[1]));
							udpsocket.send(sendPacket);
							String expection = new String();
							expection = expection + peer.split(",")[0] + ",";
							expection = expection + peer.split(",")[1] + ",";
							expection = expection + "FILE_DELETE_RESPONSE";
							infolist.exceptjoin(expection);
							Thread thread = new Thread(new checkList(doc1, expection, udpsocket));
							thread.start();

						} else {
							out.write(doc1.toJson() + "\r\n");
							out.flush();
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (this.mode.equals("tcp")) {
			Document doc1 = new Document();
			try {
				if (fileSystemEvent.event.equals(EVENT.DIRECTORY_CREATE)) {
					doc1.append("command", "DIRECTORY_CREATE_REQUEST");
					doc1.append("pathName", fileSystemEvent.pathName);

					out.write(doc1.toJson() + "\r\n");
					out.flush();
				} else if (fileSystemEvent.event.equals(EVENT.DIRECTORY_DELETE)) {
					doc1.append("command", "DIRECTORY_DELETE_REQUEST");
					doc1.append("pathName", fileSystemEvent.pathName);
					out.write(doc1.toJson() + "\r\n");
					out.flush();
				} else if (fileSystemEvent.event.equals(EVENT.FILE_MODIFY)) {
					doc1.append("command", "FILE_MODIFY_REQUEST");
					Document doc2 = new Document();
					ArrayList<String> fileDescriptor = new ArrayList<String>();
					doc2.append("md5", fileSystemEvent.fileDescriptor.md5);
					doc2.append("lastModified", fileSystemEvent.fileDescriptor.lastModified);
					doc2.append("fileSize", fileSystemEvent.fileDescriptor.fileSize);

					doc1.append("fileDescriptor", doc2);
					doc1.append("pathName", fileSystemEvent.pathName);
					out.write(doc1.toJson() + "\r\n");
					out.flush();
				} else if (fileSystemEvent.event.equals(EVENT.FILE_CREATE)) {
					doc1.append("command", "FILE_CREATE_REQUEST");
					Document doc2 = new Document();
					doc2.append("md5", fileSystemEvent.fileDescriptor.md5);
					doc2.append("lastModified", fileSystemEvent.fileDescriptor.lastModified);
					doc2.append("fileSize", fileSystemEvent.fileDescriptor.fileSize);

					doc1.append("fileDescriptor", doc2);
					doc1.append("pathName", fileSystemEvent.pathName);
					out.write(doc1.toJson() + "\r\n");
					out.flush();

				} else if (fileSystemEvent.event.equals(EVENT.FILE_DELETE)) {
					doc1.append("command", "FILE_DELETE_REQUEST");
					Document doc2 = new Document();
					ArrayList<String> fileDescriptor = new ArrayList<String>();
					doc2.append("md5", fileSystemEvent.fileDescriptor.md5);
					doc2.append("lastModified", fileSystemEvent.fileDescriptor.lastModified);
					doc2.append("fileSize", fileSystemEvent.fileDescriptor.fileSize);

					doc1.append("fileDescriptor", doc2);
					doc1.append("pathName", fileSystemEvent.pathName);
					out.write(doc1.toJson() + "\r\n");
					out.flush();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
////////////
	////////////
	////////////////
	//////////////

	public void event_opeartion(Document doc1, String ResponseAddress, int ResponsePort)
			throws IOException, NoSuchAlgorithmException {

		InetAddress address = InetAddress.getByName(ResponseAddress);

		String portString = String.valueOf(ResponsePort);

		String command = doc1.getString("command");

		String exception = ResponseAddress + "," + portString + "," + command;

		int count = 0;
		while (count < infolist.exceptlook().size()) {
			if (infolist.exceptlook().get(count).equals(exception)) {
				infolist.exceptremove(count);
				break;
			}
			count++;
		}

		if (doc1.getString("command").equals("DIRECTORY_CREATE_REQUEST")) {

			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& !fileSystemManager.dirNameExists(doc1.getString("pathName"))) {

				fileSystemManager.makeDirectory(doc1.getString("pathName"));
				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "directory created");
				doc_out.append("status", true);

				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);

			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {
				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else if (fileSystemManager.dirNameExists(doc1.getString("pathName"))) {
				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname already exists");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem creating the directory");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			}
		} else if (doc1.getString("command").equals("DIRECTORY_DELETE_REQUEST")) {
			File file = new File(Configuration.getConfigurationValue("path") + FileSystems.getDefault().getSeparator()
					+ doc1.getString("pathName"));
			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& fileSystemManager.dirNameExists(doc1.getString("pathName")) && file.isDirectory()
					&& file.list().length == 0) {

				fileSystemManager.deleteDirectory(doc1.getString("pathName"));// deletDirectory

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "directory deleted");
				doc_out.append("status", true);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else if (!file.isDirectory() || file.list().length != 0) {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem deleting the directory");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else if (!fileSystemManager.dirNameExists(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname does not exist");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			}
		} else if (doc1.getString("command").equals("FILE_DELETE_REQUEST")) {

			Document fileDescriptorFileDel = new Document();
			fileDescriptorFileDel = (Document) doc1.get("fileDescriptor");

			if (fileSystemManager.isSafePathName(doc1.getString("pathName")) && fileSystemManager
					.fileNameExists(doc1.getString("pathName"), fileDescriptorFileDel.getString("md5"))) {

				fileSystemManager.deleteFile(doc1.getString("pathName"), fileDescriptorFileDel.getLong("lastModified"),
						fileDescriptorFileDel.getString("md5"));

				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "file deleted");
				doc_out.append("status", true);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else if (!fileSystemManager.fileNameExists(doc1.getString("pathName"),
					fileDescriptorFileDel.getString("md5"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname does not exist");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else {
				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem deleting the file");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			}
		} else if (doc1.getString("command").equals("FILE_CREATE_REQUEST")) {

			Document fileDescriptorFileCre = new Document();
			if (doc1.get("fileDescriptor") instanceof Document) {
				fileDescriptorFileCre = (Document) doc1.get("fileDescriptor");
			}
			long length = fileDescriptorFileCre.getLong("fileSize");
			long blocksize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
			if (blocksize >= 8192) {
				blocksize = 8192;
			}
			if (length > blocksize) {
				length = blocksize;
			}
			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& !fileSystemManager.fileNameExists(doc1.getString("pathName"))) {

				fileSystemManager.createFileLoader(doc1.getString("pathName"), fileDescriptorFileCre.getString("md5"),
						fileDescriptorFileCre.getLong("fileSize"), fileDescriptorFileCre.getLong("lastModified"));

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "file loader ready");
				doc_out.append("status", true);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
				if (fileSystemManager.checkShortcut(doc1.getString("pathName"))) {

				} else {
					Document doc_out_create = new Document();
					doc_out_create.append("command", "FILE_BYTES_REQUEST");
					doc_out_create.append("fileDescriptor", fileDescriptorFileCre);
					doc_out_create.append("pathName", doc1.getString("pathName"));
					doc_out_create.append("position", 0);
					doc_out_create.append("length", length);
					DatagramPacket send_create = new DatagramPacket(doc_out_create.toJson().getBytes(),
							doc_out_create.toJson().getBytes().length, address, ResponsePort);
					udpsocket.send(send_create);

					String exceptionRS = ResponseAddress + "," + portString + "," + "FILE_BYTES_RESPONSE";
					infolist.except.add(exceptionRS);
					checkList waitingForFileByteRes = new checkList(doc_out_create, exceptionRS, udpsocket,
							doc1.getString("pathName"), fileSystemManager);
					Thread waitForFileByteRes = new Thread(waitingForFileByteRes);
					waitForFileByteRes.start();
				}
			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);

			} else if (fileSystemManager.fileNameExists(doc1.getString("pathName"),
					fileDescriptorFileCre.getString("md5"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname already exists");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			} else {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem creating the file\"");
				doc_out.append("status", false);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			}

		} else if (doc1.getString("command").equals("FILE_BYTES_REQUEST")) {
			Document fileDescriptorFileByt = new Document();
			fileDescriptorFileByt = (Document) doc1.get("fileDescriptor");

			ByteBuffer content = fileSystemManager.readFile(fileDescriptorFileByt.getString("md5"),
					doc1.getLong("position"), doc1.getLong("length"));
			content.rewind();

			byte[] con = new byte[content.capacity()];
			int i = 0;
			while (content.hasRemaining()) {
				con[i] = content.get();
				i++;
			}

			try {

				String b_str = Base64.getEncoder().encodeToString(con);

				Document doc_out = new Document();
				doc_out.append("command", "FILE_BYTES_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileByt);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("position", doc1.getLong("position"));
				doc_out.append("length", doc1.getLong("length"));
				doc_out.append("content", b_str);
				doc_out.append("message", "sucessful read");
				doc_out.append("status", true);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);

			} catch (Exception e) {
				Document doc_out = new Document();
				doc_out.append("command", "FILE_BYTES_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileByt);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("position", doc1.getLong("position"));
				doc_out.append("length", doc1.getLong("length"));
				doc_out.append("content", "");
				doc_out.append("message", "unsucessful read");
				doc_out.append("status", true);
				DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
						doc_out.toJson().getBytes().length, address, ResponsePort);
				udpsocket.send(send);
			}

		} else if (doc1.getString("command").equals("FILE_BYTES_RESPONSE") && (doc1.getBoolean("status"))) {
			Document fileDescriptorFileByt = new Document();
			fileDescriptorFileByt = (Document) doc1.get("fileDescriptor");

			String content = doc1.getString("content");

			byte[] decoded;
			decoded = Base64.getDecoder().decode(content.getBytes());
			ByteBuffer bb = ByteBuffer.wrap(decoded);
			bb.rewind();

			if (!fileSystemManager.checkWriteComplete(doc1.getString("pathName"))) {
				fileSystemManager.writeFile(doc1.getString("pathName"), bb, doc1.getLong("position"));
				if (!fileSystemManager.checkWriteComplete(doc1.getString("pathName"))) {
					long position = doc1.getLong("position") + doc1.getLong("length");
					long length = fileDescriptorFileByt.getLong("fileSize") - position;

					if (length > Long.parseLong(Configuration.getConfigurationValue("blockSize"))) {
						length = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
					}
					if (length >= 8192) {
						length = 8192;
					}
					if (!fileSystemManager.checkWriteComplete(doc1.getString("pathName"))) {
						Document doc_out = new Document();
						doc_out.append("command", "FILE_BYTES_REQUEST");
						doc_out.append("fileDescriptor", fileDescriptorFileByt);
						doc_out.append("pathName", doc1.getString("pathName"));
						doc_out.append("position", position);
						doc_out.append("length", length);
						DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
								doc_out.toJson().getBytes().length, address, ResponsePort);
						udpsocket.send(send);
					}
				}
			}

		} else if (doc1.getString("command").equals("FILE_MODIFY_REQUEST")) {

			Document fileDescriptorFileModi = new Document();
			fileDescriptorFileModi = (Document) doc1.get("fileDescriptor");

			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& fileSystemManager.fileNameExists(doc1.getString("pathName"))) {
				try {
					fileSystemManager.modifyFileLoader(doc1.getString("pathName"),
							fileDescriptorFileModi.getString("md5"), fileDescriptorFileModi.getLong("lastModified"));
					Document doc_out = new Document();
					doc_out.append("command", "FILE_CREATE_RESPONSE");
					doc_out.append("fileDescriptor", fileDescriptorFileModi);
					doc_out.append("pathName", doc1.getString("pathName"));
					doc_out.append("message", doc1.getString("file loader ready"));
					doc_out.append("status", true);
					DatagramPacket send = new DatagramPacket(doc_out.toJson().getBytes(),
							doc_out.toJson().getBytes().length, address, ResponsePort);
					udpsocket.send(send);
					try {
						if (fileSystemManager.checkShortcut(doc1.getString("pathName"))) {
						} else {
							long length = fileDescriptorFileModi.getLong("fileSize");
							long blocksize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
							if (blocksize > 8192) {
								blocksize = 8192;
							}
							if (fileDescriptorFileModi.getLong("fileSize") <= blocksize) {
								length = fileDescriptorFileModi.getLong("fileSize");
								Document doc_out_create = new Document();
								doc_out_create.append("command", "FILE_BYTES_REQUEST");
								doc_out_create.append("fileDescriptor", fileDescriptorFileModi);
								doc_out_create.append("pathName", doc1.getString("pathName"));
								doc_out_create.append("position", 0);
								doc_out_create.append("length", length);
								DatagramPacket send_create = new DatagramPacket(doc_out_create.toJson().getBytes(),
										doc_out_create.toJson().getBytes().length, address, ResponsePort);
								udpsocket.send(send_create);
								String exceptionRS = ResponseAddress + "," + portString + "," + "FILE_BYTES_RESPONSE";
								infolist.exceptjoin(exceptionRS);
								checkList waitingForFileByteRes = new checkList(doc_out_create, exceptionRS, udpsocket,
										doc1.getString("pathName"), fileSystemManager);
								Thread waitForFileByteRes = new Thread(waitingForFileByteRes);
								waitForFileByteRes.start();

							}
						}
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	}

	class checkList implements Runnable {
		private String expection;
		DatagramSocket udpsocket1;
		Document doc;
		boolean flag = true;
		int i = 0;
		String pathname;
		FileSystemManager fileSystemManager;

		public checkList(Document doc, String expection, DatagramSocket udpsocket1) {
			this.doc = doc;
			this.udpsocket1 = udpsocket1;
			this.expection = expection;
		}

		public checkList(Document doc, String expection, DatagramSocket udpsocket1, String pathname,
				FileSystemManager fileSystemManager) {
			this.doc = doc;
			this.udpsocket1 = udpsocket1;
			this.expection = expection;
			this.pathname = pathname;
			this.fileSystemManager = fileSystemManager;
		}

		@Override
		public void run() {
			while (flag && i < Integer.parseInt(Configuration.getConfigurationValue("retry"))) {
				try {
					Thread.sleep(10 * 10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (String i : infolist.exceptlook()) {
					if (i.equals(expection)) {
						flag = false;
						byte[] buffer = doc.toJson().getBytes();
						DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length);
						InetAddress address = null;
						try {
							address = InetAddress.getByName(expection.split(",")[0]);
						} catch (UnknownHostException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						sendPacket.setAddress(address);
						sendPacket.setPort(Integer.parseInt(expection.split(",")[1]));
						try {
							udpsocket1.send(sendPacket);
							break;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if (flag == true) {
					break;
				}
				i = i + 1;
			}
			int count = 0;
			while (count < infolist.exceptlook().size()) {
				if (infolist.exceptlook().get(count).equals(expection)) {
					infolist.exceptremove(count);
					break;
				}
				count++;
			}
			if (pathname != null) {
				try {
					fileSystemManager.cancelFileLoader(pathname);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void event_opeartion(Document doc1) throws IOException, NoSuchAlgorithmException {

		if (doc1.getString("command").equals("DIRECTORY_CREATE_REQUEST")) {

			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& !fileSystemManager.dirNameExists(doc1.getString("pathName"))) {

				fileSystemManager.makeDirectory(doc1.getString("pathName"));
				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "directory created");
				doc_out.append("status", true);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {
				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else if (fileSystemManager.dirNameExists(doc1.getString("pathName"))) {
				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname already exists");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_CREATE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem creating the directory");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();
			}
		} else if (doc1.getString("command").equals("DIRECTORY_DELETE_REQUEST")) {
			File file = new File(Configuration.getConfigurationValue("path") + FileSystems.getDefault().getSeparator()
					+ doc1.getString("pathName"));
			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& fileSystemManager.dirNameExists(doc1.getString("pathName")) && file.isDirectory()
					&& file.list().length == 0) {

				fileSystemManager.deleteDirectory(doc1.getString("pathName"));// deletDirectory

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "directory deleted");
				doc_out.append("status", true);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else if (!file.isDirectory() || file.list().length != 0) {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem deleting the directory");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else if (!fileSystemManager.dirNameExists(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "DIRECTORY_DELETE_RESPONSE");
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname does not exist");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			}
		} else if (doc1.getString("command").equals("FILE_DELETE_REQUEST")) {

			Document fileDescriptorFileDel = new Document();
			fileDescriptorFileDel = (Document) doc1.get("fileDescriptor");

			if (fileSystemManager.isSafePathName(doc1.getString("pathName")) && fileSystemManager
					.fileNameExists(doc1.getString("pathName"), fileDescriptorFileDel.getString("md5"))) {

				fileSystemManager.deleteFile(doc1.getString("pathName"), fileDescriptorFileDel.getLong("lastModified"),
						fileDescriptorFileDel.getString("md5"));

				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "file deleted");
				doc_out.append("status", true);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();
			} else if (!fileSystemManager.fileNameExists(doc1.getString("pathName"),
					fileDescriptorFileDel.getString("md5"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname does not exist");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();
			} else {
				Document doc_out = new Document();
				doc_out.append("command", "FILE_DELETE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileDel);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem deleting the file");
				doc_out.append("status", false);
				out.write(doc_out.toJson());
				out.newLine();
				out.flush();
			}
		} else if (doc1.getString("command").equals("FILE_CREATE_REQUEST")) {

			Document fileDescriptorFileCre = new Document();
			if (doc1.get("fileDescriptor") instanceof Document) {
				fileDescriptorFileCre = (Document) doc1.get("fileDescriptor");
			}

			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& !fileSystemManager.fileNameExists(doc1.getString("pathName"))) {

				fileSystemManager.createFileLoader(doc1.getString("pathName"), fileDescriptorFileCre.getString("md5"),
						fileDescriptorFileCre.getLong("fileSize"), fileDescriptorFileCre.getLong("lastModified"));

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "file loader ready");
				doc_out.append("status", true);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();

				if (fileSystemManager.checkShortcut(doc1.getString("pathName"))) {

				} else {
					long length = fileDescriptorFileCre.getLong("fileSize");
					if (fileDescriptorFileCre.getLong("fileSize") > Long
							.parseLong(Configuration.getConfigurationValue("blockSize"))) {
						length = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
					}
					Document doc_out_create = new Document();
					doc_out_create.append("command", "FILE_BYTES_REQUEST");
					doc_out_create.append("fileDescriptor", fileDescriptorFileCre);
					doc_out_create.append("pathName", doc1.getString("pathName"));
					doc_out_create.append("position", 0);
					doc_out_create.append("length", length);
					out.write(doc_out_create.toJson() + "\r\n");
					out.flush();

				}

			} else if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& fileSystemManager.fileNameExists(doc1.getString("pathName")) && !fileSystemManager
							.fileNameExists(doc1.getString("pathName"), fileDescriptorFileCre.getString("md5"))) {

				if (fileSystemManager.modifyFileLoader(doc1.getString("pathName"),
						fileDescriptorFileCre.getString("md5"), fileDescriptorFileCre.getLong("lastModified"))) {
					long length = fileDescriptorFileCre.getLong("fileSize");
					if (fileDescriptorFileCre.getLong("fileSize") > Long
							.parseLong(Configuration.getConfigurationValue("blockSize"))) {
						length = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
					}
					Document doc_out_create = new Document();
					doc_out_create.append("command", "FILE_BYTES_REQUEST");
					doc_out_create.append("fileDescriptor", fileDescriptorFileCre);
					doc_out_create.append("pathName", doc1.getString("pathName"));
					doc_out_create.append("position", 0);
					doc_out_create.append("length", length);
					out.write(doc_out_create.toJson() + "\r\n");
					out.flush();
				}

			} else if (!fileSystemManager.isSafePathName(doc1.getString("pathName"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "unsafe pathname given");
				doc_out.append("status", false);
				out.write(doc_out.toJson());
				out.newLine();
				out.flush();

			} else if (fileSystemManager.fileNameExists(doc1.getString("pathName"),
					fileDescriptorFileCre.getString("md5"))) {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "pathname already exists");
				doc_out.append("status", false);
				out.write(doc_out.toJson());
				out.newLine();
				out.flush();

			} else {

				Document doc_out = new Document();
				doc_out.append("command", "FILE_CREATE_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileCre);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("message", "there was a problem creating the file");
				doc_out.append("status", false);
				out.write(doc_out.toJson());
				out.newLine();
				out.flush();

			}

		} else if (doc1.getString("command").equals("FILE_BYTES_REQUEST")) {

			Document fileDescriptorFileByt = new Document();
			fileDescriptorFileByt = (Document) doc1.get("fileDescriptor");
			// (doc1.toJson());

			ByteBuffer content = fileSystemManager.readFile(fileDescriptorFileByt.getString("md5"),
					doc1.getLong("position"), doc1.getLong("length"));
			content.rewind();

			byte[] con = new byte[content.capacity()];
			int i = 0;
			while (content.hasRemaining()) {
				con[i] = content.get();
				i++;
			}

			try {

				String b_str = Base64.getEncoder().encodeToString(con);

				Document doc_out = new Document();
				doc_out.append("command", "FILE_BYTES_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileByt);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("position", doc1.getLong("position"));
				doc_out.append("length", doc1.getLong("length"));
				doc_out.append("content", b_str);
				doc_out.append("message", "sucessful read");
				doc_out.append("status", true);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();
			} catch (Exception e) {
				Document doc_out = new Document();
				doc_out.append("command", "FILE_BYTES_RESPONSE");
				doc_out.append("fileDescriptor", fileDescriptorFileByt);
				doc_out.append("pathName", doc1.getString("pathName"));
				doc_out.append("position", doc1.getLong("position"));
				doc_out.append("length", doc1.getLong("length"));
				doc_out.append("content", "");
				doc_out.append("message", "unsucessful read");
				doc_out.append("status", false);
				out.write(doc_out.toJson() + "\r\n");
				out.flush();
			}

		} else if (doc1.getString("command").equals("FILE_BYTES_RESPONSE") && (doc1.getBoolean("status"))) {

			Document fileDescriptorFileByt = new Document();
			fileDescriptorFileByt = (Document) doc1.get("fileDescriptor");

			String content = doc1.getString("content");

			byte[] decoded;
			decoded = Base64.getDecoder().decode(content.getBytes());
			ByteBuffer bb = ByteBuffer.wrap(decoded);
			bb.rewind();

			if (!fileSystemManager.checkWriteComplete(doc1.getString("pathName"))) {

				fileSystemManager.writeFile(doc1.getString("pathName"), bb, doc1.getLong("position"));
				long position = doc1.getLong("position") + doc1.getLong("length");
				long length = fileDescriptorFileByt.getLong("fileSize") - position;

				if (length > Long.parseLong(Configuration.getConfigurationValue("blockSize"))) {
					length = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
				}
				if (!fileSystemManager.checkWriteComplete(doc1.getString("pathName"))) {
					Document doc_out = new Document();
					doc_out.append("command", "FILE_BYTES_REQUEST");
					doc_out.append("fileDescriptor", fileDescriptorFileByt);
					doc_out.append("pathName", doc1.getString("pathName"));
					doc_out.append("position", position);
					doc_out.append("length", length);
					out.write(doc_out.toJson() + "\r\n");
					out.flush();
				}
			}
		} else if (doc1.getString("command").equals("FILE_MODIFY_REQUEST")) {

			Document fileDescriptorFileModi = new Document();
			fileDescriptorFileModi = (Document) doc1.get("fileDescriptor");

			if (fileSystemManager.isSafePathName(doc1.getString("pathName"))
					&& fileSystemManager.fileNameExists(doc1.getString("pathName"))) {
				try {
					fileSystemManager.modifyFileLoader(doc1.getString("pathName"),
							fileDescriptorFileModi.getString("md5"), fileDescriptorFileModi.getLong("lastModified"));
					Document doc_out = new Document();
					doc_out.append("command", "FILE_CREATE_RESPONSE");
					doc_out.append("fileDescriptor", fileDescriptorFileModi);
					doc_out.append("pathName", doc1.getString("pathName"));
					doc_out.append("message", doc1.getString("file loader ready"));
					doc_out.append("status", true);
					out.write(doc_out.toJson() + "\r\n");
					out.flush();

					try {
						if (fileSystemManager.checkShortcut(doc1.getString("pathName"))) {
						} else {
							long length = fileDescriptorFileModi.getLong("fileSize");
							if (fileDescriptorFileModi.getLong("fileSize") > Long
									.parseLong(Configuration.getConfigurationValue("blockSize"))) {
								length = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
							}
							Document doc_out_create = new Document();
							doc_out_create.append("command", "FILE_BYTES_REQUEST");
							doc_out_create.append("fileDescriptor", fileDescriptorFileModi);
							doc_out_create.append("pathName", doc1.getString("pathName"));
							doc_out_create.append("position", 0);
							doc_out_create.append("length", length);
							out.write(doc_out_create.toJson() + "\r\n");
							out.flush();

						}
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
	}
}
