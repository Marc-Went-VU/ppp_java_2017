package ida.ipl;

import java.io.IOException;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ida.ipl.extra.Arguments;
import ida.ipl.extra.Board;
import ida.ipl.extra.BoardCache;
import ida.ipl.extra.consts.Constants;
import ida.ipl.extra.consts.State;
import ida.ipl.lists.Solution;
import ida.ipl.messages.MessageType;

public class Slave extends Shared {

	private IbisIdentifier master;
	private SendPort sendPort;
	private ReceivePort receivePort;
	private BoardCache cache;
	
	private State state;
	final private Object waiting = new Object(); 

	Slave(Ibis ibis, Arguments arg) {
		super(ibis, arg);
		this.master = arg.getMasterIdentifier();
		this.state = State.INITIALIZING;
		if(arg.useCache()) {
			this.cache = new BoardCache();
		}
		try {
			this.connectToMaster();
			this.requestQuestion();
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(this.state != State.DONE) {
			synchronized(this.waiting) {
				try {
					this.waiting.wait(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	private void connectToMaster() throws IOException {
		receivePort = this.ibis.createReceivePort(Constants.RECEIVE_PORT_TYPE, Constants.SLAVE_IDENTIFIER, this);
		receivePort.enableConnections();
		receivePort.enableMessageUpcalls();
		
		this.sendPort = ibis.createSendPort(Constants.SEND_PORT_TYPE);
		this.sendPort.connect(master, Constants.MASTER_IDENTIFIER);		
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		if(this.state == State.DONE) {
			return;
		}
		this.state = State.RUNNING;
		MessageType messageType = (MessageType) readMessage.readObject();
		System.out.println("Received message: " + messageType);
		switch (messageType) {
		case QUESTION:
			Board b = (Board) readMessage.readObject();
			solve(b);
			break;
		case FINISHED_CALCULATION:
			this.state = State.DONE;
			this.sendPort.close();
			this.receivePort.close();
			synchronized (this.waiting) {
				this.waiting.notifyAll();
			}
			break;
		default:
			break;
		}
	}

	private void solve(Board b) {
		int solution = 0;
		if(this.cache != null) {
			solution = Slave.solutions(b, this.cache);
		}
		else {
			solution = Slave.solutions(b);
		}
		try {
			this.sendSolution(new Solution(b.bound(), solution));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendSolution(Solution solution) throws IOException {
		this.sendMessage(MessageType.SOLUTION, solution);
	}

	private void requestQuestion() throws IOException {
		this.sendMessage(MessageType.REQUEST_QUESTION);
	}
	
	private void sendMessage(Object...objects) throws IOException {
		WriteMessage wm = this.sendPort.newMessage();
		for(Object obj : objects) {
			wm.writeObject(obj);
		}
		wm.finish();
	}
}
