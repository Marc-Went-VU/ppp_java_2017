package ida.test;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ida.ipl.extra.Arguments;
import ida.ipl.extra.Board;
import ida.ipl.extra.BoardCache;
import ida.ipl.extra.consts.Constants;
import ida.ipl.extra.consts.State;
import ida.ipl.lists.QuestionList;
import ida.ipl.lists.SlaveConnection;
import ida.ipl.lists.SlaveManager;
import ida.ipl.lists.Solution;
import ida.ipl.lists.SolutionsList;
import ida.ipl.messages.Message;
import ida.ipl.messages.MessageType;

public class Master extends Shared implements ReceivePortConnectUpcall{
	private SlaveManager slaves;
	private QuestionList questions;
	private SolutionsList solutions;
	private State state;
	
	final private Object messageHolder = new Object();
	final private Object masterHolder = new Object();
	private ReceivePort receivePort;
	private int bound_area;
	
	Master(Ibis ibis, Arguments arg) {
		super(ibis, arg);
		this.state = State.INITIALIZING;
		this.print("[CONSTRUCTOR]", this.state.toString());
		this.slaves = new SlaveManager();
		this.solutions = new SolutionsList();
		this.questions = new QuestionList();
		this.print("[CONSTRUCTOR]", "Start listening");
		this.startListening();
		this.print("[CONSTRUCTOR]", "Start parsing file");
		this.parseFile(arg);
		this.print("[CONSTRUCTOR]", "Done executing");
	}
	
