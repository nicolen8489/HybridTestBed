package hybridtestbed;

public class First10Ambivalence extends Ambivalence {

	private double[] activities;
	private int[] vals;

	public First10Ambivalence(double ambProb, int scoutProb, HybridHeap heap) {
		super(ambProb, scoutProb, heap);
		activities = new double[10];
		vals = new int[10];
	}

	@Override
	public boolean isAmbivalent() {
		if (heap.hasActivityValues()) {
			isAmbivalentCalls++;

			//if (random.nextInt(scoutProb) == 0) {
				//evaluateAmbivalence++;
				double sum = 0;
				int numItems = 0;
				for (int i = 0; i < 10 && !heap.empty(); i++) {
					vals[i] = heap.getmin();
					activities[i] = heap.getActivity(vals[i]);
					sum += activities[i];
					if (i > 0 && activities[i] > activities[i - 1]) {
						System.out.println(activities[i] + " " + activities[i - 1]);
						System.exit(1);
					}
					numItems++;
				}
				for (int j = 0; j < numItems; j++) {
					activities[j] = activities[j] / sum;
					heap.insert(vals[numItems - j - 1]);
				}
				if (activities[0] < ambProb) {
//					this.timesAmbivalent++;
//					return true;
					evaluateAmbivalence++;
					boolean returnVal = random.nextInt(scoutProb) == 0;
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
					// return random.nextInt(scoutProb) == 0;
				}
			//}
		}
		return false;
	}

}
