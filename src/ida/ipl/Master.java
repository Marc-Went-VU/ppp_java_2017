package ida.ipl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ida.ipl.extra.Arguments;
import ida.ipl.extra.Board;
import ida.ipl.extra.BoardCache;
import ida.ipl.extra.consts.Constants;
import ida.ipl.extra.consts.State;
import ida.ipl.lists.QuestionList;
import ida.ipl.lists.Solution;
import ida.ipl.lists.SolutionsList;
import ida.ipl.messages.MessageType;

public class Master extends Shared implements ReceivePortConnectUpcall{
	private HashMap<IbisIdentifier, SendPort> slaves;
	private QuestionList questions;
	private SolutionsList solutions;
	private State state;
	
	final private Object messageHolder = new Object();
	final private Object masterHolder = new Object();
	private ReceivePort receivePort;
	
	Master(Ibis ibis, Arguments arg) {
		super(ibis, arg);
		this.state = State.INITIALIZING;
		this.slaves = new HashMap<IbisIdentifier, SendPort>();
		this.startListening();
		this.solutions = new SolutionsList();
		this.questions = new QuestionList();
		this.parseFile(arg);
	}
	
	private void startListening() {
		
		try {
			this.receivePort = this.ibis.createReceivePort(Constants.RECEIVE_PORT_TYPE, Constants.MASTER_IDENTIFIER, this, this, new Properties());
			this.receivePort.enableConnections();
			this.receivePort.enableMessageUpcalls();
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

//		long start = System.currentTimeMillis();
		solve(initialBoard, args.useCache());
		
//		long end = System.currentTimeMillis();

		// NOTE: this is printed to standard error! The rest of the output
		// is
		// constant for each set of parameters. Printing this to standard
		// error
		// makes the output of standard out comparable with "diff"
//		System.err.println("ida took " + (end - start) + " milliseconds");
	}
	
	private void solve(Board board, boolean useCache) {
		BoardCache cache = null;
		if (useCache) {
			cache = new BoardCache();
		}
		int bound = board.distance();

		System.out.print("Try bound ");
		System.out.flush();

		do {
			this.state = State.INITIALIZING;
			board.setBound(bound);

			System.out.print(bound + " ");
			System.out.flush();

			if (useCache) {
				this.questions.addAll(children(Constants.MAX_MASTER_STEPS, board, 0));
			} else {
				this.questions.addAll(children(Constants.MAX_MASTER_STEPS, board, cache, 0));
			}
			this.state = State.RUNNING;
			synchronized (messageHolder) {
				this.messageHolder.notifyAll();
 			}

			while(!this.questions.isEmpty()) {
				synchronized(this.masterHolder) {
					try {
						this.masterHolder.wait();
					} catch (InterruptedException e) {
					}
				}
			}
			bound += 2;
		} while (!this.solutions.foundSolution());
		this.state = State.DONE;
		Solution s = this.solutions.getSmallestSolution();
		System.out.println("\nresult is " + s.getSolution() + " solutions of "
				+ s.getBound() + " steps");

	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		// Read the sendport toward the slave
		IbisIdentifier identifier = readMessage.origin().ibisIdentifier();
		
		// Read the request type from the slave.
		MessageType messageType = (MessageType) readMessage.readObject();
		System.out.println("Receieved message: " + messageType + " from " + readMessage.origin().name());
		
		switch (messageType) {
		case SOLUTION:
			Solution sm = (Solution) readMessage.readObject();
			this.parseSolution(sm);
			this.sendQuestion(identifier);
			break;
		case REQUEST_QUESTION:
			this.sendQuestion(identifier);
		default:
			break;
		}
	}
	private void parseSolution(Solution s) {
		if(this.solutions.setSolution(s))
			this.questions.purgeAboveBound(s.getBound());
		this.masterHolder.notifyAll();
	}

	private void sendQuestion(IbisIdentifier identifier) throws IOException {
		if(this.state == State.INITIALIZING) {
			System.out.println("State is still initializing, waiting...");
			synchronized (this.messageHolder) {
				try {
					this.messageHolder.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		else if (this.state == State.DONE) {
			System.out.println("State is finished, don't request new data from slave");
			this.sendToAll(MessageType.FINISHED_CALCULATION);
			return;
		}
		try {
			System.out.println("Getting board from open questions");
			Board b = this.questions.take();
			System.out.println("Sending board to slave");
			this.sendMessage(identifier, MessageType.QUESTION, b);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void sendToAll(MessageType mt, Object... objects) throws IOException {
		for(IbisIdentifier slaveIdentifier : this.slaves.keySet()) {
			this.sendMessage(slaveIdentifier, mt, objects);
		}
	}
	
	private void sendMessage(IbisIdentifier identifier, Object...objects) throws IOException {
		SendPort sp = this.slaves.get(identifier);
		System.out.println("Sending to: "+ sp.name());
		WriteMessage wm = sp.newMessage();
		System.out.println("[MESSAGE] Writing message");
		for(Object obj : objects) {
			wm.writeObject(obj);
		}
		wm.finish();
	}

	@Override
	public boolean gotConnection(ReceivePort receiver, SendPortIdentifier applicant) {
		
		IbisIdentifier slaveIdentifier = applicant.ibisIdentifier();
		System.out.println(slaveIdentifier.toString());
		try {
			SendPort slaveSendPort = this.ibis.createSendPort(Constants.SEND_PORT_TYPE);
			slaveSendPort.connect(slaveIdentifier, Constants.SLAVE_IDENTIFIER);
			this.slaves.put(slaveIdentifier, slaveSendPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void lostConnection(ReceivePort receiver, SendPortIdentifier origin, Throwable cause) {
		IbisIdentifier slaveIdentifier = origin.ibisIdentifier();
		try {
			this.slaves.remove(slaveIdentifier).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}