package ida.ipl.lists;

import java.io.IOException;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class SlaveConnection {

	private SendPort sp;
	private WriteMessage wm;
	public SlaveConnection(SendPort sp){
		this.sp = sp;
		this.wm = null;
	}
	
	public void startNewMessage() throws IOException {
		if(this.wm != null) {
			this.finish();
		}
		this.wm = this.sp.newMessage();
	}
	public void writeObject(Object o) throws IOException {
		if(o == null) { return; }
		synchronized(this.wm) {
			this.wm.writeObject(o);
		}
	}
	public void finish() throws IOException {
		synchronized (this.wm) {
			if(this.wm == null) return;
			this.wm.finish();
			this.wm = null;			
		}
	}

	public void close() throws IOException {
		this.finish();
		this.sp.close();
	}	
}
