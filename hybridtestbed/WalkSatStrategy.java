package hybridtestbed;

import org.sat4j.minisat.core.ILits;

public abstract class WalkSatStrategy {
	protected WalkSatSolver solver = null;
	protected int walksatCount = 0;
	protected long timeInWalkSat = 0;
	protected long callToWalkSATTime = 0;
	protected ILits lits;
	protected long wEnd;
	protected long start;
	//long solverStartTime;

	public WalkSatStrategy(WalkSatSolver solver, ILits vocabulary) {
		this.solver = solver;
		lits = vocabulary;
	}

	public abstract boolean run(int[][] clauses, int numVars, 
			int[] size, DataInfo info, double[] activity);

	protected void satisfied(DataInfo dataInfo) {
		callToWalkSATTime += (wEnd - start);
		System.out.println("scoutCount " + walksatCount);
		System.out.println("scoutTime " + timeInWalkSat / 1000.0);
		System.out.println("scoutSetup " + (callToWalkSATTime - timeInWalkSat)
				/ 1000.0);
		System.out.println("scoutOverhead " + callToWalkSATTime / 1000.0);
		System.out.println("totalTime " + (wEnd - dataInfo.getSolverStartTime()) / 1000.0);
		System.out.println("solvedBy scout");
		System.out.println("%overhead: "
				+ (callToWalkSATTime / (wEnd - dataInfo.getSolverStartTime() + 0.0)) * 100);
		System.out.print("solution ");
		boolean[] solution = dataInfo.getSolution();
		for (int i = 1; i <= lits.nVars(); i++) {
			if (lits.belongsToPool(i)) {
				int p = lits.getFromPool(i);
				if (!lits.isUnassigned(p)) {
					solution[i] = lits.isSatisfied(p);
				}
			}
		}
		System.out.print(solution[1] ? 1 : -1);
		for (int i = 2; i <= lits.nVars(); i++) {
			System.out.print(":" + (solution[i] ? i : -i));
		}
		System.out.println("");
	}
}
