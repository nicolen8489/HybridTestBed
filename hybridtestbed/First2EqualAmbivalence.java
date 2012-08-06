package hybridtestbed;

public class First2EqualAmbivalence extends Ambivalence {

	private double[] activities;
	private int[] vals;

	public First2EqualAmbivalence(double ambProb, int scoutProb, HybridHeap heap) {
		super(ambProb, scoutProb, heap);
		activities = new double[10];
		vals = new int[10];
	}

	@Override
	public boolean isAmbivalent(DataInfo dataInfo) {
		if (heap.hasActivityValues() &&
				(maintainPercent && 
				(dataInfo.getWalksatTime() / (System.currentTimeMillis() - dataInfo.getSolverStartTime())) * 100 < percent
				|| !maintainPercent)) {
			isAmbivalentCalls++;
			//if (random.nextInt(scoutProb) == 0) {
			//System.out.println("check equality");
				if (heap.size() >= 2
						&& ((activities[0] = heap.getActivity(vals[0] = heap.getmin())) == (activities[1] = heap
								.getActivity(vals[1] = heap.getmin())))) {
					//evaluateAmbivalence++;
					//System.out.println("passed equality check");
					//System.out.println(activities[0] + " -- " + activities[1]);
					double sum = 0;
					int numItems = 2;
					sum += activities[0] + activities[1];
					for (int i = 2; i < 10 && heap.size() > 0; i++) {
						vals[i] = heap.getmin();
						activities[i] = heap.getActivity(vals[i]);
						//System.out.println(activities[i]);
						sum += activities[i];
						if (activities[i] > activities[i - 1]) {
							System.exit(1);
						}
						numItems++;
					}
					//System.out.println("normalized values " + sum);
					for (int j = 0; j < numItems; j++) {
						activities[j] = activities[j] / sum;
						//System.out.println(activities[j]);
						heap.insert(vals[numItems - j - 1]);
					}
					
					if (activities[0] < ambProb) {
						// return random.nextInt(scoutProb) == 0;
						evaluateAmbivalence++;
						boolean returnVal = random.nextInt(scoutProb) == 0;
						//System.out.println("less than ambProb " + returnVal);
						firstGuy += activities[0];
						if (activities[0] < minFirstGuy) {
							minFirstGuy = activities[0];
						}
						if (activities[0] > maxFirstGuy) {
							maxFirstGuy = activities[0];
						}
						if (activities[0] < 0.099) {
							lessThan++;
						} else if (activities[0] >= ambProb) {
							greaterThan++;
						} else if (activities[0] >= 0.099 && activities[0] < firstRange) {
							firstQuartile++;
						} else if (activities[0] >= firstRange && activities[0] < secondRange) {
							secondQuartile++;
						} else if (activities[0] >= secondRange && activities[0] < thirdRange) {
							thirdQuartile++;
						} else if (activities[0] >= thirdRange && activities[0] < ambProb) {
							fourthQuartile++;
						}
						if(returnVal) {
							timesAmbivalent++;
						}
						return returnVal;
						//return true;
					}
				} else {
					heap.insert(vals[0]);
					heap.insert(vals[1]);
				}
			//}
		}
		return false;
	}

}
