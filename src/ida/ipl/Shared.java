package ida.ipl;

import java.util.ArrayList;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ida.ipl.extra.Arguments;
import ida.ipl.extra.Board;
import ida.ipl.extra.BoardCache;

public abstract class Shared implements MessageUpcall{

	protected Arguments arg;
	protected Ibis ibis;

	public Shared(Ibis ibis, Arguments arg) {
		this.ibis = ibis;
		this.arg = arg;
		System.out.println("Starting "+ arg.getMyIdentifier().name());
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
