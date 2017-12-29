package ida.ipl.lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

import ida.ipl.extra.Board;

public class QuestionList extends PriorityBlockingQueue<Board> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1111410092525913284L;
	public QuestionList() {
		super(50, new Comparator<Board>() {

			@Override
			public int compare(Board o1, Board o2) {
				// Place lowest bound first, needs to be solved first
				return Integer.compare(o1.bound(), o2.bound());
			}
			
		});
	}
	
	public synchronized void merge(QuestionList ql) {
		this.addAll(ql);
	}
	public synchronized void purgeAboveBound(int bound) {
		Iterator<Board> bl = this.iterator();
		ArrayList<Board> toRemove = new ArrayList<Board>();
		Board b;
		while(bl.hasNext()) {
			b = bl.next();
			if(b.bound() > bound) {
				toRemove.add(b);
			}
		}
		this.removeAll(toRemove);
	}
}
