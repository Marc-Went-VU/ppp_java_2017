package ida.ipl.messages;

import java.io.Serializable;

import ida.ipl.extra.Board;
import ida.ipl.lists.Solution;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3934429119306657115L;
	private MessageType messageType;
	
	private Board board;
	private Solution solution;
	private int bound_area;
	
	
	public Message(MessageType mt) {
		this.messageType = mt;
	}


	public Board getBoard() {
		return board;
	}


	public Message setBoard(Board board, int bound_area) {
		this.board = board;
		this.bound_area = bound_area;
		return this;
	}

	public int getSearchBound() {
		return this.bound_area;
	}

	public Solution getSolution() {
		return solution;
	}


	public Message setSolution(Solution solution) {
		this.solution = solution;
		return this;
	}


	public MessageType getMessageType() {
		return messageType;
	}
	
	public boolean hasBoard() {
		return this.board != null;
	}
	
	public boolean hasSolution() {
		return this.solution != null;
	}
	
}
