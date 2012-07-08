package hybridtestbed;

import java.io.*;
import java.util.*;

// ************** WalkSatSolver **************
// Optimized translation of Henry Kautz and Bart Selman's C WalkSat algorithm.
// Takes boolean input in CNF form (from either a Reader or an Enumeration)

// nicolen
public class WalkSatSolver {

	/************************************/
	/* Constant parameters */
	/************************************/

	// maximum possible number of clauses allowed for a problem
	public static final int MAXCLAUSE = 500000;

	// maximum number of literals which will be allowed in any clause
	public static final int MAXLENGTH = 500;

	/************************************/
	/* Internal constants */
	/************************************/

	// The various heuristics which are supported in the C version. Only
	// BEST (the normal WalkSAT heuristic has currently been translatecd
	// into Java

	// enum heuristics { RANDOM, BEST, TABU, NOVELTY, RNOVELTY };
	private static final int RANDOM = 0;
	private static final int BEST = 1;
	private static final int TABU = 2;
	private static final int NOVELTY = 3;
	private static final int RNOVELTY = 4;
	// nicolen
	private static final int NOVELTY_PLUS = 5;
	private static final int RNOVELTY_PLUS = 6;
	
	// nicolen
	private Picker pickcode[] = new Picker[]{new RandomPicker(), new BestPicker(), new TabuPicker(),
	                                       new NoveltyPicker(), new RNoveltyPicker(),
	                                       new NoveltyPlusPicker(), new RNoveltyPlusPicker()};

	private static int NOVALUE = 0;

	// A number assumed to be practically infinite. larger than the
	// size of the largest clause
	private static int BIG = Integer.MAX_VALUE;

	// The input and output streams to be used when using Reader/Writer I/O.
	// (such as from/to a file)
	BufferedReader input;
	BufferedWriter output;

	// Handle on the Enumeration used when in Enumeration input mode.
	// (such as an Enumeration from a Trie)
	Enumeration trieClauses = null;

	// Holds all solutions found by WalkSAT
	boolean allSolutions[][] = null;

	// Used to keep track how many solutions we have so far. <allSolutionsI>
	// is the index in allSolutions where then next solution[] should go.
	int allSolutionsI = 0;

	// The numer of atoms, clauses, and literals that appear in the problem.
	int numAtoms;
	int numClauses;
	int numLiterals;

	// Used by pickbest (and other heuristics, if they are translated into
	// Java). The array is allocated at initialization and recycled/reused by
	// the heuristic every flip. This is because in Java, allocating a new array
	// every flip is, performance wise, Very Bad (since Java arrays are always
	// Objects and never stack arrays).
	int best[];

	// The clauses in the boolean formula. [clause#][literal#]
	int clause[][];

	// The length of each clause [clause#] (unneccessary for Java, but slightly
	// faster performance in some cases by using it? clause[x].length == size[x]
	int size[];

	// A list of clauses which are false
	int falseClauses[];

	int lowFalse[];

	// Where each clause is listed in falseClauses. [clause#]
	// e.g., if clause 5 is false, and is listed at, say, falseClauses[2]
	// (e.g., falseClauses[2]=5), then whereFalse[5]=2
	int whereFalse[];

	// The number of true literals in each clause [clause#]
	int numTrueLiterals[];

	// <literalOccurences>: For each literal, list of clauses where the
	// literal occurs.
	// [literal+numAtoms][occurence#] e.g., the list for literal 8 is at
	// numAtoms+8, while the list for !8 (i.e., -8) is at numAtoms-8
	// NOTE: The literal 0 is never used in CNF clauses (how would you have -0?).
	// So, nothing ever happens to literalOccurences[numAtoms] or
	// numLiteralOccurences[numAtoms]
	int literalOccurences[][];

	// number of times each literal occurs.
	// numLiteralOccurences[x]==literalOccurences[x].length
	// Occasional use under same rational as <size>
	int numLiteralOccurences[];

	// value of each variable (atom)
	// [variable#]
	boolean atom[];

	boolean lowAtom[];

	// Used to hold a satisfying assignment (solution) for the problem.
	boolean solution[];

	// step at which variable (atom) was last flipped. This is used by
	// the picknovelty and pickrnovelty heuristics (which are not currently
	// translated to Java)
	// [variable#]
	// e.g., changed[5]=3 means that variable 5 was last flipped at step 3
	long changed[];

	// number of clauses that become unsatisfied if variable is flipped
	// [variable#]
	int breakCount[];
	// number of clauses that become satisified if variable is flipped
	// [variable#]
	int makeCount[];

	// the number of clauses that are currently false
	int numFalseClauses;

	/************************************/
	/* Global flags and parameters */
	/************************************/

	// Set to true when we want to halt the algorithm. Useful when in Thread
	// mode.
	boolean abort_flag;

	// heuristic to be used by the algorithm
	int heuristic = BEST;

	// make random flip with numerator/denominator frequency
	// (e.g., if numerator=50 and denominator=100, then we will do a random
	// flip 50% of the time)
	int numerator = 50;
	int denominator = 100;

	// number of changes (flips) so far
	long numflip;
	// number of times a clause was picked, but no variable from it was flipped
	long numnullflip;

