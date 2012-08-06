package hybridtestbed;

public class InverseProbAmbivalence extends Ambivalence {
	
	private double[] activities;
	private int[] vals;
	private int slope;
	private int offset;
	
	public InverseProbAmbivalence(double ambProb, int scoutProb, HybridHeap heap, int lowestProb) {
		super(ambProb, scoutProb, heap);
		activities = new double[10];
		vals = new int[10];
		slope = (int) ((lowestProb - scoutProb) / (0.1 - ambProb));
		offset = (int) (lowestProb - slope * 0.1);
	}
	
	public void setLowestProb(int lowestProb) {
		slope = (int) ((lowestProb - scoutProb) / (0.1 - ambProb));
		offset = (int) (lowestProb - slope * 0.1);		
	}
	
	public int getProb(double val) {
		//double d = val - 0.1;
		return (int)(slope * val + offset);
	}
	
	@Override
	public boolean isAmbivalent(DataInfo dataInfo) {
		if (heap.hasActivityValues() &&
				(maintainPercent && 
				(dataInfo.getWalksatTime() / (System.currentTimeMillis() - dataInfo.getSolverStartTime())) * 100 < percent
				|| !maintainPercent)) {
			isAmbivalentCalls++;
			if (heap.size() >= 2
					&& ((activities[0] = heap.getActivity(vals[0] = heap.getmin())) 
							== (activities[1] = heap.getActivity(vals[1] = heap.getmin())))) {
				evaluateAmbivalence++;
				double sum = 0;
				int numItems = 2;
				sum += activities[0] + activities[1];
				for (int i = 2; i < 10 && !heap.empty(); i++) {
					vals[i] = heap.getmin();
					activities[i] = heap.getActivity(vals[i]);
					sum += activities[i];
					if(activities[i] > activities[i - 1]) {
						System.out.println(activities[i] + " " + activities[i-1]);
						System.exit(1);
					}
					numItems++;
				}
				for (int j = 0; j < numItems; j++) {
					activities[j] = activities[j] / sum;
					heap.insert(vals[numItems - j - 1]);
				}
//				firstGuy += activities[0];
//				if (activities[0] < minFirstGuy) {
//					minFirstGuy = activities[0];
//				}
//				if (activities[0] > maxFirstGuy) {
//					maxFirstGuy = activities[0];
//				}
//				if (activities[0] < 0.099) {
//					lessThan++;
//				} else if (activities[0] >= 0.15) {
//					greaterThan++;
//				} else if (activities[0] >= 0.099 && activities[0] < 0.102375) {
//					// 0.003375
//					lowCount++;
//				} else if (activities[0] >= 0.102375 && activities[0] < 0.10575) {
//					// 0.003375
//					midLowCount++;
//				} else if (activities[0] >= 0.10575 && activities[0] < 0.109125) {
//					// 0.003375
//					midLowCount2++;
//				} else if (activities[0] >= 0.109125 && activities[0] < 0.1125) {
//					// 0.003375
//					highLowCount++;
//				} else if (activities[0] >= 0.1125 && activities[0] < 0.125) {
//					firstMidCount++;
//				} else if (activities[0] >= 0.125 && activities[0] < 0.1375) {
//					secondMidCount++;
//				} else if (activities[0] >= 0.1375 && activities[0] < 0.15) {
//					highCount++;
//				}
				if (activities[0] < ambProb) {
					evaluateAmbivalence++;
					int prob = getProb(activities[0]);
					boolean returnVal = random.nextInt(prob) == 0;
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
//					timesAmbivalent++;
//					int val = getProb(activities[0]);
//					if(val < 0) {
//						System.out.println("slope " + slope);
//						System.out.println("offset " + offset);
//						System.out.println("scoutProb " + scoutProb);
//						System.out.println("ambProb " + ambProb);
//						System.out.println("activity: " + activities[0]);
//					}
//					if(random.nextInt(getProb(activities[0])) == 0) {
//						this.timesAmbivalent++;
//						return true;
//					}
//					//return random.nextInt(getProb(activities[0])) == 0;
//					return false;
				}
			} else {
				heap.insert(vals[1]);
				heap.insert(vals[0]);
			}
		}
		return false;
	}
}
