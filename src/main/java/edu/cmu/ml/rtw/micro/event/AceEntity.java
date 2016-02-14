package edu.cmu.ml.rtw.micro.event;
import edu.cmu.ml.rtw.generic.util.Pair;

public class AceEntity {
	public String entityId;
	public String entityType;
	public Pair<Integer, Integer> charSpan = new Pair<Integer, Integer>(0,0);
	public String spanStr;
	
	public String toString() {
		return entityId + "\t" + entityType + " " + charSpan.getFirst().toString() + 
				" " + charSpan.getSecond().toString() + "\t" + spanStr;
	}
}
