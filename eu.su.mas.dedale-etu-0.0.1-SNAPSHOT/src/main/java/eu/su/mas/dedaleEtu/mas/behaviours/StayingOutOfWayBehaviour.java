package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.CollectorAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMultiMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GetOutOfMyWayBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * This behaviour is used to avoid blocking other agents when they are still active
 * It only looks out for GET-OUT-OF-MY-WAY messages from other agents, 
 */
public class StayingOutOfWayBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 1L;

	private static final int TIMER = 1000; // Time to wait between actions (in milliseconds)
	
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation goldMap; // Gold map, gold nodes with remaining gold are open
	private MapRepresentation diamondMap; // Diamond map, diamond nodes are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open

	private List<String> receivers; // List of agents to send maps to

	public StayingOutOfWayBehaviour(final AbstractDedaleAgent myagent, MapRepresentation exploreMap, 
																	MapRepresentation goldMap, 
																	MapRepresentation diamondMap, 
																	MapRepresentation tankerMap, 
																	List<String> receivers) {
		super(myagent);
		
		// =========== To disable the GraphStream UI ==================
		// System.setProperty("org.graphstream.ui", "null");
		// ============================================================

		this.exploreMap = exploreMap;
		this.goldMap = goldMap;
		this.diamondMap = diamondMap;
		this.tankerMap = tankerMap;

		this.receivers = receivers; // List of agents to send maps to
	}
	
	@Override
	public void action() {
		AbstractDedaleAgent me = (AbstractDedaleAgent) this.myAgent;
		if (me == null) return;
		//System.out.println();
		//System.out.println(this.myAgent.getLocalName() + " - NEW ACTION - CURRENT POSITION: " + (me.getCurrentPosition().getLocationId()));
		//System.out.println();

		// Current position
		Location myPosition = me.getCurrentPosition();
		if (myPosition == null) return;

		/**
		* Just added here to let you see what the agent is doing, otherwise he will be too quick
		*/
		try {
			this.myAgent.doWait(TIMER);
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		String myPositionId = myPosition.getLocationId();

		// 1) Observe
		List<Couple<Location, List<Couple<Observation, String>>>> lobs = me.observe();
		List<Couple<Observation, String>> here = lobs.get(0).getRight();

		// ===================== RECEIVE GET-OUT-OF-MY-WAY ==============================
		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("GET-OUT-OF-MY-WAY"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
		if (msgReceived!=null) {
			String path = null;
			try {
				path = (String) msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			System.out.println(this.myAgent.getLocalName() + " - NEW ACTION - CURRENT POSITION: " + (me.getCurrentPosition().getLocationId()));
			System.out.println(this.myAgent.getLocalName() + " - Received GET-OUT-OF-MY-WAY from: " + msgReceived.getSender().getLocalName() + " - Path: " + path + " getting out of the way.");
			// if current position is equal to path, move to random adjacent node
			System.out.println(this.myAgent.getLocalName() + " - Current position: " + myPositionId + " - Path: " + path);
			if (myPositionId.equals(path)) {
				System.out.println(this.myAgent.getLocalName() + " - Current position "+ "(" + myPositionId + ") is equal to path "+ "(" + path + "), moving to random adjacent node.");
				List<String> possibleMoves = new ArrayList<>();
				for (int i = 1; i < lobs.size(); i++) {
					String adjacentNodeId = lobs.get(i).getLeft().getLocationId();
					if (!adjacentNodeId.equals(myPositionId) && !adjacentNodeId.equals(path)) {
						possibleMoves.add(adjacentNodeId);
					}
				}
				
				if (!possibleMoves.isEmpty()) {
					int randomIndex = new Random().nextInt(possibleMoves.size());
					GsLocation newpos = new GsLocation(possibleMoves.get(randomIndex));
					System.out.println(this.myAgent.getLocalName() + " - Moving to random adjacent node: " + newpos.getLocationId() + " to avoid collision with " + msgReceived.getSender().getLocalName());
					
					// check if there is yet another agent in the cell in newpos
					String agentAtNewPos = null;
					for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
						//System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
						if (obs.getLeft().getLocationId().equals(newpos.getLocationId())) {
							// Check if there's an agent at this location
							for (Couple<Observation, String> o : obs.getRight()) {
								//System.out.println("Observation: " + o.getLeft() + " - " + o.getRight());
								if (o.getLeft() == Observation.AGENTNAME) {
									//System.out.println(this.myAgent.getLocalName() + " - Another agent detected at " + path.get(0) + ", avoiding collision.");
									agentAtNewPos = o.getRight();
									break;
								}
							}
							break;
						}
					}

					// if there is yet another agent in the new position, send GET-OUT-OF-MY-WAY to that agent
					if (agentAtNewPos != null) {
						// if the other agent is a wumpus, dont bother sending it a message, just go back to possibleMoves.isEmpty() case
						if (agentAtNewPos.equals("Wumpus")) {
							System.out.println(this.myAgent.getLocalName() + " - Another agent detected at " + newpos.getLocationId() + ", but it's a Wumpus, so I'm not sending it a message.");
							System.out.println(this.myAgent.getLocalName() + " - No possible moves to avoid collision, staying in original position: " + myPositionId + " sending GET-OUT-OF-MY-WAY back to " + msgReceived.getSender().getLocalName());
							// get position of the sender
							String senderPositionId = null;
							for (Couple<Location, List<Couple<Observation, String>>> l : lobs) {
								if (l.getLeft().getLocationId().equals(msgReceived.getSender().getLocalName())) {
									senderPositionId = l.getLeft().getLocationId();
									break;
								}
							}
							// create receivers list that's just the agent at newpos
							List<String> temp_receivers = new ArrayList<>();
							temp_receivers.add(msgReceived.getSender().getLocalName());
							// send GET-OUT-OF-MY-WAY message to the agent at newpos
							myAgent.addBehaviour(new GetOutOfMyWayBehaviour(this.myAgent, senderPositionId, temp_receivers));
							return;
						}
						System.out.println(this.myAgent.getLocalName() + " - Another agent detected at " + newpos.getLocationId() + ", sending it a GET-OUT-OF-MY-WAY message.");
						// create receivers list that's just the agent that sent the message
						List<String> temp_receivers = new ArrayList<>();
						temp_receivers.add(agentAtNewPos);
						// send GET-OUT-OF-MY-WAY message to the agent
						myAgent.addBehaviour(new GetOutOfMyWayBehaviour(this.myAgent, newpos.getLocationId(), temp_receivers));
						return;
					}
					
					me.moveTo(newpos);
					return;
				} else {
					System.out.println(this.myAgent.getLocalName() + " - No possible moves to avoid collision, staying in original position: " + myPositionId + " sending GET-OUT-OF-MY-WAY back to " + msgReceived.getSender().getLocalName());
					// get position of the sender
					String senderPositionId = null;
					for (Couple<Location, List<Couple<Observation, String>>> l : lobs) {
						if (l.getLeft().getLocationId().equals(msgReceived.getSender().getLocalName())) {
							senderPositionId = l.getLeft().getLocationId();
							break;
						}
					}
					// create receivers list that's just the agent that sent the message
					List<String> temp_receivers = new ArrayList<>();
					temp_receivers.add(msgReceived.getSender().getLocalName());
					// send GET-OUT-OF-MY-WAY message to the agent
					myAgent.addBehaviour(new GetOutOfMyWayBehaviour(this.myAgent, senderPositionId, temp_receivers));
					return;
				}
			}
		}
		// ==================================================================================

	}

	@Override
	public boolean done() {
		return false;
	}
}
