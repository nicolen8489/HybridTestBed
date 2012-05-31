package hybridtestbed;

/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004-2008 Daniel Le Berre
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 *******************************************************************************/
import static org.sat4j.core.LiteralsUtils.var;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import org.sat4j.core.LiteralsUtils;
import org.sat4j.minisat.core.Constr;
import org.sat4j.minisat.core.ILits;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.PhaseInLastLearnedClauseSelectionStrategy;
import org.sat4j.specs.IVec;

/*
 * Created on 16 oct. 2003
 */

/**
 * @author leberre Heuristique du prouveur. Changement par rapport au MiniSAT
 *         original : la gestion activity est faite ici et non plus dans Solver.
 */
public class HybridVarOrderHeap implements IOrder, Serializable {

	private static final long serialVersionUID = 1L;

	private static final double VAR_RESCALE_FACTOR = 1e-100;

	private static final double VAR_RESCALE_BOUND = 1 / VAR_RESCALE_FACTOR;

	/**
	 * mesure heuristique de l'activite d'une variable.
	 */
	protected double[] activity = new double[1];

	private double varDecay = 1.0;

	/**
	 * increment pour l'activite des variables.
	 */
	private double varInc = 1.0;

	protected ILits lits;

	private long nullchoice = 0;

	protected HybridHeap heap;

	protected IPhaseSelectionStrategy phaseStrategy;

	// nicolen
	//private WalkSatSolver solver;
	private ScoutSolver solver = null;
	private DataInfo dataInfo; // = new DataInfo();

	public HybridVarOrderHeap(DataInfo info) {
		this(new PhaseInLastLearnedClauseSelectionStrategy(), info);
	}
	
	public HybridVarOrderHeap(IPhaseSelectionStrategy strategy, DataInfo info) {
		this.phaseStrategy = strategy;
		this.dataInfo = info;
		//solver = new WalkSatSolver(dataInfo);
	}
	
	// nicolen
	public void setScoutSolver(ScoutSolver solver) {
		this.solver = solver;
	}

	/**
	 * Change the selection strategy.
	 * 
	 * @param strategy
	 */
	public void setPhaseSelectionStrategy(IPhaseSelectionStrategy strategy) {
		phaseStrategy = strategy;
	}
	
	public IPhaseSelectionStrategy getPhaseSelectionStrategy() {
		return phaseStrategy;
	}

	public void setLits(ILits lits) {
		this.lits = lits;
	}

	/**
	 * Selectionne une nouvelle variable, non affectee, ayant l'activite la plus
	 * elevee.
	 * 
	 * @return Lit.UNDEFINED si aucune variable n'est trouvee
	 */
	/*
	 * public int select() { while (!heap.empty()) { int var = heap.getmin();
	 * System.out.println(var); int next = phaseStrategy.select(var); if
	 * (lits.isUnassigned(next)) { if (activity[var] < 0.0001) { nullchoice++; }
	 * System.out.println("next "+ next); return next; } } return ILits.UNDEFINED;
	 * }
	 */

	/**
	 * Change la valeur de varDecay.
	 * 
	 * @param d
	 *          la nouvelle valeur de varDecay
	 */
	public void setVarDecay(double d) {
		varDecay = d;
	}

	/**
	 * Methode appelee quand la variable x est desaffectee.
	 * 
	 * @param x
	 */
	public void undo(int x) {
		if (!heap.inHeap(x))
			heap.insert(x);
	}

	/**
	 * Appelee lorsque l'activite de la variable x a change.
	 * 
	 * @param p
	 *          a literal
	 */
	public void updateVar(int p) {
		int var = var(p);
		updateActivity(var);
		phaseStrategy.updateVar(p);
		if (heap.inHeap(var))
			heap.increase(var);
	}

	protected void updateActivity(final int var) {
		if ((activity[var] += varInc) > VAR_RESCALE_BOUND) {
			varRescaleActivity();
		}
	}

	/**
     *
     */
	public void varDecayActivity() {
		varInc *= varDecay;
	}

	/**
     *
     */
	private void varRescaleActivity() {
		for (int i = 1; i < activity.length; i++) {
			activity[i] *= VAR_RESCALE_FACTOR;
		}
		varInc *= VAR_RESCALE_FACTOR;
	}

	public double varActivity(int p) {
		return activity[var(p)];
	}

	/**
     *
     */
	public int numberOfInterestingVariables() {
		int cpt = 0;
		for (int i = 1; i < activity.length; i++) {
			if (activity[i] > 1.0) {
				cpt++;
			}
		}
		return cpt;
	}

	/**
	 * that method has the responsability to initialize all arrays in the
	 * heuristics. PLEASE CALL super.init() IF YOU OVERRIDE THAT METHOD.
	 */
	public void init() {
		int nlength = lits.nVars() + 1;
		if (activity == null || activity.length < nlength) {
			activity = new double[nlength];
		}
		phaseStrategy.init(nlength);
		activity[0] = -1;
		heap = new HybridHeap(activity);
		heap.setBounds(nlength);
		for (int i = 1; i < nlength; i++) {
			assert i > 0;
			assert i <= lits.nVars() : "" + lits.nVars() + "/" + i; //$NON-NLS-1$ //$NON-NLS-2$
			activity[i] = 0.0;
			if (lits.belongsToPool(i)) {
				heap.insert(i);
			}
		}
	}

	@Override
	public String toString() {
		return "VSIDS like heuristics from MiniSAT using a heap " + phaseStrategy; //$NON-NLS-1$
	}

	public ILits getVocabulary() {
		return lits;
	}

	public void printStat(PrintWriter out, String prefix) {
		out.println(prefix + "non guided choices\t" + nullchoice); //$NON-NLS-1$
	}

	public void assignLiteral(int p) {
		// do nothing
	}