	private void startListening() {
		this.print("[START_LISTENING]", "Start listening");
		try {
			this.receivePort = this.ibis.createReceivePort(Constants.RECEIVE_PORT_TYPE, Constants.MASTER_IDENTIFIER, this, this, new Properties());
			this.receivePort.enableConnections();
			this.receivePort.enableMessageUpcalls();
			this.print("[START_LISTENING]", "Listening");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parseFile(Arguments args) {
		Board initialBoard = null;

		if (args.getFileName() == null) {
			initialBoard = new Board(args.getLength());
		} else {
			try {
				initialBoard = new Board(args.getFileName());
			} catch (Exception e) {
				System.err
						.println("could not initialize board from file: " + e);
				System.exit(1);
			}
		}
//		this.questions.add(initialBoard);
		System.out.println("Running IDA*, initial board:");
		System.out.println(initialBoard);
		long start = System.currentTimeMillis();
		this.print("[INITIAL BOARD]", initialBoard.toString());

		this.print("[INITIAL BOARD]", "Start solving");
		solve(initialBoard, args.useCache());
		this.print("[INITIAL BOARD]", "Done solving");
		
		long end = System.currentTimeMillis();

		// NOTE: this is printed to standard error! The rest of the output
		// is
		// constant for each set of parameters. Printing this to standard
		// error
		// makes the output of standard out comparable with "diff"
		System.err.println("ida took " + (end - start) + " milliseconds");
	}
	
	private void solve(Board board, boolean useCache) {
		BoardCache cache = null;
		if (this.arg.useCache()) {
			cache = new BoardCache();
		}
		this.bound_area = board.distance();

		System.out.print("Try bound ");
		System.out.flush();
		
		do {
			this.state = State.INITIALIZING;
			board.setBound(this.bound_area);
			this.print("[SOLVE]", "Bound: " + this.bound_area);
			System.out.print(this.bound_area + " ");
			System.out.flush();

//			if (this.arg.useCache()) {
//				this.questions.addAll(children(Constants.MAX_MASTER_STEPS, board, cache, 0));
//			} else {
				this.questions.addAll(children(Constants.MAX_MASTER_STEPS, board, 0));
//			}
			this.state = State.RUNNING;
			synchronized (this.messageHolder) {
				this.messageHolder.notifyAll();
 			}

			this.print("[SOLVE]", "Start waiting");
			while(!this.isDone()) {
				synchronized(this.masterHolder) {
					try {
						this.masterHolder.wait();
					} catch (InterruptedException e) {
					}
				}
			}
			this.print("[SOLVE]", "Done waiting");
			bound_area += 2;
		} while (!this.solutions.foundSolution());
		this.print("[SOLVED]");
		this.state = State.DONE;
		Solution s = this.solutions.getSmallestSolution();
		try {
			this.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		this.print("[!SOLUTION!]", s);
		System.out.println("\nresult is " + s.getSolution() + " solutions of " + s.getBound() + " steps");
	}
	
	private void finish() throws IOException {
		if(this.state == State.FINISHED) {
			return;
		}
		this.print("[SEND_DONE]", "State is finished, don't request new data from slave");
		this.sendToAll(MessageType.FINISHED_CALCULATION);
		this.state = State.FINISHED;
		this.print("[SEND_FINISHED]");
		this.slaves.finish();
		this.print("[SLAVES_FINISHED]");
		this.receivePort.close();
		this.print("[RECEIVEPORT_CLOSED]");
		
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		// Read the sendport toward the slave
		IbisIdentifier identifier = readMessage.origin().ibisIdentifier();
		
		// Read the request type from the slave.
		Message message = (Message) readMessage.readObject();
		readMessage.finish();
		switch (message.getMessageType()) {
		case SOLUTION:
			this.slaves.slaveDoneWorking(identifier);
			this.print("[UPCALL]", message.getMessageType(), "from:", identifier.name(), message.getSolution());
			this.print("[UPCALL]", "Working slaves:", this.slaves.workingSlaves());
			this.parseSolution(message.getSolution());
			this.sendQuestion(identifier);
			break;
		case REQUEST_QUESTION:
			this.print("[UPCALL]", message.getMessageType(), "from:", identifier.name());
			this.sendQuestion(identifier);
			break;
		default:
			break;
		}
	}
	private void parseSolution(Solution s) {
		if(this.solutions.setSolution(s))
			this.questions.purgeAboveBound(s.getBound());
		
		if(this.isDone()) {
			synchronized (this.masterHolder) {
				this.masterHolder.notifyAll();
			}
		}
	}
	
	private boolean isDone() {
		return this.questions.isEmpty() && !this.slaves.slavesWorking();
	}

	private void sendQuestion(IbisIdentifier identifier) throws IOException {
		if(this.state == State.FINISHED) {
			return;
		}
		else if (this.state == State.DONE) {
			this.finish();
			return;
		}
		if(this.state == State.INITIALIZING) {
			this.slaveWait();
		}
		try {
			this.print("[SEND_QUESTION]", "Getting board from open questions.", this.questions.size(), " boards in backlog.");
			Board b = this.questions.take();
			this.slaves.slaveStartsWorking(identifier);
			this.print("[SEND_QUESTION]", "Working slaves:", this.slaves.workingSlaves());
//			this.print("[SEND_QUESTION]", "Sending board to slave");
			this.sendMessage(identifier, new Message(MessageType.QUESTION).setBoard(b, this.bound_area));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void slaveWait() {
		this.print("[SEND_QUESTION]", "State is still initializing, waiting...");
		synchronized (this.messageHolder) {
			try {
				this.messageHolder.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	private void sendToAll(MessageType mt, Object... objects) throws IOException {
		for(IbisIdentifier slaveIdentifier : this.slaves.getIdentifiers()) {
			this.sendMessage(slaveIdentifier, new Message(mt));
		}
	}
	
	private void sendMessage(IbisIdentifier identifier, Message m) throws IOException {
		if(this.state == State.FINISHED)
			return;
		SlaveConnection sp = this.slaves.getConnection(identifier);
		if(sp == null)
			return;
//		this.print("[MESSAGE]", "Sending to: "+ identifier.name(), m.getMessageType());
		sp.startNewMessage();
		sp.writeObject(m);
		sp.finish();
	}

	@Override
	public boolean gotConnection(ReceivePort receiver, SendPortIdentifier applicant) {
		try {
			IbisIdentifier slaveIdentifier = applicant.ibisIdentifier();
			this.print("[SLAVE CONNECTED]", slaveIdentifier.name());
			SendPort slaveSendPort = this.ibis.createSendPort(Constants.SEND_PORT_TYPE);
			slaveSendPort.connect(slaveIdentifier, Constants.SLAVE_IDENTIFIER);
			this.slaves.addSlave(slaveIdentifier, new SlaveConnection(slaveSendPort));
//			this.sendQuestion(slaveIdentifier);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	public void lostConnection(ReceivePort receiver, SendPortIdentifier origin, Throwable cause) {
		IbisIdentifier slaveIdentifier = origin.ibisIdentifier();
		this.slaves.closeIdentifier(slaveIdentifier);
	}
}