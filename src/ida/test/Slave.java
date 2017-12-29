package ida.test;

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
import ida.ipl.messages.Message;
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
		this.print("[CONSTRUCTOR]", this.state.toString());
		
		try {
			this.print("[CONSTRUCTOR]", "Connecting to master");
			this.connectToMaster();
			this.print("[CONSTRUCTOR]", "Connected to master");
			this.print("[CONSTRUCTOR]", "Requesting question");
			this.requestQuestion();
			this.print("[CONSTRUCTOR]", "Question requested");
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.print("[CONSTRUCTOR]", "Start waiting");
		while(this.state != State.DONE) {
			this.print("[CONSTRUCTOR]", "Waiting...");
			synchronized(this.waiting) {
				try {
					this.waiting.wait();
				} catch (InterruptedException e) {
				}
				this.print("[CONSTRUCTOR]", "Wait ended");
			}
		}
		try {
			this.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void finish() throws IOException {
		this.sendPort.close();
		this.receivePort.close();
	}

	private void connectToMaster() throws IOException {
		this.print("[CONNECT_TO_MASTER]", "Start");
		this.receivePort = this.ibis.createReceivePort(Constants.RECEIVE_PORT_TYPE, Constants.SLAVE_IDENTIFIER, this);
		this.receivePort.enableConnections();
		this.receivePort.enableMessageUpcalls();
		
		this.print("[CONNECT_TO_MASTER]", "Started receiving");
		
		this.sendPort = this.ibis.createSendPort(Constants.SEND_PORT_TYPE);
		this.sendPort.connect(this.master, Constants.MASTER_IDENTIFIER);
		this.print("[CONNECT_TO_MASTER]", "Done");
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		if(this.state == State.DONE) {
			this.print("[RECEIVED_MESSAGE]", "Script done, ignoring message");
			return;
		}
		this.state = State.RUNNING;
		Message m = (Message) readMessage.readObject();
		readMessage.finish();
		this.print("[RECEIVED_MESSAGE]", "Got message", m.getMessageType());
		switch (m.getMessageType()) {
		case QUESTION:
//			this.print("[RECEIVED_MESSAGE]", m.getBoard());
//			this.print("[RECEIVED_MESSAGE]", "got board bound: " + m.getBoard().bound());
			int solution = this.solve(m.getBoard());
			this.sendSolution(new Solution(m.getSearchBound(), solution));
			break;
		case FINISHED_CALCULATION:
			this.print("[RECEIVED_MESSAGE]", "Master is finished, closing");
			this.state = State.DONE;
			synchronized (this.waiting) {
				this.waiting.notifyAll();
			}
			break;
		default:
			break;
		}
	}

	private int solve(Board b) {
		int solution = 0;
		if(this.cache != null) {
			solution = Slave.solutions(b, this.cache);
		}
		else {
			solution = Slave.solutions(b);
		}
		return solution;
	}

	private void sendSolution(Solution solution) throws IOException {
		this.sendMessage(new Message(MessageType.SOLUTION).setSolution(solution));
	}

	private void requestQuestion() throws IOException {
		this.sendMessage(new Message(MessageType.REQUEST_QUESTION));
	}
	
	private void sendMessage(Object...objects) throws IOException {
//		this.print("[SEND MESSAGE]", objects);
		WriteMessage wm = this.sendPort.newMessage();
//		this.print("[SEND MESSAGE]", "New message opened");
		for(Object obj : objects) {
			wm.writeObject(obj);
		}
		if(this.state == State.DONE)
			return;
		wm.finish();
//		this.print("[SEND MESSAGE]", "Message sent");
	}
}
