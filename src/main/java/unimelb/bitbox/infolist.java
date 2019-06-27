package unimelb.bitbox;

import java.util.ArrayList;

public class infolist {
	public ArrayList<String> peerlist=new ArrayList<String>();
	public ArrayList<String> except=new ArrayList<String>();
	public synchronized void peerjoin(String str) {
		peerlist.add(str);
	}
	public synchronized ArrayList<String> peerlook() {
		return peerlist;
	}
	public synchronized void peerremove(int i) {
		peerlist.remove(i);
	}
	public synchronized void exceptjoin(String str) {
		except.add(str);
	}
	public synchronized ArrayList<String> exceptlook() {
		return except;
	}
	public synchronized void exceptremove(int i) {
		except.remove(i);
	}
}