	public void updateVarAtDecisionLevel(int q) {
		phaseStrategy.updateVarAtDecisionLevel(q);

	}
	// nicolen 
	int walksatCount = 0;
  int nodeCount = 0;
	Random random = new Random();
  long timeInWalkSAT = 0;
  long callToWalkSATTime = 0;
  long timeReducing = 0;
  int probability = 1024;
  long solverStartTime = 0;
  
	public int select() {
    nodeCount++;
		while (!heap.empty()) {
			int var = 0;
			int next = 0;
			// we only want to use scout when variables have been rated
			// and when the selection is ambivalent
			if(solver != null && random.nextInt(probability) == 0 &&
              heap.hasActivityValues() && heap.multipleVariableSelection()) {
        long start = System.currentTimeMillis();
				//System.out.println("start " + System.currentTimeMillis());
				walksatCount++;
				heap.hasActivityValues();
        long startR = System.currentTimeMillis();
				this.gatherReducedClauses();
        long endR = System.currentTimeMillis();
        timeReducing += (endR - startR);
				int[][] clauseArray = new int[clauses.size()][];
				clauses.toArray(clauseArray);
        long wStart = System.currentTimeMillis();
				boolean satisfied = solver.solve(clauseArray, lits.nVars());
        long wEnd = System.currentTimeMillis();
        timeInWalkSAT += (wEnd - wStart);
				if(satisfied) {
					callToWalkSATTime += (wEnd - start);
          System.out.println("nodeCount " + nodeCount);
          System.out.println("walkSATCount " + walksatCount);
          System.out.println("time ambivalent " + callToWalkSATTime);
          System.out.println("total time " + (wEnd - solverStartTime));
          System.out.println("percent of time in scout: " + (callToWalkSATTime / (wEnd - solverStartTime)));
          boolean[] solution = dataInfo.getSolution();
          int[] trail = Solver.getTrail();
          for(int i = 0; i < trail.length; i++) {
            solution[Math.abs(trail[i])] = trail[i] > 0 ? true : false;
          }
          
          System.out.print(solution[1] ? 1 : -1);
          for(int i = 2; i <= lits.nVars(); i++) {
            System.out.print(":" + (solution[i] ? i : -i));
          }
					System.out.println("scout SAT");
					System.exit(0);
				}
				int hardestClause = dataInfo.getHardestToSatisfyClause();
       // System.out.println("hardestClause " + hardestClause);
				int[] hardestClauseLiterals = clauses.get(hardestClause);
        double maxActivity = 0;
        for(int i = 0; i < hardestClauseLiterals.length; i++) {
          int temp = Math.abs(hardestClauseLiterals[i]);
          if(activity[temp] > maxActivity) {
            maxActivity = activity[temp];
            var = hardestClauseLiterals[i];
          }
        }
        // if the hardest clause all variables have an
        // activity of zero then no variable will be
        // selected by the max technique, so use a random
        // selection
        if(maxActivity == 0) {
          var = hardestClauseLiterals[random.nextInt(hardestClauseLiterals.length)];
        }
				
				// for now we will randomly select one of
				// the unset variables
				//Random random = new Random();
				//int i = random.nextInt(hardestClauseLiterals.length);
				//var = Math.abs(hardestClauseLiterals[i]);
				//System.out.println("using clause " + i + " with variable " + var);

				//var = LiteralsUtils.toInternal(var);
				if(var > 0) {
					next = LiteralsUtils.toInternal(var);
				} else {
					next = LiteralsUtils.toInternal(var) ^ 1;
				}
				var = Math.abs(var);
				heap.get(heap.indexOf(var));
        //System.out.println("var " + var + " next " + next);
        long end = System.currentTimeMillis();
        callToWalkSATTime += (end - start);
				//System.out.println("stop " + System.currentTimeMillis());
				// run scout solver with true assignment for variable
				/*lits.satisfies(var);
				this.gatherReducedClauses();
				clauseArray = new int[clauses.size()][];
				clauses.toArray(clauseArray);
				satisfied = solver.solve(clauseArray, lits.nVars());
				if(satisfied) {
					System.out.println("true assignment SAT");
					System.exit(0);
				}
				int stat0 = dataInfo.getMaxSatClause();
				// run scout solver with false assignment for variable
				lits.satisfies(lits.not(var));
				this.gatherReducedClauses();				
				clauseArray = new int[clauses.size()][];
				clauses.toArray(clauseArray);
				satisfied = solver.solve(clauseArray, lits.nVars());
				if(satisfied) {
					System.out.println("false assignment SAT");
					System.exit(0);
				}
				int stat1 = dataInfo.getMaxSatClause();
				if(stat0 >= stat1) {
					next = var;
				} else {
					next = var ^ 1;
				}
				// now unassign the variable that we've tested with
				// the software will handle assigning it correctly
				// now that we've made a selection
				lits.unassign(var);*/
			} else {
				// if we clearly have one variable to pick
				// just pick it
				var = heap.getmin();
				next = phaseStrategy.select(var);
			}

			//next = phaseStrategy.select(var);
			if (lits.isUnassigned(next)) {
				if (activity[var] < 0.0001) {
					nullchoice++;
				}
				return next;
			}
		}
		return ILits.UNDEFINED;
	}

	
	private IVec<Constr> constraints;
	// nicolen
	public void setConstraints(IVec<Constr> constraints) {
		this.constraints = constraints;
	}
	
	private ArrayList<int[]> clauses;

	
	private void gatherReducedClauses() {
		clauses = new ArrayList<int[]>();
		for (int i = 0; i < constraints.size(); i++) {
			int[] reduced = constraints.get(i).reduceConstraint();
			if (reduced.length > 0) {
				clauses.add(reduced);
			}
		}
	}
}
