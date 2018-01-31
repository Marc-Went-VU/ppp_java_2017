package ida.ipl;


import java.util.ArrayList;
import java.util.Arrays;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ida.ipl.extra.Arguments;
import ida.ipl.extra.Board;
import ida.ipl.extra.BoardCache;
import ida.ipl.extra.consts.Constants;

public abstract class Shared implements MessageUpcall{

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	protected Arguments arg;
	protected Ibis ibis;
	final private String name;

	public Shared(Ibis ibis, Arguments arg) {
		this.ibis = ibis;
		this.arg = arg;
		this.name = (this.arg.isMaster() ? ANSI_RED + Constants.MASTER_IDENTIFIER + ANSI_RESET : ANSI_GREEN + Constants.SLAVE_IDENTIFIER + ANSI_RESET) + " " + this.arg.getMyIdentifier().name();
	}


	public synchronized void print(Object...objects) {
		if(!this.arg.canPrint()) { return; }
		ArrayList<Object> objs = new ArrayList<Object>();
		for(Object o : objects) {
			if(o instanceof Object[]) {
				objs.addAll(Arrays.asList((Object[])o));
			}
			else {
				objs.add(o);
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(this.name);
		for(Object o : objs) {
			sb.append(" " + o.toString());
		}
		System.out.println(sb.toString());
	}

	/**
	 * ORIGINAL
	 * expands this board into all possible positions, and returns the number of
	 * solutions. Will cut off at the bound set in the board.
	 */
	protected static int solutions(Board board, BoardCache cache) {
		if (board.distance() == 0) {
			return 1;
		}

		if (board.distance() > board.bound()) {
			return 0;
		}

		Board[] children = board.makeMoves(cache);
		int result = 0;

		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				result += solutions(children[i], cache);
			}
		}
		cache.put(children);
		return result;
	}

	/**
	 * ORIGINAL
	 * expands this board into all possible positions, and returns the number of
	 * solutions. Will cut off at the bound set in the board.
	 */
	protected static int solutions(Board board) {
		if (board.distance() == 0) {
			return 1;
		}

		if (board.distance() > board.bound()) {
			return 0;
		}

		Board[] children = board.makeMoves();
		int result = 0;

		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				result += solutions(children[i]);
			}
		}
		return result;
	}

	protected static ArrayList<Board> children(int MAX_DEPTH, Board board, int curr_depth) {
		ArrayList<Board> result = new ArrayList<Board>();
		if (board.distance() > board.bound()) {
			return result;
		}
		Board[] children = board.makeMoves();

		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				if(MAX_DEPTH == curr_depth) {
					result.add(children[i]);
				}
				else {
					result.addAll(children(MAX_DEPTH, children[i], curr_depth + 1));
				}
			}
		}
		return result;
	}

	protected static ArrayList<Board> children(int MAX_DEPTH, Board board, BoardCache cache, int curr_depth) {
		ArrayList<Board> result = new ArrayList<Board>();
		if (board.distance() > board.bound()) {
			return result;
		}
		Board[] children = board.makeMoves(cache);

		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				if(MAX_DEPTH == curr_depth) {
					result.add(children[i]);
				}
				else {
					result.addAll(children(MAX_DEPTH, children[i], cache, curr_depth + 1));
				}
			}
		}
		cache.put(children);
		return result;
	}
}
