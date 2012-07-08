package hybridtestbed;

import java.util.Random;

import org.sat4j.core.LiteralsUtils;
import org.sat4j.minisat.core.ILits;

public class ClauseCountStrategy extends WalkSatStrategy {
	public ClauseCountStrategy(WalkSatSolver solver, ILits vocabulary) {
		super(solver, vocabulary);
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
		int maxCount = 0;
		int maxCountVar = 0;
		for (int i = 0; i < size[hardestClause]; i++) {
			int temp = Math.abs(hardestClauseLiterals[i]);
			if (activity[temp] > maxActivity) {
				maxActivity = activity[temp];
				dataInfo.setVar(hardestClauseLiterals[i]);
			}
			int count = dataInfo.getCount(hardestClauseLiterals[i]);
			if(count > maxCount) {
				maxCount = count;
				maxCountVar = hardestClauseLiterals[i];
			}
		}
		// if the hardest clause all variables have an
		// activity of zero then no variable will be
		// selected by the max technique, so use a random
		// selection
		if (maxActivity == 0) {
			//dataInfo.setVar(hardestClauseLiterals[random.nextInt(size[hardestClause])]);
			dataInfo.setVar(maxCountVar);
			if(maxCount == 0) {
				System.out.println("Error max count is 0");
				System.exit(0);
			}
		}
		dataInfo.setNext(LiteralsUtils.toInternal(dataInfo.getVar()));
		dataInfo.setVar(Math.abs(dataInfo.getVar()));
		
		long end = System.currentTimeMillis();
		callToWalkSATTime += (end - start);
		return false;
	}
}
