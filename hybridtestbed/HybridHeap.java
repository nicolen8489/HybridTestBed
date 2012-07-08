package hybridtestbed;

/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004-2008 Denial Le Berre
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
import java.io.Serializable;

import org.sat4j.core.VecInt;
import org.sat4j.specs.IVecInt;

/**
 * Heap implementation used to maintain the variables order in some heuristics.
 * 
 * @author daniel
 * 
 */
public final class HybridHeap implements Serializable {

	/*
	 * default serial version id
	 */
	private static final long serialVersionUID = 1L;

	int size = 0;

	private static final int left(int i) {
		return i << 1;
	}

	private static final int right(int i) {
		return (i << 1) ^ 1;
	}

	private static final int parent(int i) {
		return i >> 1;
	}

	private final boolean comp(int a, int b) {
		return activity[a] > activity[b];
	}

	private final IVecInt heap = new VecInt(); // heap of ints

	private final IVecInt indices = new VecInt(); // int -> index in heap

	private final double[] activity;

	final void percolateUp(int i) {
		int x = heap.get(i);
		while (parent(i) != 0 && comp(x, heap.get(parent(i)))) {
			heap.set(i, heap.get(parent(i)));
			indices.set(heap.get(i), i);
			i = parent(i);
		}
		heap.set(i, x);
		indices.set(x, i);
	}

	final void percolateDown(int i) {
		int x = heap.get(i);
		while (left(i) < heap.size()) {
			int child = right(i) < heap.size()
					&& comp(heap.get(right(i)), heap.get(left(i))) ? right(i) : left(i);
			if (!comp(heap.get(child), x))
				break;
			heap.set(i, heap.get(child));
			indices.set(heap.get(i), i);
			i = child;
		}
		heap.set(i, x);
		indices.set(x, i);
	}

	boolean ok(int n) {
		return n >= 0 && n < indices.size();
	}

	public HybridHeap(double[] activity) { // NOPMD
		this.activity = activity;
		heap.push(-1);
	}

	public void setBounds(int size) {
		assert (size >= 0);
		indices.growTo(size, 0);
	}

	public boolean inHeap(int n) {
		assert (ok(n));
		return indices.get(n) != 0;
	}

	public void increase(int n) {
		assert (ok(n));
		assert (inHeap(n));
		percolateUp(indices.get(n));

		if (heap.size() > 0) {
			int i = indexOf(n);
			if (parent(i) != 0 && activity[n] > activity[heap.get(parent(i))]) {
				System.out.println("incorrect increase " + n);
				System.exit(1);
			}
			if (left(i) != 0 && left(i) < heap.size()
					&& activity[n] < activity[heap.get(left(i))]) {
				System.out.println("incorrect increase " + n);
				System.exit(1);
			}
			if (right(i) != 0 && right(i) < heap.size()
					&& activity[n] < activity[heap.get(right(i))]) {
				System.out.println("incorrect increase " + n);
				System.exit(1);
			}
		}
		// validate();
	}

	public boolean empty() {
		return heap.size() == 1;
	}

	public int size() {
		return heap.size() - 1;
	}

	public int get(int i) {
		int r = heap.get(i);
		int n = heap.get(i);
		if (i == indexOf(heap.last())) {
			indices.set(r, 0);
			heap.pop();
			// heap.set(i, indexOf(heap.last()) - 1);
		} else {
			heap.set(i, heap.last());
			n = heap.get(i);
			indices.set(heap.get(i), i);
			indices.set(r, 0);
			heap.pop();
			if (heap.size() > 1) { // > i) { // 1
				if (activity[heap.get(i)] > activity[r]) {
					percolateUp(i);
				} else {
					percolateDown(i);
				}
			}
		}
		// if (heap.size() > 1) { // > i) { // 1
		// if (activity[heap.get(i)] > activity[r]) {
		// percolateUp(i);
		// } else {
		// percolateDown(i);
		// }
		// }
		/*
		 * if(heap.size() > i) { percolateDown(i); }
		 */
		if (heap.size() > 0 && (i = indexOf(n)) != 0) {
			if (parent(i) != 0 && activity[n] > activity[heap.get(parent(i))]) {
				System.out.println("incorrect get " + n);
				System.exit(1);
			}
			if (left(i) != 0 && left(i) < heap.size()
					&& activity[n] < activity[heap.get(left(i))]) {
				System.out.println("incorrect get " + n);
				System.exit(1);
			}
			if (right(i) != 0 && right(i) < heap.size()
					&& activity[n] < activity[heap.get(right(i))]) {
				System.out.println("incorrect get " + n);
				System.exit(1);
			}
		}

		// validate();
		return r;
	}

	// nicolen
	public int last() {
		return heap.last();
	}

	public int first() {
		return heap.get(1);
	}

	public void insert(int n) {
		assert (ok(n));
		indices.set(n, heap.size());
		heap.push(n);
		// if(heap.size() <= 10) {
		// topTen[heap.size() - 1] = n;
		// }
		percolateUp(indices.get(n));
		if (heap.size() > 0) {
			int i = indexOf(n);
			if (parent(i) != 0 && activity[n] > activity[heap.get(parent(i))]) {
				System.out.println("incorrect insert " + n);
				System.exit(1);
			}
			if (left(i) != 0 && left(i) < heap.size()
					&& activity[n] < activity[heap.get(left(i))]) {
				System.out.println("incorrect insert " + n);
				System.exit(1);
			}
			if (right(i) != 0 && right(i) < heap.size()
					&& activity[n] < activity[heap.get(right(i))]) {
				System.out.println("incorrect insert " + n);
				System.exit(1);
			}
		}

	}

	public int getmin() {
		return get(1);
	}

	public boolean heapProperty() {
		return heapProperty(1);
	}

	public boolean heapProperty(int i) {
		return i >= heap.size()
				|| ((parent(i) == 0 || !comp(heap.get(i), heap.get(parent(i))))
						&& heapProperty(left(i)) && heapProperty(right(i)));
	}

	// nicolen
	public double getActivity(int literal) {
		return activity[literal];
	}

	public int indexOf(int variable) {
		return indices.get(variable);
	}

	// public boolean validate() {
	// for(int i = 1; i < heap.size(); i++) {
	// int left = left(i);
	// int right = right(i);
	// if(left >= heap.size() && right >= heap.size()) {
	// break;
	// }
	// if(left < heap.size() && activity[heap.get(left)] > activity[heap.get(i)]
	// || right < heap.size() && activity[heap.get(right)] >
	// activity[heap.get(i)]) {
	// System.out.println("heap does not validate");
	// System.exit(1);
	// return false;
	// }
	// }
	// return true;
	// }

	public boolean hasActivityValues() {
		// boolean returnVal = false;
		// if(heap != null && !heap.empty()) {
		// int val = heap.getmin();
		// if(heap.getActivity(val) != 0.0) {
		// returnVal = true;
		// }
		// heap.insert(val);
		// }
		// return returnVal;
		return heap.size() > 0 && activity[heap.get(1)] != 0.0;
	}

}
