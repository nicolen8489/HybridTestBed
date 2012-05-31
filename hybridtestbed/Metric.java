package hybridtestbed;

public interface Metric extends Comparable<Metric> {
	// this value should be set to a unique name
	// inside the implemented metric
	public static String NAME = "Metric";
	public int compareTo(Metric m);
}
