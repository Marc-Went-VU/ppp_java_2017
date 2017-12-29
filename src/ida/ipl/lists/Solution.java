package ida.ipl.lists;

import java.io.Serializable;

public class Solution implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9201832424041204040L;
	private int bound;
	private int solution;

	public Solution(int bound, int solution) {
		this.bound = bound;
		this.solution = solution;
	}

	public int getBound() {
		return bound;
	}

	public int getSolution() {
		return solution;
	}
	
	@Override
	public String toString() {
		// SOLUTION(bound: <bound>, solution: <solution>)
		StringBuilder sb = new StringBuilder();
		sb.append("SOLUTION(");
		sb.append("bound: ");
		sb.append(this.bound);
		sb.append(", solution: ");
		sb.append(this.solution);
		sb.append(')');
		return sb.toString();
	}

	
}
