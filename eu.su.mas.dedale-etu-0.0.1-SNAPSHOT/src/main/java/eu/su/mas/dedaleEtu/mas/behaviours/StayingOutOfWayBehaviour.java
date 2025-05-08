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
 * A behavior similar to ExploSoloBehaviour, but specialized in searching
 * and collecting a resource. If the target resource is visible, go pick it; otherwise continue
 * normal exploration.
 */
public class StayingOutOfWayBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 1L;

	private static final int TIMER = 1000; // Time to wait between actions (in milliseconds)
	
	private boolean finished = false;
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation goldMap; // Gold map, gold nodes with remaining gold are open
	private MapRepresentation diamondMap; // Diamond map, diamond nodes are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open

	private int targetResourceIndex; // Index of the resource in the backpack (0 for gold, 1 for diamond)
	private Observation targetResource; // Resource to search for (gold or diamond)

	private List<String> receivers; // List of agents to send maps to

	private HashMap<String, Boolean> expert_list; // List of agents that are experts (0 for non-expert, 1 for expert) (example: 001010 means agents 2 and 4 are experts, 0 1 3 5 are not)
	
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
		System.out.println();
		System.out.println(this.myAgent.getLocalName() + " - NEW ACTION - CURRENT POSITION: " + (me.getCurrentPosition().getLocationId()));
		System.out.println(this.myAgent.getLocalName() + " - Current backpack: " + (me.getBackPackFreeSpace()));
		System.out.println();

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
			System.out.println(this.myAgent.getLocalName() + " - Received GET-OUT-OF-MY-WAY from: " + msgReceived.getSender().getLocalName() + " - Path: " + path + " getting out of the way.");
			// if current position is equal to path, move to random adjacent node
			System.out.println(this.myAgent.getLocalName() + " - Current position: " + myPositionId + " - Path: " + path);
			if (myPositionId.equals(path)) {
				System.out.println(this.myAgent.getLocalName() + " - Current position "+ "(" + myPositionId + ") is equal to path "+ "(" + path + "), moving to random adjacent node.");
				List<String> possibleMoves = new ArrayList<>();
				for (int i = 1; i < lobs.size(); i++) {
					String adjacentNodeId = lobs.get(i).getLeft().getLocationId();
					if (!adjacentNodeId.equals(myPositionId)) {
						possibleMoves.add(adjacentNodeId);
					}
				}
				
				if (!possibleMoves.isEmpty()) {
					int randomIndex = new Random().nextInt(possibleMoves.size());
					GsLocation newpos = new GsLocation(possibleMoves.get(randomIndex));
					System.out.println(this.myAgent.getLocalName() + " - Moving to random adjacent node: " + newpos.getLocationId() + " to avoid collision with " + msgReceived.getSender().getLocalName());
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
					// create receivers list that's just the agent at newpos
					List<String> temp_receivers = new ArrayList<>();
					temp_receivers.add(msgReceived.getSender().getLocalName());
					// send GET-OUT-OF-MY-WAY message to the agent at newpos
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
