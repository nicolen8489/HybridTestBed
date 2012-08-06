package hybridtestbed;

import java.util.Random;

public abstract class Ambivalence {
	public Random random;
	public double ambProb;
	public int scoutProb;
	public HybridHeap heap;

	double isAmbivalentCalls = 0;
	int timesAmbivalent = 0;
	double firstGuy = 0;
	double minFirstGuy = Double.MAX_VALUE;
	double maxFirstGuy = Double.MIN_VALUE;
	int firstQuartile = 0;
	int secondQuartile = 0;
	int thirdQuartile = 0;
	int fourthQuartile = 0;
	int lessThan = 0;
	int greaterThan = 0;
	double evaluateAmbivalence = 0;
	double firstRange;
	double secondRange;
	double thirdRange;
	boolean maintainPercent;
	double percent = 5;

	public String getAmbivalenceData() {
		String returnString = "";
		if (isAmbivalentCalls > 0) {
			returnString += "\npercentAmb "
					+ ((timesAmbivalent / isAmbivalentCalls) * 100.0);
			if (evaluateAmbivalence > 0) {
				returnString += "\navgFirst " + (firstGuy / evaluateAmbivalence);
				returnString += "\nminFirst " + minFirstGuy;
				returnString += "\nmaxFirst " + maxFirstGuy;
				returnString += "\nfirstQuartile " + (firstQuartile / evaluateAmbivalence) * 100.0;
				returnString += "\nsecondQuartile " + (secondQuartile / evaluateAmbivalence) * 100.0;
				returnString += "\nthirdQuartile " + (thirdQuartile / evaluateAmbivalence) * 100.0;
				returnString += "\nfourthQuartile " + (fourthQuartile / evaluateAmbivalence) * 100.0;
				returnString += "\nlessThan " + (lessThan / evaluateAmbivalence) * 100.0;
				returnString += "\ngreaterThan " + (greaterThan / evaluateAmbivalence) * 100.0;
			} else {
				returnString += "\navgFirst 0";
				returnString += "\nminFirst 0";
				returnString += "\nmaxFirst 0";
				returnString += "\nfirstQuartile 0";
				returnString += "\nsecondQuartile 0";
				returnString += "\nthirdQuartile 0";
				returnString += "\nfourthQuartile 0";
				returnString += "\nlessThan 0";
				returnString += "\ngreaterThan 0";
			}
		} else {
			returnString += "\npercentAmb 0";
		}
		return returnString;
	}

	public Ambivalence(double ambProb, int scoutProb, HybridHeap heap) {
		long seed = System.currentTimeMillis();
		//seed = Long.parseLong("1341793769559");
		random = new Random(seed);
		System.out.println("ambivalence seed " + seed);
		this.ambProb = ambProb;
		double quartile = (ambProb - 0.099) / 4;
		firstRange = 0.099 + quartile;
		secondRange = firstRange + quartile;
		thirdRange = secondRange + quartile;
		this.scoutProb = scoutProb;
		this.heap = heap;
	}

	public void setAmbProb(double ambProb) {
		this.ambProb = ambProb;
		double quartile = (ambProb - 0.099) / 4;
		firstRange = 0.099 + quartile;
		secondRange = firstRange + quartile;
		thirdRange = secondRange + quartile;
	}

	public double getAmbProb() {
		return ambProb;
	}

	public void setScoutProb(int scoutProb) {
		this.scoutProb = scoutProb;
	}

	public int getScoutProb() {
		return scoutProb;
	}

	public void setHeap(HybridHeap heap) {
		this.heap = heap;
	}

	public abstract boolean isAmbivalent(DataInfo dataInfo);
}
