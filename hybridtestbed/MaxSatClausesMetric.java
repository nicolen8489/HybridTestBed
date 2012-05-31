package hybridtestbed;

public class MaxSatClausesMetric implements Metric {

	private int maxSatClauses = Integer.MIN_VALUE;
	private int literalAssign;
	
	public MaxSatClausesMetric(int literalTest) {
		this.literalAssign = literalTest;
	}
	
	public void updatedMaxSatClauses(int numSatClauses) {
		if(numSatClauses > maxSatClauses) {
			maxSatClauses = numSatClauses;
		}
	}
	
	public int getMaxSatClauses() {
		return maxSatClauses;
	}
	
	@Override
	public int compareTo(Metric m) {
		if(m instanceof MaxSatClausesMetric) {
			if(this.maxSatClauses > ((MaxSatClausesMetric)m).getMaxSatClauses()) {
				return 1;
			} else if (this.maxSatClauses < ((MaxSatClausesMetric)m).getMaxSatClauses()) {
				return -1;
			}
		}
		return 0;
	}

}
