package ida.ipl.lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
public class SolutionsList extends HashMap<Integer, AtomicInteger>
{	 
	private static final long serialVersionUID = 8952136262511198264L;
	private ArrayList<Integer> foundSolution;

	public SolutionsList() {
		this.foundSolution = new ArrayList<Integer>();
	}
	
	public boolean setSolution(Solution s) {
		int bound = s.getBound();
		int solution = s.getSolution();
		if(solution > 0 && !this.foundSolution.contains(bound)) {
			this.foundSolution.add(bound);
		}
		if(this.containsKey(bound)) {
			this.get(bound).addAndGet(solution);
		}
		else {
			this.put(bound, new AtomicInteger(solution));
		}
		
		return solution > 0;
	}
	
	public boolean foundSolution() {
		return this.foundSolution.size() > 0;
	}
	
	public Solution getSmallestSolution() {
		int bound = Collections.min(this.foundSolution);
		int solution = this.get(bound).get();
		return new Solution(bound, solution);
	}
}
