package edu.cmu.ml.rtw.micro.event;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.util.Pair;

public class AceEvent {
	public String triggerId;
	public String eventId;
	public String eventType;
	public Pair<Integer, Integer> charSpan = new Pair<Integer, Integer>(0,0);
	public String spanStr;
	
	public class EventArgument {
		String entityId;
		String roleType;
		
		public EventArgument(String ID, String Role) {
			entityId = ID;
			roleType = Role;
		}
		
		public String toString() {
			return roleType + ":" + entityId;
		}
	}
	
	public List<EventArgument> eventArgs = new ArrayList<EventArgument>();
	
	public void AddArgument(String EntityID, String RoleType) {
		eventArgs.add(new EventArgument(EntityID, RoleType));
	}
	
	public String toTriggerString() {
		return triggerId + "\t" + eventType + " " + charSpan.getFirst().toString() 
				+ " " + charSpan.getSecond().toString() + "\t" + spanStr;
	}
	
	public String toEventString() {
		String str = eventId + "\t" + eventType + ":"  + triggerId;
		for (EventArgument arg : eventArgs) {
			str += " " + arg.toString();
		}
		return str;
	}
}

