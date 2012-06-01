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
                  && comp(heap.get(right(i)), heap.get(left(i))) ? right(i)
                  : left(i);
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
  }

  public boolean empty() {
    return heap.size() == 1;
  }

  public int size() {
    return heap.size() - 1;
  }

  public int get(int i) {
    int r = heap.get(i);
    heap.set(i, heap.last());
    indices.set(heap.get(i), i);
    indices.set(r, 0);
    heap.pop();
    if (heap.size() > 1)
      percolateDown(1);
    return r;
  }

  public void insert(int n) {
    assert (ok(n));
    indices.set(n, heap.size());
    heap.push(n);
    percolateUp(indices.get(n));
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
  
  public boolean hasActivityValues() {
  	if(activity[heap.get(1)] != 0.0) {
  		return true;
  	}
  	return false;
  }
  
  public boolean isAmbivalent() {
/*  	double val = activity[heap.get(1)];
  	double fraction = heap.size() / 1000;
  	//for(int i = 2; i <= heap.size() / 10; i++) {
  		if(val - activity[heap.get((int)fraction)] > fraction) {
  			return true;
  		}
  	//}*/
  	int val1 = this.getmin();
  	int val2 = this.getmin();
		this.insert(val1);
		this.insert(val2);
  	if(activity[val1] == activity[val2]) {
  		return true;
  	}
  	return false;
  }
  
  public void first10Activities() {
  	int[] vals = new int[10];
  	for(int i = 1; i <= 10; i++) {
  		int val = this.getmin();
  		vals[i-1] = val;
  		//System.out.println(heap.get(i) + " --- " + activity[heap.get(i)]);
  		System.out.println(activity[val]);
  	}
  	for(int i = 0; i < 10; i++) {
  		this.insert(vals[i]);
  	}
  }

  // nicolen
  public int variablesWithActivity(double activityVar, int[] variables, int i, int count) {
    if (count == 0) {
      activityVar = activity[heap.get(1)];
      variables[count++] = heap.get(1);
    }
    if (left(i) < heap.size() && activity[heap.get(left(i))] == activityVar) {
      variables[count++] = heap.get(left(i));
      int j = left(i);
      count = variablesWithActivity(activityVar, variables, j, count);
    }
    if (right(i) < heap.size() && activity[heap.get(right(i))] == activityVar) {
      variables[count++] = heap.get(right(i));
      int j = right(i);
      count = variablesWithActivity(activityVar, variables, j, count);
    }
    return count;
  }

  public int indexOf(int variable) {
    return indices.get(variable);
  }

}
