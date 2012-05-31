package hybridtestbed;

public class DataInfo {
  private int maxSatClause;
  private int localMinCount;
  private int localMinSatClauseSum;
  private int hardestToSatisfy;
  private boolean[] solution;
  public DataInfo() {
    maxSatClause = 0;
  }
  public void setMaxSatClause(int maxSatClause) {
    this.maxSatClause = maxSatClause;
  }
  public int getMaxSatClause() {
    return maxSatClause;
  }
  public double getAvgLocalMinSatClauses() {
    return (double)localMinCount / localMinSatClauseSum;
  }
  public void setHardestToSatisfyClause(int i) {
  	hardestToSatisfy = i;
  }
  public int getHardestToSatisfyClause() {
  	return hardestToSatisfy;
  }
  public void setSolution(boolean[] solution) {
    this.solution = solution;
  }
  public boolean[] getSolution() {
    if(solution == null) {
      return new boolean[0];
    }
    return solution;
  }
}