	// The number of tries that should be made.
	int numrun = 10;
	// The number of flips that should be made per try
	//long cutoff = 100000;
	// nicolen
	long cutoff = 25000;
	// long cutoff = 5000000;

	long base_cutoff = 100000;

	int target = 0;
	// the number of tries made so far
	int numtry = 0;
	// number of solutions to find
	int numsol = NOVALUE;

	boolean superlinear = false;

	// set to true by heuristics that require the make values to be calculated
	boolean makeflag = false;

	int tail = 3;
	int tail_start_flip;

	// Randomization
	// -------------

	long seed; /* seed for random */
	// The fast random number generator
	Ranmar randGenerator;

	// Statistics

	double expertime;
	long flips_this_solution;
	/* lowest number of bad clauses during try */
	long lowbad;
	/* total number of flips in all tries so far */
	long totalflip = 0;
	/* total number of flips in all tries which succeeded so far */
	long totalsuccessflip = 0;
	/* total found solutions */
	int numsuccesstry = 0;

	// True if input if from a Reader. False if from an Enumeration
	// nicolen
	// boolean fileInput = true;
	private static final int FILE = 1;
	private static final int ENUM = 2;
	private static final int PASSED = 3;
	int inputFormat = this.FILE;

	public WalkSatSolver(Reader input, Writer output, int numsol, String parameter) {
		// Constructor for the Reader/Writer I/O version
		// <numsol> is the max number of solutions that the algorithm should try
		// to find

		this.input = new BufferedReader(input);
		this.output = new BufferedWriter(output);
		this.numsol = numsol;
		parseParameters(parameter);
	}

	// nicolen
	int numTimesBroken[];

	// nicolen
	private DataInfo dataInfo;

	// private boolean enumerationInput;
	// nicolen
	public WalkSatSolver(DataInfo info, String parameter) {
		this.dataInfo = info;
		this.numsol = 1;
		parseParameters(parameter);
	}

	public WalkSatSolver(Enumeration clauses, int numClauses, int numAtoms,
			int numsol, String parameter) {
		// Constructor for the Enumeration I/O version

		trieClauses = clauses;
		// nicolen
		// fileInput = false;
		inputFormat = this.ENUM;
		this.numClauses = numClauses;
		this.numAtoms = numAtoms;
		this.numsol = numsol;
		parseParameters(parameter);
		// System.out.println("WalkSat created. " + numClauses + " clauses, " +
		// numAtoms + " atoms.");
	}

