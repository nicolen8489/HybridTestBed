package hybridtestbed;

import java.util.Random;

import org.sat4j.core.LiteralsUtils;
import org.sat4j.minisat.core.ILits;

public class RandomSelectionStrategy extends WalkSatStrategy {
	private Random random;
	public RandomSelectionStrategy(WalkSatSolver solver, ILits vocabulary) {
		super(solver, vocabulary);
		random = new Random();
	}

	public boolean run(int[][] clauses, int numVars, 
			int[] size, DataInfo dataInfo, double[] activity) {
		walksatCount++;
		start = System.currentTimeMillis();
		long wStart = System.currentTimeMillis();
		boolean satisfied = solver.solve(clauses, lits.nVars(), size);
		wEnd = System.currentTimeMillis();
		timeInWalkSat += (wEnd - wStart);
		if(satisfied) {
			this.satisfied(dataInfo);
			return true;
		}
		int hardestClause = dataInfo.getHardestToSatisfyClause();
		int[] hardestClauseLiterals = clauses[hardestClause];
		double maxActivity = 0;
		for (int i = 0; i < size[hardestClause]; i++) {
			int temp = Math.abs(hardestClauseLiterals[i]);
			if (activity[temp] > maxActivity) {
				maxActivity = activity[temp];
				dataInfo.setVar(hardestClauseLiterals[i]);
			}
		}
		// if the hardest clause all variables have an
		// activity of zero then no variable will be
		// selected by the max technique, so use a random
		// selection
		if (maxActivity == 0) {
			dataInfo.setVar(hardestClauseLiterals[random.nextInt(size[hardestClause])]);
		}
		dataInfo.setNext(LiteralsUtils.toInternal(dataInfo.getVar()));
		dataInfo.setVar(Math.abs(dataInfo.getVar()));
		
		long end = System.currentTimeMillis();
		callToWalkSATTime += (end - start);
		return false;
	}
}
