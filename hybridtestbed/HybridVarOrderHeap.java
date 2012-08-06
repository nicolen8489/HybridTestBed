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
import org.sat4j.specs.IVecInt;

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
	private Ambivalence ambivalence;

	public HybridVarOrderHeap(DataInfo info, Ambivalence ambivalence) {
		this(new PhaseInLastLearnedClauseSelectionStrategy(), info, ambivalence);
	}

	public HybridVarOrderHeap(IPhaseSelectionStrategy strategy, DataInfo info,
			Ambivalence ambivalence) {
		this.phaseStrategy = strategy;
		this.ambivalence = ambivalence;
		this.dataInfo = info;
		long seed = System.currentTimeMillis();
		//seed = Long.parseLong("1341793769561");
		random = new Random(seed);
		System.out.println("heap seed " + seed);
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
	 * System.out.println("next "+ next); return next; } } return
	 * ILits.UNDEFINED; }
	 */

	/**
	 * Change la valeur de varDecay.
	 * 
	 * @param d
	 *            la nouvelle valeur de varDecay
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
	 *            a literal
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

	public void varDecayActivity() {
		varInc *= varDecay;
	}

	private void varRescaleActivity() {
		for (int i = 1; i < activity.length; i++) {
			activity[i] *= VAR_RESCALE_FACTOR;
		}
		varInc *= VAR_RESCALE_FACTOR;
	}

	public double varActivity(int p) {
		return activity[var(p)];
	}

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
		// nicolen
		heap = new HybridHeap(activity);
		ambivalence.setHeap(heap);
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
	int nodeCount = 0;
	Random random;
	boolean firstSelectCall = true;
    private DataInfo dataInfo;

	public int select() {
		nodeCount++;
		while (!heap.empty()) {
			int var = 0;
			int next = 0;
			if (strategy != null
					&& (firstSelectCall || ambivalence.isAmbivalent(dataInfo))) {

				firstSelectCall = false;
				this.updateClauseStates();
				if(strategy.run(clauses, lits.nVars(), size, dataInfo, activity)) {
				  System.out.println("nodeCount " + nodeCount);
				  System.out.println(ambivalence.getAmbivalenceData());
				  System.out.println("\nSAT");
				  System.exit(0);
				}
				heap.get(heap.indexOf(dataInfo.getVar()));
				next = dataInfo.getNext();
			} else {
				// if we clearly have one variable torandom.next pick
				// just pick it
				var = heap.getmin();
				next = phaseStrategy.select(var);
			}

			if (lits.isUnassigned(next)) {
				if (activity[var] < 0.0001) {
					nullchoice++;
				}
				return next;
			} 
		}
		return ILits.UNDEFINED;
	}

	private WalkSatStrategy strategy;

	public void setStrategy(WalkSatStrategy strategy) {
		this.strategy = strategy;
	}

	private int[][] clauses;
	private int[] size;

	public void setupClauseStates(IVec<Constr> constraints) {
		clauses = new int[constraints.size()][];
		size = new int[constraints.size()];
		for (int i = 0; i < constraints.size(); i++) {
			int[] reduced = constraints.get(i).toArray(); 
			clauses[i] = reduced;
			size[i] = reduced.length;
		}
	}

	private void updateClauseStates() {
		for (int i = 0; i < clauses.length; i++) {
			int setVariables = 0;
			for (int j = 0; j < clauses[i].length - setVariables; j++) {
				int internal = LiteralsUtils.toInternal(clauses[i][j]);
				if (lits.isSatisfied(internal)) {
					setVariables = clauses[i].length;
					break;
				} else if (!lits.isUnassigned(internal)) {
					int temp = clauses[i][j];
					clauses[i][j] = clauses[i][clauses[i].length
							- ++setVariables];
					clauses[i][clauses[i].length - setVariables] = temp;
					j--;
				}
			}
			size[i] = clauses[i].length - setVariables;
		}
	}
}