	private void parseParameters(String parameter) {
		String[] parts = parameter.split(" ");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].equals("-random"))
				heuristic = RANDOM;
			else if (parts[i].equals("-novelty")) {
				heuristic = NOVELTY;
				makeflag = true;
			} else if (parts[i].equals("-rnovelty")) {
				heuristic = RNOVELTY;
				makeflag = true;
			} else if (parts[i].equals("-novelty+")) {
				heuristic = NOVELTY_PLUS;
				makeflag = true;
			} else if (parts[i].equals("-rnovelty+")) {
				heuristic = RNOVELTY_PLUS;
				makeflag = true;
			} else if (parts[i].equals("-tabu")) {
				tabu_length = Integer.parseInt(parts[++i]);
				heuristic = TABU;
			} else if (parts[i].equals("-best"))
				heuristic = BEST;
			else if (parts[i].equals("-wp")) {
				wp_numerator = Integer.parseInt(parts[++i]);
				if (i < parts.length - 1) {
					try {
						wp_denominator = Integer.parseInt(parts[i + 1]);
						i++;
					} catch (NumberFormatException e) {
						wp_denominator = NOVALUE;
					}
				}
			} else if(parts[i].equals("-seed")) {
        this.seed = Long.parseLong(parts[++i]);
      } else if(parts[i].equals("-cutoff")) {
      	this.cutoff = Long.parseLong(parts[++i]);
      }
		}
	}

  private void printsolcnf()
  {
    System.out.println("seed " + seed);
    boolean[] temp = allSolutions[0];
    System.out.print(temp[1] ? 1 : -1);
    for(int i = 2; i <= numAtoms; i++) {
      System.out.print(":" + (temp[i] ? i : -i));
    }
    System.out.println();
  }


	public static void main(String argv[]) {
		// Run from the command line, takes as arguments the input file and
		// the output file.
		Reader in = null;
		Writer out = null;
		try {
			in = new FileReader(argv[0]);
			out = new FileWriter(argv[1]);
			System.out.println("results will be saved in " + argv[1]);
		} catch (FileNotFoundException e) {
			System.out.println(argv[0] + " not found!");
		} catch (IOException e) {
			System.out.println("IO exception");
		}
		// Defaults to making ten tries.
		WalkSatSolver solver = new WalkSatSolver(in, out, 1, "");
		solver.solve();

		try {
			System.out.println("closing results file");
			out.close();
		} catch (IOException e) {
			System.err.println("problem closing output file");
		}
	}

	// public WalkSatConnector connector;
	// public void setConnector(WalkSatConnector algo){
	// connector=algo;
	// }

	// nicolen
	public boolean solve(int[][] clauses, int numAtoms, int[] size) {
		this.clause = clauses;
		this.numAtoms = numAtoms;
		this.numClauses = clauses.length;
		this.inputFormat = this.PASSED;
		this.size = size;
		return solve();
	}

	public boolean solve() {
		return solve(numrun, numsol);
	}

	public void run() {
		// This is so the solver can be threaded.
		solve();
	}

	public void abort() {
		// Effects: Aborts execution at the end of the current flip.
		// Modifies: this
		abort_flag = true;
	}

	public boolean solve(int tries, int solutions) {

		// Effects: tries to find at most <solutions> sets of satisfying
		// assignments. Runs the algorithm at most <tries> times. (stops early
		// if it has already found <solutions> solutions. Returns true if any
		// solutions have been found.
		// Modifies: this & either the Reader/Writers or the Enumeration

		allSolutions = new boolean[solutions][];

		Date dt = new Date();
		long tm = dt.getTime();
    if(seed == 0) {
      seed = tm;// (tm & 0177) * 1000000;
      //seed = Long.parseLong("1341297178660");
      System.out.println("walksat seed " + seed);
    }

		randGenerator = new Ranmar(seed);

		// Initialize the problem
		initprob();

		abort_flag = false;
		// nicolen
		int maxSatClause = 0;

		while ((!(abort_flag)) && (numtry < tries) && numsuccesstry < numsol) {
			// Keep trying until we find enough solutions, we run out of tries,
			// or we are given the signal to abort.
			// while(connector.paused){if(connector.aborted){return false;}}

			numtry++;

			// initialize problem with certain (currently random) atom values
			init();

			update_statistics_start_try();
			numflip = 0;

			if (superlinear)
				cutoff = base_cutoff * numtry;
			// if (numClauses >100000) cutoff = base_cutoff*10;
			// System.out.println("cutoff> "+cutoff);

			long sTime = System.currentTimeMillis();
			while ((numFalseClauses > target) && (numflip < cutoff)) {
				// Keep flipping until we have found a solution or reached the
				// max number of flips.

				/*
				 * if ((numflip % 10) == 0) { System.out.println("unsat clauses> " +
				 * numFalseClauses); }
				 */

				numflip++;

				// Choose the atom to flip, and then flip it
				flipAtom(pickcode[heuristic].pick());

				if (numFalseClauses < lowbad) {
					lowbad = numFalseClauses;
				}

				// Abort if flagged
				if (abort_flag)
					break;

			}

			// Calculate how many flips/sec for this try
			long elTime = System.currentTimeMillis() - sTime;
			double flipsSec = (((double) numflip) / ((double) elTime)) * 1000;
			// System.out.println("time = " + elTime);
			// System.out.println("flips/sec = " + ((long) flipsSec));

			update_and_print_statistics_end_try();
			// nicolen
			if (this.numClauses - this.numFalseClauses > maxSatClause) {
				maxSatClause = this.numClauses - this.numFalseClauses;
			}
		}

		if (inputFormat == this.FILE) {
			// We are all done. We are in Reader/Writer mode, so flush the output
			// buffer.
			try {
				output.flush();
			} catch (IOException e) {
				System.err.println("problem flushing output buffer to results file");
			}
		}
		// if (numsuccesstry > 0) {
		// System.out.println("SAT");
		// } else {
		// System.out.println("UNSAT");
		// }
		// nicolen
		int maxUnSatClause = -1;
		int maxUnSatCount = -1;
		for (int i = 0; i < numClauses; i++) {
			if (numTimesBroken[i] > maxUnSatCount) {
				maxUnSatCount = numTimesBroken[i];
				maxUnSatClause = i;
			}
		}
		// nicolen
		dataInfo.setHardestToSatisfyClause(maxUnSatClause);
		dataInfo.setMaxSatClause(maxSatClause);

		// Return true if we found any solutions at all.
   /* if(numsuccesstry > 0) {
      this.printsolcnf();
    }*/
    if(numsuccesstry > 0) {
      dataInfo.setSolution(allSolutions[0]);
    }
		return (numsuccesstry > 0);

	}

	private final void flipAtom(int varToFlip) {

		// flipAtom, along with the heuristic methods, should be as optimized
		// as possible for best performance.

		// Effects: flips the atom specified by <varToFlip>. Updates all the
		// aprropriate data structures such as breakCount, etc.

		int i, j;/* loop counter */

		// literal to enforce. e.g., if toEnforce=-5, atom[5] is now false, so
		// we should update breakCount, etc., appropriately
		int toEnforce;

		int iClause;
		int lit;
		int numocc;
		int sz;
		int litptr[];
		int occptr[];

		if (varToFlip == NOVALUE) {
			numnullflip++;
			return;
		}

		changed[varToFlip] = numflip;
		if (atom[varToFlip])
			toEnforce = -varToFlip;
		else
			toEnforce = varToFlip;

		atom[varToFlip] = !(atom[varToFlip]);

		numocc = numLiteralOccurences[numAtoms - toEnforce];
		// occptr points to a list (array) of occurences for a particular variable
		occptr = literalOccurences[numAtoms - toEnforce];

		for (i = 0; i < numocc; i++) {

			// iClause = literalOccurences[numAtoms-toEnforce][i];
			// In C, we could just do:
			// iClause = *(occptr++);
			// Alas, in Java we can't get away with the extra efficent array
			// referencing above; we're going to have to end up with some array
			// arithmetic. This will reappear several times below.

			// iClause is the index of the clause we are currently checking
			iClause = occptr[i];
			
			if (--numTrueLiterals[iClause] == 0) {
				// --numTrueLiterals[iClause] == 0 means that we just made this
				// clause false by this flip.
				falseClauses[numFalseClauses] = iClause;
				whereFalse[iClause] = numFalseClauses++;
				// nicolen
				numTimesBroken[iClause]++;

				/* Decrement varToFlip's breakCount */
				breakCount[varToFlip]--;

				if (makeflag) {
					/* Increment the makeCount of all vars in the clause */
					sz = size[iClause];
					litptr = clause[iClause];
					for (j = 0; j < sz; j++) {
						/* lit = clause[iClause][j]; */
						// lit = *(litptr++);
						lit = litptr[j];
						lit = (lit < 0) ? -lit : lit;
						makeCount[lit]++;
					}
				}
			} else if (numTrueLiterals[iClause] == 1) {
				// Find the literal in this clause that makes the clause true,
				// and increase its breakCount
				sz = size[iClause];
				litptr = clause[iClause];
				for (j = 0; j < sz; j++) {
					/* lit = clause[iClause][j]; */
					// lit = *(litptr++);
					lit = litptr[j];

					boolean posNeg = (lit > 0);
					int literalValueIndex = (posNeg) ? (lit) : (-lit);
					if ((posNeg) == atom[literalValueIndex]) {
						breakCount[literalValueIndex]++;
						break;
					}
				}
			}
		}

		numocc = numLiteralOccurences[numAtoms + toEnforce];
		occptr = literalOccurences[numAtoms + toEnforce];
		for (i = 0; i < numocc; i++) {
			// iClause = literalOccurences[numAtoms+toEnforce][i];
			// iClause = *(occptr++);
			iClause = occptr[i];
			if (++numTrueLiterals[iClause] == 1) {
				// ++numTrueLiterals[iClause] == 1 means that we just made this
				// clause true by this flip
				numFalseClauses--;
				int oldEndValue = falseClauses[numFalseClauses];
				int nowTrueClauseIndex = whereFalse[iClause];
				falseClauses[nowTrueClauseIndex] = oldEndValue;
				// falseClauses[numFalseClauses];
				whereFalse[oldEndValue] = nowTrueClauseIndex; // falseClauses[numFalseClauses]]
				// =
				// whereFalse[iClause];
				/* Increment varToFlip's breakCount */
				breakCount[varToFlip]++;

				if (makeflag) {
					/* Decrement the makeCount of all vars in the clause */
					sz = size[iClause];
					litptr = clause[iClause];
					for (j = 0; j < sz; j++) {
						/* lit = clause[iClause][j]; */
						// lit = *(litptr++);
						lit = litptr[j];
						lit = (lit < 0) ? -lit : lit;
						makeCount[lit]--;
					}
				}
			} else if (numTrueLiterals[iClause] == 2) {
				/*
				 * Find the lit in this clause other than varToFlip that makes it true,
				 * and decrement its breakCount
				 */
				sz = size[iClause];
				litptr = clause[iClause];
				for (j = 0; j < sz; j++) {
					/* lit = clause[iClause][j]; */
					// lit = *(litptr++);
					lit = litptr[j];
					boolean posNeg = (lit > 0);
					int literalValueIndex = (posNeg) ? (lit) : (-lit);
					if ((posNeg == atom[literalValueIndex])
							&& (varToFlip != literalValueIndex)) {
						breakCount[literalValueIndex]--;
						break;
					}
				}
			}
		}
	}

	private void initprob() {
		int i; /* loop counter */
		int j; /* another loop counter */

		// Initialize the problem, either from a Reader or an Enumeration
		// fill all appropriate data structures

		// Read the first line, which contains the atom and clause numbers
		String aLine = null;
		if (inputFormat == this.FILE) {
			try {
				while ((aLine = input.readLine()).startsWith("c")) {
				}
				;
			} catch (IOException e) {
			}
			if (aLine == null) {
				System.err.println("bad file format");
				throw new RuntimeException();
			}
			String pLine = "p cnf ";
			aLine = aLine.substring(aLine.indexOf(pLine) + pLine.length());
			StringTokenizer sTokens = new StringTokenizer(aLine);

			int ints[] = new int[sTokens.countTokens()];
			i = 0;
			while (sTokens.hasMoreTokens()) {
				ints[i++] = Integer.parseInt(sTokens.nextToken());
			}
			numAtoms = ints[0];
			numClauses = ints[1];
			System.out.println(numAtoms + "  " + numClauses);

		}

		best = new int[MAXLENGTH];

		// nicolen
		// clause = new int[numClauses][];
		if(size == null) {
		  size = new int[numClauses];
		}
		falseClauses = new int[numClauses];
		lowFalse = new int[numClauses];
		whereFalse = new int[numClauses];
		numTrueLiterals = new int[numClauses];
		// nicolen
		numTimesBroken = new int[numClauses];
		// if(numClauses > MAXCLAUSE) {
		// System.err.println("ERROR - too many clauses");
		// throw new RuntimeException();
		// }

		numLiterals = 0;
    // nicolen
    numtry = 0;

		literalOccurences = new int[numAtoms * 2 + 1][];
		numLiteralOccurences = new int[numAtoms * 2 + 1];
		atom = new boolean[numAtoms + 1];
		lowAtom = new boolean[numAtoms + 1];
		solution = new boolean[numAtoms + 1];

		changed = new long[numAtoms + 1];
		breakCount = new int[numAtoms + 1];
		makeCount = new int[numAtoms + 1];

		if (inputFormat == this.FILE) {
			// Build clauses from a Reader
			// nicolen
			clause = new int[numClauses][];
			for (i = 0; i < numClauses; i++) {
				// Build clauses, avoiding array arithmetic when possible.
				try {
					aLine = input.readLine();
				} catch (IOException e) {
				}
				StringTokenizer sTokens = new StringTokenizer(aLine);
				int litsInClause = sTokens.countTokens() - 1;
				int aClause[] = new int[litsInClause];
				size[i] = litsInClause;
				clause[i] = aClause;
				j = 0;
				int parsedInt;
				// Parse a clause from a line of numbers
				while ((sTokens.hasMoreTokens())
						&& ((parsedInt = Integer.parseInt(sTokens.nextToken())) != 0)) {
					aClause[j++] = parsedInt;
					numLiteralOccurences[parsedInt + numAtoms]++;
				}
			}
			// nicolen
			// changed to else if
		} else if (inputFormat == this.ENUM) {
			// Build clauses from an Enumeration
			// System.out.print("Loading clauses...");
			// nicolen
			clause = new int[numClauses][];
			for (i = 0; i < numClauses; i++) {
				int aClause[] = (int[]) trieClauses.nextElement();
				size[i] = aClause.length;
				clause[i] = aClause;
				for (j = 0; j < aClause.length; j++) {
					numLiteralOccurences[aClause[j] + numAtoms]++;
				}
			}
			// System.out.println("done.");
		} else {
			// nicolen
			// clauses are already built so just
			// populate numLiteralOccurences and size
			for (i = 0; i < numClauses; i++) {
				//size[i] = clause[i].length;
				for (j = 0; j < size[i]; j++) {//clause[i].length; j++) {
					numLiteralOccurences[clause[i][j] + numAtoms]++;
				}
			}
		}

		for (i = 0; i < (2 * numAtoms + 1); i++) {
			// Allocate the arrays for each entry in literalOccurence
			literalOccurences[i] = new int[numLiteralOccurences[i]];
			// Resetting numLiteralOccurences[i] to zero seems weird at first, but
			// it is neccessary so that below, when we build literalOccurences, we
			// can keep track of what <j>
			numLiteralOccurences[i] = 0;
		}
		for (i = 0; i < numClauses; i++) {
			for (j = 0; j < size[i]; j++) {
				int literalIndex = clause[i][j] + numAtoms;
				literalOccurences[literalIndex][numLiteralOccurences[literalIndex]] = i;
				numLiteralOccurences[literalIndex]++;
			}
		}
		dataInfo.setCounts(numLiteralOccurences);
		dataInfo.setNumAtoms(numAtoms);
	}

	private void initialize_statistics() {
		tail_start_flip = tail * numAtoms;
		// System.out.println("tail starts after flip = " + tail_start_flip);
		numnullflip = 0;
	}

	private void update_statistics_start_try() {

		lowbad = numFalseClauses;

	}

	private void update_and_print_statistics_end_try() {
		totalflip += numflip;
		if (numFalseClauses == 0) {
			// --We have found a solution--
			// System.out.println("saving solution...");
			save_solution();
			numsuccesstry++;
			totalsuccessflip += numflip;
		}
		/*
		 * if(numFalseClauses == 0 && countunsat() != 0){ //
		 * System.out.println(countunsat());
		 * 
		 * System.out.println("Program error, verification of solution fails!\n"+
		 * "(no false clauses remaining, yet not all clauses "+
		 * "are verified as being satisified!)"); throw new RuntimeException(); }
		 */
	}

	private void init() {

		// Called before each try. Initialize atom with random values.
		// Then, build breakcount, etc.

		int i;
		int j;
		int thetruelit = 0;

		int lit;

		for (i = 0; i < numClauses; i++)
			numTrueLiterals[i] = 0;
		numFalseClauses = 0;

		for (i = 1; i < numAtoms + 1; i++) {
			changed[i] = -BIG;
			breakCount[i] = 0;
			makeCount[i] = 0;
		}

		for (i = 1; i < numAtoms + 1; i++) {
			int tg = ((int) (randGenerator.raw() * 2));
			atom[i] = (tg >= 1);

		}

		/* Initialize breakcount and makecount in the following: */
		for (i = 0; i < numClauses; i++) {
			for (j = 0; j < size[i]; j++) {
				if ((clause[i][j] > 0) == atom[Math.abs(clause[i][j])]) {
					numTrueLiterals[i]++;
					thetruelit = clause[i][j];
				}
			}
			// nicolen added check for clause size
			if (numTrueLiterals[i] == 0 && size[i] > 0) {
				whereFalse[i] = numFalseClauses;
				falseClauses[numFalseClauses] = i;
				// nicolen
				numTimesBroken[i]++;
				numFalseClauses++;
				for (j = 0; j < size[i]; j++) {
					makeCount[Math.abs(clause[i][j])]++;
				}
			} else if (numTrueLiterals[i] == 1) {
				breakCount[Math.abs(thetruelit)]++;
			}
		}

	}

	private int countunsat() {
		// Figure out how many unstaified clauses are left
		int i, j, unsat, lit;
		boolean bad, sign;
		unsat = 0;
		for (i = 0; i < numClauses; i++) {
			bad = true;
			for (j = 0; j < size[i]; j++) {
				lit = clause[i][j];
				sign = (lit > 0);
				if (atom[Math.abs(lit)] == sign) {
					bad = false;
					break;
				}
			}
			if (bad) {
				// System.out.println("Clauses number " + i);

				unsat++;
			}
		}
		return unsat;
	}

	private void save_false_clauses(long lowbad) {
		int i;

		for (i = 0; i < lowbad; i++)
			lowFalse[i] = falseClauses[i];
	}

	private void save_low_assign() {
		int i;

		for (i = 1; i <= numAtoms; i++)
			lowAtom[i] = atom[i];
	}

	private void save_solution() {
		int i;
		if (inputFormat == this.FILE) {
			System.out.println("writing to file...");
			try {
				String solutionHeader = "Solution #" + allSolutionsI + "\n"
						+ "-------------\n";
				output.write(solutionHeader);
				for (i = 1; i <= numAtoms; i++) {
					solution[i] = atom[i];
					if (solution[i]) {
						output.write("true\n");
					} else {
						output.write("false\n");
					}
				}
			} catch (IOException e) {
				System.err.println("problem writing to output file");
			}
		} else {
			for (i = 1; i <= numAtoms; i++)
				solution[i] = atom[i];
		}
		allSolutions[allSolutionsI++] = solution;
		// connector.addModel(solution);
		solution = new boolean[numAtoms + 1];

	}

	private int[] parseLineOfInts(String str) {

		StringTokenizer sTokens = new StringTokenizer(str);
		int ints[] = new int[sTokens.countTokens()];
		int i = 0;
		while (sTokens.hasMoreTokens()) {
			ints[i] = Integer.parseInt(sTokens.nextToken());
		}
		return ints;
	}

	private int ABS(int x) {
		return Math.abs(x);
	}

	private long superL(long i) {
		long power;
		int k;

		if (i <= 0) {
			System.err.println("bad argument super(" + i + ")");
			System.exit(1);
		}
		/* let 2^k be the least power of 2 >= (i+1) */
		k = 1;
		power = 2;
		while (power < (i + 1)) {
			k += 1;
			power *= 2;
		}
		if (power == (i + 1))
			return (power / 2);
		return (superL(i - (power / 2) + 1));
	}

	public Enumeration getSolutions() {
		// Effects: returns an enumeration of solution objects. Each solution
		// is a boolean[]

		if (allSolutions == null) {
			return null;
		}

		return new Enumeration() {
			private int i = 0;

			public boolean hasMoreElements() {
				return ((i < allSolutions.length) && (allSolutions[i] != null));
			}

			public Object nextElement() {
				if (!(hasMoreElements())) {
					throw new NoSuchElementException();
				}
				return allSolutions[i++];
			}
		};
	}

	public boolean foundSolution() {
		// Effects: returns true if at least one solution has been found
		return (allSolutions != null);
	}

	// by ueda

	public boolean[] getAssign() {
		return allSolutions[0];
	}

	// nicolen

	



	private int wp_denominator = 100;
	private int wp_numerator = NOVALUE;
	int tabu_length;

	private interface Picker {
		public int pick();
	}

	private class BestPicker implements Picker {
		public int pick() {

			// Effects: Heuristic that returns the index of the variable to flip
			// next. For this heuristic, this means either the index of the variable
			// that will break the smallest number of clauses when flipped (i.e.,
			// an uphill move), or a noisy (random) move

			// The heuristic method, along with flipAtom, is the most important
			// part of the code in terms of performance. It should be as fast as
			// possible.

			int numbreak;
			int tofix;
			int clausesize;
			int i;

			// best[] : best possibility so far (this is allocated at the problem
			// initialization stage and recycled in exchange for a big performance
			// boost)

			int numbest; /* how many are tied for best */
			int bestvalue; /* best value so far */
			int var;

			// Randomly pick a clause to fix.
			int scrt = ((int) (randGenerator.raw() * numFalseClauses));
			int toFixIndex = (scrt > 0) ? (scrt) : (-scrt);
			tofix = falseClauses[toFixIndex];

			int clauseToFix[] = clause[tofix];
			// changed to use the clause array
			// for size instead of length
			clausesize = size[tofix];

			numbest = 0;
			bestvalue = BIG;

			for (i = 0; i < clausesize; i++) {
				// Find the best variable in the clause to flip. (the one with the
				// lowest breakCount)

				// This is faster than calling Math.abs
				int clv = clauseToFix[i];
				var = (clv > 0) ? (clv) : (-clv);

				numbreak = breakCount[var];
				if (numbreak <= bestvalue) {
					// If this is a tie, add it to best. If it beats those in best,
					// resent best and put this one at the beginning
					if (numbreak < bestvalue)
						numbest = 0;
					bestvalue = numbreak;
					best[numbest++] = var;
				}
			}

			// Pick a random number to determine if we should make a random move.
			int pv = ((int) (randGenerator.raw() * denominator));
			pv = (pv < 0) ? -pv : pv;
			if (bestvalue > 0 && (pv < numerator)) {
				// Randomly flip a variable in the clause. Note that if the best
				// flip breaks no clauses, then the random flip will not be chosen,
				// no matter what random value comes up above.

				int randReturn = ((int) (randGenerator.raw() * clausesize));
				;
				int randIndx = (randReturn > 0) ? randReturn : -randReturn;
				int randVarToEnforce = clauseToFix[randIndx];
				return (randVarToEnforce > 0) ? randVarToEnforce : -randVarToEnforce;
			}
			// Choose the best one to flip, or, if there are ties for the best,
			// randomly pick one of those ties.
			if (numbest == 1) {
				return best[0];
			}
			int scrt2 = ((int) (randGenerator.raw() * numbest));
			int randBestIndx = (scrt2 > 0) ? scrt2 : -scrt2;
			return best[randBestIndx];
		}
	}

	private class TabuPicker implements Picker {
		public int pick() {
			int numbreak[] = new int[MAXLENGTH];
			int tofix;
			int clausesize;
			int i; /* a loop counter */
			int best[] = new int[MAXLENGTH]; /* best possibility so far */
			int numbest; /* how many are tied for best */
			int bestvalue; /* best value so far */
			boolean noisypick;

			tofix = falseClauses[(int) randGenerator.raw() % numFalseClauses];
			clausesize = size[tofix];
			for (i = 0; i < clausesize; i++)
				numbreak[i] = breakCount[ABS(clause[tofix][i])];

			numbest = 0;
			bestvalue = BIG;

			noisypick = (numerator > 0 && randGenerator.raw() % denominator < numerator);
			for (i = 0; i < clausesize; i++) {
				if (numbreak[i] == 0) {
					if (bestvalue > 0) {
						bestvalue = 0;
						numbest = 0;
					}
					best[numbest++] = i;
				} else if (tabu_length < numflip - changed[ABS(clause[tofix][i])]) {
					if (noisypick && bestvalue > 0) {
						best[numbest++] = i;
					} else {
						if (numbreak[i] < bestvalue) {
							bestvalue = numbreak[i];
							numbest = 0;
						}
						if (numbreak[i] == bestvalue) {
						}
					}
				}
			}
			if (numbest == 0)
				return NOVALUE;
			if (numbest == 1)
				// return Var(tofix, best[0]);
				return clause[tofix][best[0]];
			// return (Var(tofix, best[(int)randGenerator.raw()%numbest]));

			return clause[tofix][best[(int) randGenerator.raw() % numbest]];
		}
	}
	
	private class RNoveltyPlusPicker implements Picker {
		public int pick() {
			long birthdate, youngest_birthdate;
			int var, diff, diffdiff, best_diff, second_best_diff;
			int tofix, clausesize, i;
			int best = 0, second_best = 0, youngest = 0;

			tofix = falseClauses[(int) randGenerator.raw() % numFalseClauses];
			clausesize = size[tofix];

			if (clausesize == 1)
				return ABS(clause[tofix][0]);

			/* hh: modified loop breaker: */
			if ((randGenerator.raw() % wp_denominator < wp_numerator))
				return ABS(clause[tofix][(int) randGenerator.raw() % clausesize]);

			/*
			 * if ((numflip % 100) == 0) return ABS(clause[tofix][random()%clausesize]);
			 */

			youngest_birthdate = -1;
			best_diff = -BIG;
			second_best_diff = -BIG;

			for (i = 0; i < clausesize; i++) {
				var = ABS(clause[tofix][i]);
				diff = makeCount[var] - breakCount[var];
				birthdate = changed[var];
				if (birthdate > youngest_birthdate) {
					youngest_birthdate = birthdate;
					youngest = var;
				}
				if (diff > best_diff
						|| (diff == best_diff && changed[var] < changed[best])) {
					/* found new best, demote best to 2nd best */
					second_best = best;
					second_best_diff = best_diff;
					best = var;
					best_diff = diff;
				} else if (diff > second_best_diff
						|| (diff == second_best_diff && changed[var] < changed[second_best])) {

					/* found new second best */
					second_best = var;
					second_best_diff = diff;
				}
			}
			if (best != youngest)
				return best;

			diffdiff = best_diff - second_best_diff;

			/*
			 * if (numerator < 50 && diffdiff > 1) return best; if (numerator < 50 &&
			 * diffdiff == 1){
			 */

			if (numerator * 2 < denominator && diffdiff > 1)
				return best;
			if (numerator * 2 < denominator && diffdiff == 1) {
				if ((randGenerator.raw() % denominator) < 2 * numerator)
					return second_best;
				return best;
			}
			if (diffdiff == 1)
				return second_best;

			/*
			 * if ((random()%denominator) < 2*(numerator-50)) return second_best;
			 */

			if ((randGenerator.raw() % denominator) < 2 * (numerator - (denominator / 2)))
				return second_best;

			return best;
		}
	}
	
	private class NoveltyPlusPicker implements Picker {
		public int pick() {
			long birthdate, youngest_birthdate;
			int var, diff, best_diff, second_best_diff;
			int tofix, clausesize, i;
			int best = 0, second_best = 0, youngest = 0;

			tofix = falseClauses[(int) randGenerator.raw() % numFalseClauses];
			clausesize = size[tofix];

			if (clausesize == 1)
				return ABS(clause[tofix][0]);

			/* hh: inserted modified loop breaker: */
			if ((randGenerator.raw() % wp_denominator < wp_numerator))
				return ABS(clause[tofix][(int) randGenerator.raw() % clausesize]);

			youngest_birthdate = -1;
			best_diff = -BIG;
			second_best_diff = -BIG;

			for (i = 0; i < clausesize; i++) {
				var = ABS(clause[tofix][i]);
				diff = makeCount[var] - breakCount[var];
				birthdate = changed[var];
				if (birthdate > youngest_birthdate) {
					youngest_birthdate = birthdate;
					youngest = var;
				}
				if (diff > best_diff
						|| (diff == best_diff && changed[var] < changed[best])) {
					/* found new best, demote best to 2nd best */
					second_best = best;
					second_best_diff = best_diff;
					best = var;
					best_diff = diff;
				} else if (diff > second_best_diff
						|| (diff == second_best_diff && changed[var] < changed[second_best])) {
					/* found new second best */
					second_best = var;
					second_best_diff = diff;
				}
			}
			if (best != youngest)
				return best;
			if ((randGenerator.raw() % denominator < numerator))
				return second_best;
			return best;
		}
	}
	
	private class RNoveltyPicker implements Picker {
		public int pick() {
			long birthdate, youngest_birthdate;
			int var, diff, diffdiff, best_diff, second_best_diff;
			int tofix, clausesize, i;
			int best = 0, second_best = 0, youngest = 0;

			tofix = falseClauses[(int) randGenerator.raw() % numFalseClauses];
			clausesize = size[tofix];

			if (clausesize == 1)
				return ABS(clause[tofix][0]);
			if ((numflip % 100) == 0)
				return ABS(clause[tofix][(int) randGenerator.raw() % clausesize]);

			youngest_birthdate = -1;
			best_diff = -BIG;
			second_best_diff = -BIG;

			for (i = 0; i < clausesize; i++) {
				var = ABS(clause[tofix][i]);
				diff = makeCount[var] - breakCount[var];
				birthdate = changed[var];
				if (birthdate > youngest_birthdate) {
					youngest_birthdate = birthdate;
					youngest = var;
				}
				if (diff > best_diff
						|| (diff == best_diff && changed[var] < changed[best])) {
					/* found new best, demote best to 2nd best */
					second_best = best;
					second_best_diff = best_diff;
					best = var;
					best_diff = diff;
				} else if (diff > second_best_diff
						|| (diff == second_best_diff && changed[var] < changed[second_best])) {
					/* found new second best */
					second_best = var;
					second_best_diff = diff;
				}
			}
			if (best != youngest)
				return best;
			diffdiff = best_diff - second_best_diff;

			/*
			 * if (numerator < 50 && diffdiff > 1) return best; if (numerator < 50 &&
			 * diffdiff == 1){
			 */

			if (numerator * 2 < denominator && diffdiff > 1)
				return best;
			if (numerator * 2 < denominator && diffdiff == 1) {
				if ((randGenerator.raw() % denominator) < 2 * numerator)
					return second_best;
				return best;
			}
			if (diffdiff == 1)
				return second_best;

			/*
			 * if ((random()%denominator) < 2*(numerator-50)) return second_best;random
			 */

			if ((randGenerator.raw() % denominator) < 2 * (numerator - (denominator / 2)))
				return second_best;

			return best;
		}
	}
	
	private class RandomPicker implements Picker {
		public int pick() {
			int tofix;
			tofix = falseClauses[(int) randGenerator.raw() % numFalseClauses];
			// return Var(tofix, (int) randGenerator.raw() % size[tofix]);
			return clause[tofix][(int) randGenerator.raw() % size[tofix]];
		}
	}
	
	private class NoveltyPicker implements Picker {
		public int pick() {
			long birthdate, youngest_birthdate;
			int var, diff;
			int best_diff, second_best_diff;
			int tofix, clausesize, i;
			int best = 0, youngest = 0, second_best = 0;

			tofix = falseClauses[(int) randGenerator.raw() % numFalseClauses];
			clausesize = size[tofix];

			if (clausesize == 1)
				return ABS(clause[tofix][0]);

			youngest_birthdate = -1;
			best_diff = -BIG;
			second_best_diff = -BIG;

			for (i = 0; i < clausesize; i++) {
				var = ABS(clause[tofix][i]);
				diff = makeCount[var] - breakCount[var];
				birthdate = changed[var];
				if (birthdate > youngest_birthdate) {
					youngest_birthdate = birthdate;
					youngest = var;
				}
				if (diff > best_diff
						|| (diff == best_diff && changed[var] < changed[best])) {
					/* found new best, demote best to 2nd best */
					second_best = best;
					second_best_diff = best_diff;
					best = var;
					best_diff = diff;
				} else if (diff > second_best_diff
						|| (diff == second_best_diff && changed[var] < changed[second_best])) {
					/* found new second best */
					second_best = var;
					second_best_diff = diff;
				}
			}
			if (best != youngest)
				return best;
			if ((randGenerator.raw() % denominator < numerator))
				return second_best;
			return best;
		}
	}
}
