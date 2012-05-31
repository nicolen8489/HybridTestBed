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
	
	// HybridSolver -scout ScoutSolver -metric Metric -prob n (1/n) (other properties for solver or metric) cnfFile
  public static void main(String[] args) {
    if (args.length > 0) {
      long start = System.currentTimeMillis();
      ISolver solver = SolverFactory.newDefault();
      DataInfo dataInfo = new DataInfo();
      HybridVarOrderHeap heap = new HybridVarOrderHeap(((VarOrderHeap)((Solver)solver).getOrder()).getPhaseSelectionStrategy(), dataInfo);
      ((Solver)solver).setOrder(heap);
      String parameters = "";
      // we don't want to include the filename
      for(int i = 0; i < args.length - 1; i++) {
        if(args[i].equals("-prob")) {
          heap.probability = Integer.parseInt(args[++i]);
        } else {
      	  parameters += args[i] + " ";
        }
      }
      parameters += "-solcnf";
      WalkSatSolver scoutSolver = new WalkSatSolver(dataInfo, parameters);
      scoutSolver.numrun = 1;
      heap.setScoutSolver(scoutSolver);
      heap.setConstraints(((Solver)solver).getOriginalConstraints());
      heap.solverStartTime = start;
      Reader reader = new DimacsReader(solver);
      IProblem problem = null;
      try {
        problem = reader.parseInstance(args[args.length - 1]);
        if (problem.isSatisfiable()) {
          int[] solution = problem.model();
          if(solution.length > 0) {
          	System.out.println(solution[0]);
          	for(int i = 1; i < solution.length; i++) {
          		System.out.print(":" + solution[i]);
          	}
          	System.out.println();
          }
          long end = System.currentTimeMillis();
          System.out.println("nodeCount " + heap.nodeCount);
          System.out.println("walkSATCount " + heap.walksatCount);
          System.out.println("time ambivalent " + heap.callToWalkSATTime);
          System.out.println("Time to run " + (end - start));
          System.out.println("percent of time in scout: " + (heap.callToWalkSATTime / (end - start)));
          System.out.println("SAT");
        } else {
          long end = System.currentTimeMillis();
          System.out.println("nodeCount " + heap.nodeCount);
          System.out.println("walkSATCount " + heap.walksatCount);
          System.out.println("time ambivalent " + heap.callToWalkSATTime);
          System.out.println("Time to run " + (end - start));
          System.out.println("percent of time in scout: " + (heap.callToWalkSATTime / (end - start)));
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
