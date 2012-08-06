package hybridtestbed;

import org.sat4j.specs.ISolver;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.reader.Reader;
import org.sat4j.reader.DimacsReader;
import org.sat4j.specs.IProblem;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.TimeoutException;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class HybridSolver {

	// HybridSolver -scout ScoutSolver -metric Metric -prob n (1/n) (other
	// properties for solver or metric) cnfFile
	public static void main(String[] args) {
		if (args.length > 0) {
			long start = System.currentTimeMillis();
			ISolver solver = SolverFactory.newDefault();
			DataInfo dataInfo = new DataInfo();
			Ambivalence ambivalence = new First2EqualAmbivalence(0.1042, 1024, null);
			String parameters = "";
			boolean useNVars = false;
			String strategyType = "randomSelect";
			boolean maintainPercent = false;
			// we don't want to include the filename
			for (int i = 0; i < args.length - 1; i++) {
				if (args[i].equals("-prob")) {
					ambivalence.setScoutProb(Integer.parseInt(args[++i]));
				} 
				else if (args[i].equals("-ambProb")) {
					ambivalence.setAmbProb(Double.parseDouble(args[++i]));
				} else if (args[i].equals("-ambType")) {
					if (args[++i].equals("inverseProb")) {
						ambivalence = new InverseProbAmbivalence(ambivalence.getAmbProb(),
								ambivalence.getScoutProb(), null, 500);
						if (args[++i].equals("-lowestProb")) {
							((InverseProbAmbivalence) ambivalence).setLowestProb(Integer
									.parseInt(args[++i]));
						} else {
							i--;
						}
					} else if(args[i].equals("original10")) {
						ambivalence = new First10Ambivalence(ambivalence.getAmbProb(),
								ambivalence.getScoutProb(), null);
					} else {
						System.err.println("Unrecognized ambivalence type " + args[i]);
						System.exit(1);
					}
				} else if(args[i].equals("-nVars")) {
					useNVars = true;
				} else if(args[i].equals("-strategy")) { 
					strategyType = args[++i];
			    } else if(args[i].equals("-maintainPercent")) {
			    	maintainPercent = true;
			    } else {
					parameters += args[i] + " ";
				}
			}

			ambivalence.maintainPercent = maintainPercent;
			HybridVarOrderHeap heap = new HybridVarOrderHeap(
					((VarOrderHeap) ((Solver) solver).getOrder())
							.getPhaseSelectionStrategy(), dataInfo, ambivalence);
			((Solver) solver).setOrder(heap);
			WalkSatSolver scoutSolver = new WalkSatSolver(dataInfo, parameters);
			scoutSolver.numrun = 1;
			Reader reader = new DimacsReader(solver);
			IProblem problem = null;
			try {
				problem = reader.parseInstance(args[args.length - 1]);
				WalkSatStrategy strategy = null;
				if(strategyType.equals("clauseCount")) {
					strategy = new ClauseCountStrategy(scoutSolver, ((Solver)solver).getVocabulary());
				} else if(strategyType.equals("polarityClauseCount")) {
					strategy = new PolarityClauseCountStrategy(scoutSolver, ((Solver)solver).getVocabulary());
				} else {
					strategy = new RandomSelectionStrategy(scoutSolver, ((Solver)solver).getVocabulary());
				}
				dataInfo.setSolverStartTime(start);
				heap.setStrategy(strategy);
				heap.setupClauseStates(((Solver) solver).getOriginalConstraints());
				
				if (problem.isSatisfiable()) {
					System.out.print("solution ");
					int[] solution = problem.model();
					if (solution.length > 0) {
						System.out.print(solution[0]);
						for (int i = 1; i < solution.length; i++) {
							System.out.print(":" + solution[i]);
						}
						System.out.println();
					}
					long end = System.currentTimeMillis();
					System.out.println("nodeCount " + heap.nodeCount);
					System.out.println("scoutCount " + strategy.walksatCount);
					System.out.println("scoutTime " + strategy.timeInWalkSat / 1000.0);
					System.out.println("scoutSetup "
							+ (strategy.callToWalkSATTime - strategy.timeInWalkSat) / 1000.0);
					System.out
							.println("scoutOverhead " + strategy.callToWalkSATTime / 1000.0);
					System.out.println("totalTime " + (end - start) / 1000.0);
					System.out.println("solvedBy sat4j");
					System.out.println("%overhead: "
							+ (strategy.callToWalkSATTime / (end - start + 0.0)) * 100);
					System.out.println(ambivalence.getAmbivalenceData());
					System.out.println("SAT");
				} else {
					long end = System.currentTimeMillis();
					System.out.println("nodeCount " + heap.nodeCount);
					System.out.println("scoutCount " + strategy.walksatCount);
					System.out.println("scoutTime " + strategy.timeInWalkSat / 1000.0);
					System.out.println("scoutSetup "
							+ (strategy.callToWalkSATTime - strategy.timeInWalkSat) / 1000.0);
					System.out
							.println("scoutOverhead " + strategy.callToWalkSATTime / 1000.0);
					System.out.println("totalTime " + (end - start) / 1000.0);
					System.out.println("solvedBy sat4j");
					System.out.println("%overhead: "
							+ (strategy.callToWalkSATTime / (end - start + 0.0)) * 100);
					System.out.println(ambivalence.getAmbivalenceData());
					System.out.println("UNSAT");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			} catch (ParseFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ContradictionException e) {
				e.printStackTrace();
			}
		}
	}
}
