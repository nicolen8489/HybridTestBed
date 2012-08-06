package hybridtestbed;

public class DataInfo {
	private int maxSatClause;
	private int localMinCount;
	private int localMinSatClauseSum;
	private int hardestToSatisfy;
	private boolean[] solution;
	private int next;
	private int var;
	private int[] counts;
	private int numAtoms;
	private long walksatTime;
	private long solverStartTime;

	public DataInfo() {
		maxSatClause = 0;
	}

	public void setMaxSatClause(int maxSatClause) {
		this.maxSatClause = maxSatClause;
	}

	public int getMaxSatClause() {
		return maxSatClause;
	}

	public double getAvgLocalMinSatClauses() {
		return (double) localMinCount / localMinSatClauseSum;
	}

	public void setHardestToSatisfyClause(int i) {
		hardestToSatisfy = i;
	}

	public int getHardestToSatisfyClause() {
		return hardestToSatisfy;
	}

	public void setSolution(boolean[] solution) {
		this.solution = solution;
	}

	public boolean[] getSolution() {
		if (solution == null) {
			return new boolean[0];
		}
		return solution;
	}

	public int getNext() {
		return next;
	}

	public void setNext(int next) {
		this.next = next;
	}

	public int getVar() {
		return var;
	}

	public void setVar(int var) {
		this.var = var;
	}

	public int getCount(int var) {
		return counts[numAtoms + var] + counts[numAtoms + -var];
	}

	public int getPolarityCount(int var) {
		return counts[numAtoms + var];
	}

	public void setNumAtoms(int numAtoms) {
		this.numAtoms = numAtoms;
	}

	public void setCounts(int[] counts) {
		this.counts = counts;
	}

	public long getWalksatTime() {
		return walksatTime;
	}

	public void setWalksatTime(long walksatTime) {
		this.walksatTime = walksatTime;
	}
	
	public void updateWalksatTime(long walksatTime) {
		this.walksatTime += walksatTime;
	}

	public long getSolverStartTime() {
		return solverStartTime;
	}

	public void setSolverStartTime(long solverStartTime) {
		this.solverStartTime = solverStartTime;
	}

}
