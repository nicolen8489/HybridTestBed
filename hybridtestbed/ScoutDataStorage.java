//package hybridtestbed;
//
//import java.util.ArrayList;
//import org.sat4j.minisat.core.Constr;
//import org.sat4j.specs.IVec;
//
//public class ScoutDataStorage {
//	private int numVars;
//	private ScoutSolver solver;
//	private ArrayList<ArrayList<Integer>> clauses;
//	private IVec<Constr> constraints;
//	private DataInfo dataInfo;
//	private int[][] reducedClauses; 
//	
//	public ScoutDataStorage(int numVars, IVec<Constr> constraints) {
//		this.numVars = numVars;
//		this.constraints = constraints;
//		this.dataInfo = new DataInfo();
//	}
//	
//	public void setupSolver() {
//		solver = new WalkSatSolver(dataInfo);
//	}
//	
//	public boolean solve(boolean firstCall) {
//		if(firstCall) {
//			this.gatherReducedClauses();
////		  solver.setupProb(clauses, numVars);
//		}
//		
//		int[][] reducedClauses = new int[clauses.size()][];
//		clauses.toArray(reducedClauses);
//		solver.setupProb(reducedClauses, numVars);
//		return solver.solve();
//	}
//	
//	public boolean solve(boolean firstCall, int testVariable) {
//		if(firstCall) {
//			this.gatherReducedClauses();
////		  solver.setupProb(clauses, numVars);
//		}
//		for(int i = 0; i < clauses.size(); i++) {
//			int index = clauses.get(i).indexOf(testVariable);
//			while(index != -1) {
//				clauses.remove(index);
//				index = clauses.get(i).indexOf(testVariable);
//			}
//			index = clauses.get(i).indexOf(-testVariable);
//			while(index != -1) {
//				clauses.remove(index);
//				index = clauses.get(i).indexOf(-testVariable);
//			}
////			for(int j = 0; j < clauses.get(i).size(); j++) {
////				if(clauses.get(i).get(j) == testVariable) {
////					// this clause is satisfied
////					clauses.remove(i);
////					i--;
////					break;
////				} else if (clauses.get(i).get(j) == -testVariable) {
////					clauses.get(i).remove(j);
////					j--;
////				}
////			}
//			// if this clause has emptied out and is not satisfied
//			if(clauses.get(i).size() == 0) {
//				return false;
//			}
//		}
//		int[][] clauses2 = new int[clauses.size()][];
//		clauses.toArray(clauses2);
//		solver.setupProb(clauses2, numVars);
//		return solver.solve(testVariable);
//	}
//
//	public boolean hasSolver() {
//		return solver != null;
//	}
//	
//	private void gatherReducedClauses() {
//		clauses = new ArrayList<ArrayList<Integer>>();
//		for(int i = 0; i < constraints.size(); i++) {
//			ArrayList<Integer> reduced = constraints.get(i).reduceConstraint();
//			if(reduced.size() > 0) {
//				clauses.add(reduced);
//			}
//		}
//	}
//	
//	public int getNumVars() {
//		return numVars;
//	}
//
//	public DataInfo getDataInfo() {
//		return dataInfo;
//	}
//	
//	public int[][] getReducedClauses() {
//		return reducedClauses;
//	}
//}
