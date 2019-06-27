package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
import javax.crypto.spec.SecretKeySpec;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


import unimelb.bitbox.util.Document;
public class Client {
	public static String command;
	public static String serverinfo;
	public static String peerinfo;
	public static String publickey;
	public static String privatekey;
	public static String identity;
	static SecretKey sharedsecretkey2;
	public class CmdLineArgs {

		@Option(required = true, name = "-c")
		private String command;
		@Option(required = true, name = "-s")
		private String serverinfo;
		@Option(required = false, name = "-p")
		private String peerinfo;
		@Option(required = false, name = "-i")
		private String identity;
		public String getcommand() {
			return command;
		}

		public String getserverinfo() {
			return serverinfo;
		}
		public String getpeerinfo() {
			return peerinfo;
		}
		public String getpeeridentity() {
			return identity;
		}
		
	}
	public void getcommandlineinfo(String[] args) {
		CmdLineArgs argsBean = new CmdLineArgs();
		
		//Parser provided by args4j
		CmdLineParser parser = new CmdLineParser(argsBean);
		try {
			
			//Parse the arguments
			parser.parseArgument(args);
			command = argsBean.getcommand();
			serverinfo = argsBean.getserverinfo();
			peerinfo = argsBean.getpeerinfo();
			identity = argsBean.getpeeridentity();

		} catch (CmdLineException e) {
			System.out.println(e);
		}
	}
	private static void getTemplateContent(String path,String type) throws Exception{
	    File file = new File(path);
	    FileInputStream inputStream = new FileInputStream(file);
	    int length = inputStream.available();
	    byte bytes[] = new byte[length];
	    inputStream.read(bytes);
	    inputStream.close();
	    if(type.equals("private")) {
	    	privatekey =new String(bytes, StandardCharsets.UTF_8);
	    }
	    else {
	    	publickey =new String(bytes, StandardCharsets.UTF_8);
	    }
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Client client = new Client();
		
		client.getcommandlineinfo(args);
		getTemplateContent("bitboxclient_rsa","private");
		getTemplateContent("bitboxclient_pub_rsa","public");
		System.out.println(command);
		System.out.println(serverinfo);
		System.out.println(peerinfo);
		System.out.println(privatekey);
		System.out.println(publickey);
		String[] serverPeers = serverinfo.split(":");
		String peerIp = serverPeers[0];
		Integer peerPort = Integer.parseInt(serverPeers[1]);
		System.out.println(peerIp);
		System.out.println(peerPort);
		Socket socket = new Socket(peerIp, peerPort);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		//send connect request
		Document request = new Document();
		request.append("command", "AUTH_REQUEST");
		request.append("identity", identity);
		out.write(request.toJson()+"\n");
		out.flush();
		//listen to response
		System.out.println(request.toJson()+"        out");
		String receive = null;
		try {
			socket.setSoTimeout(200000);
			receive = in.readLine();
			System.out.println(receive+"        in");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		Document response = Document.parse(receive);
		if((response.getString("command").equals("AUTH_RESPONSE"))&&(response.getBoolean("status"))) {
			Document doc = new Document();
			String sharedkey = response.getString("AES128");
			Document secretdoc = new Document();
			if(command.equals("list_peers")) {
				secretdoc.append("command", "LIST_PEERS_REQUEST");
				//encrept

			}
			else if(command.equals("connect_peer")) {
				//todo
				String[] conPeers = peerinfo.split(":");
				String conpeerIp = conPeers[0];
				Integer conpeerPort = Integer.parseInt(conPeers[1]);
				secretdoc.append("command", "CONNECT_PEER_REQUEST");
				System.out.println(conpeerIp);
				System.out.println(conpeerPort);
				secretdoc.append("host", conpeerIp);
				secretdoc.append("port", conpeerPort);
				
				//encrept
				
				
			}
			else if(command.equals("disconnect_peer")) {
				//todo
				String[] conPeers = peerinfo.split(":");
				String conpeerIp = conPeers[0];
				Integer conpeerPort = Integer.parseInt(conPeers[1]);
				secretdoc.append("command", "DISCONNECT_PEER_REQUEST");
				secretdoc.append("host", conpeerIp);
				secretdoc.append("port", conpeerPort);
				//encrept
			}
			else {
				System.out.println("incorrect command");
				System.exit(0);
			}
			String secretcotent = secretdoc.toJson()+"\n";
			System.out.println(secretcotent+"            before encrypt");
			//set up own private key
			byte[] keyBytes;
			keyBytes = Base64.getDecoder().decode(privatekey.getBytes());
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privatekey = keyFactory.generatePrivate(keySpec);
			//get encrypted shared private key
			String sharedprivatekey = response.getString("AES128");
			//decrypt shared privete key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privatekey);
			byte[] encodeReceive = Base64.getDecoder().decode(sharedprivatekey.getBytes());
			byte[] decodeReceive = cipher.doFinal(encodeReceive);
			byte[] sharedprivatekeybyte = Base64.getDecoder().decode(decodeReceive);
			String sharedprivatekey1 = Base64.getEncoder().encodeToString(sharedprivatekeybyte);
			//set up shared private key
			byte[] keyBytes1;
			keyBytes1 = Base64.getDecoder().decode(sharedprivatekey1.getBytes());
			sharedsecretkey2 = new SecretKeySpec(keyBytes1,0,keyBytes1.length,"AES");
			
			
			
			//encrypt
			System.out.println(secretcotent+"                 content");
			Cipher cipher1 = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher1.init(Cipher.ENCRYPT_MODE, sharedsecretkey2);
			byte[] enBytes = cipher1.doFinal(secretcotent.getBytes());
			String encodeSent = Base64.getEncoder().encodeToString(enBytes);
			System.out.println(encodeSent);
			doc.append("payload", encodeSent);
			out.write(doc.toJson()+"\n");
			out.flush();
			System.out.println(doc.toJson()+"        out");
		}
		else {
			socket.close();
		}
		try {
		socket.setSoTimeout(200000);
		receive = in.readLine();
		Document sent = new Document();
		Document doc = Document.parse(receive);
		String commStr = doc.getString("payload");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		System.out.println(commStr + "                 content");
		cipher.init(Cipher.DECRYPT_MODE, sharedsecretkey2);
		byte[] encodeReceive = Base64.getDecoder().decode(commStr.getBytes());
		byte[] decodeReceive = cipher.doFinal(encodeReceive);
		String finalReceive = new String(decodeReceive);
		System.out.println(finalReceive+"        in");
		socket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
