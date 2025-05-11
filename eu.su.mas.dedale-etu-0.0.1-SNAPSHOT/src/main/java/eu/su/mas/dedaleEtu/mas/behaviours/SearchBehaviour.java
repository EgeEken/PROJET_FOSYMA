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
import eu.su.mas.dedaleEtu.mas.behaviours.StayingOutOfWayBehaviour;
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
public class SearchBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 1L;

	private static final int TIMER = 400; // Time to wait between actions (in milliseconds)
	
	private boolean finished = false;
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation goldMap; // Gold map, gold nodes with remaining gold are open
	private MapRepresentation diamondMap; // Diamond map, diamond nodes are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open

	private int targetResourceIndex; // Index of the resource in the backpack (0 for gold, 1 for diamond)
	private Observation targetResource; // Resource to search for (gold or diamond)

	private List<String> receivers; // List of agents to send maps to

	private HashMap<String, Boolean> expert_list; // List of agents that are experts (0 for non-expert, 1 for expert) (example: 001010 means agents 2 and 4 are experts, 0 1 3 5 are not)
	
	private List<String> i_cant_open; // List of resource nodes that this agent can't open (lockpick failure)

	public SearchBehaviour(final AbstractDedaleAgent myagent, MapRepresentation exploreMap, 
																	MapRepresentation goldMap, 
																	MapRepresentation diamondMap, 
																	MapRepresentation tankerMap, 
																	List<String> receivers,
																	int targetResourceIndex) {
		super(myagent);
		
		// =========== To disable the GraphStream UI ==================
		// System.setProperty("org.graphstream.ui", "null");
		// ============================================================

		this.exploreMap = exploreMap;
		this.goldMap = goldMap;
		this.diamondMap = diamondMap;
		this.tankerMap = tankerMap;
		
		this.targetResourceIndex = targetResourceIndex; // 0 for gold, 1 for diamond
		if (targetResourceIndex == 0) {
			this.targetResource = Observation.GOLD;
		} else if (targetResourceIndex == 1) {
			this.targetResource = Observation.DIAMOND;
		} else {
			this.targetResource = null; // No resource to search for
		}

		this.receivers = receivers; // List of agents to send maps to

		this.expert_list = new HashMap<>();
		this.expert_list.put(myagent.getLocalName(), false); // Current agent starts as non-expert
		for (String agent : receivers) {
			this.expert_list.put(agent, false); // All other agents start as non-experts
		}

		this.i_cant_open = new ArrayList<>(); // list of resource nodes that this agent can't open (lockpick failure)

		this.finished = false; // Set to true when exploration is done
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

		// initialize maps if needed
		if (this.exploreMap == null) {
			this.exploreMap = new MapRepresentation(me.getLocalName());
		}
		if (this.goldMap == null) {
			this.goldMap = new MapRepresentation(me.getLocalName());
		}
		if (this.diamondMap == null) {
			this.diamondMap = new MapRepresentation(me.getLocalName());
		}
		if (this.tankerMap == null) {
			this.tankerMap = new MapRepresentation(me.getLocalName());
		}

		
		String myPositionId = myPosition.getLocationId();

		// 1) Observe
		List<Couple<Location, List<Couple<Observation, String>>>> lobs = me.observe();
		List<Couple<Observation, String>> here = lobs.get(0).getRight();

		// print here
		//System.out.println(this.myAgent.getLocalName() + " - Observing from: " + myPositionId + " - " + here);

		//print all lobs
		for (Couple<Location, List<Couple<Observation, String>>> l : lobs) {
			//System.out.println(this.myAgent.getLocalName() + " - Observed: " + l.getLeft().getLocationId() + " - " + l.getRight());
		}
		//System.out.println();

		// 2) Mark current node as closed on the exploration map
		this.exploreMap.addNode(myPositionId, MapAttribute.closed);
		// if current position is a gold node, mark it as open in the gold map, else mark it as closed
		if (here.stream().anyMatch(o -> o.getLeft() == Observation.GOLD)) {
			this.goldMap.addNode(myPositionId, MapAttribute.open);
		} else {
			this.goldMap.addNode(myPositionId, MapAttribute.closed);
		}
		// if current position is a diamond node, mark it as open in the diamond map, else mark it as closed
		if (here.stream().anyMatch(o -> o.getLeft() == Observation.DIAMOND)) {
			this.diamondMap.addNode(myPositionId, MapAttribute.open);
		} else {
			this.diamondMap.addNode(myPositionId, MapAttribute.closed);
		}
		this.tankerMap.addNode(myPosition.getLocationId(),MapAttribute.closed);

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

		// ===================== RECEIVE AND SEND EXPERT LIST ==================================
		// receive EXPERT_LIST message from other agents and update the expert list
		MessageTemplate expertlistTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("EXPERT_LIST"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage expertReceived=this.myAgent.receive(expertlistTemplate);

		if (expertReceived!=null) {
			HashMap<String, Boolean> receivedExpertMap = null;
			try {
				receivedExpertMap = (HashMap<String, Boolean>) expertReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			
			if (receivedExpertMap != null) {
				mergeExpertList(receivedExpertMap);
				// received an expert list, means an expert sent it, means an expert also sent the map
				i_am_now_expert();
			}
		}

		// send EXPERT_LIST message to other agents IF this agent is an expert
		//System.out.println(this.myAgent.getLocalName() + " - Sending EXPERT_LIST message to all agents.");
		if (am_i_expert()) {
			ACLMessage expert_msg = new ACLMessage(ACLMessage.INFORM);
			expert_msg.setProtocol("EXPERT_LIST");
			expert_msg.setSender(this.myAgent.getAID());
			for (String agentName : this.receivers) {
				expert_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			}
			try {					
				expert_msg.setContentObject(this.expert_list);
				((AbstractDedaleAgent)this.myAgent).sendMessage(expert_msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//System.out.println(this.myAgent.getLocalName() + " - Sent EXPERT_LIST message to all agents.");
			//System.out.println(this.myAgent.getLocalName() + " - My expert list: " + this.expert_list);
		}
		// ====================================================================================


		// ===================== RECEIVE MAPS FROM OTHER AGENTS =========================
		// Receive maps from other agents and merge them into the local maps
		MessageTemplate e_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-EXPLORE"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage e_msgReceived=this.myAgent.receive(e_msgTemplate);
		
		MessageTemplate g_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-GOLD"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage g_msgReceived=this.myAgent.receive(g_msgTemplate);

		MessageTemplate d_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-DIAMOND"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage d_msgReceived=this.myAgent.receive(d_msgTemplate);

		MessageTemplate t_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TANKER"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage t_msgReceived=this.myAgent.receive(t_msgTemplate);

		if (e_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> e_sgreceived=null;
			//System.out.println(this.myAgent.getLocalName() + " - Received explore map from: " + e_msgReceived.getSender().getLocalName());
			try {
				e_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)e_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.exploreMap.mergeMap(e_sgreceived);
		}

		if (g_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> g_sgreceived=null;
			//System.out.println(this.myAgent.getLocalName() + " - Received gold map from: " + g_msgReceived.getSender().getLocalName());
			try {
				g_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)g_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.goldMap.mergeMap(g_sgreceived);
		}

		if (d_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> d_sgreceived=null;
			//System.out.println(this.myAgent.getLocalName() + " - Received diamond map from: " + d_msgReceived.getSender().getLocalName());
			try {
				d_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)d_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.diamondMap.mergeMap(d_sgreceived);
		}

		if (t_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> t_sgreceived=null;
			//System.out.println(this.myAgent.getLocalName() + " - Received tanker map from: " + t_msgReceived.getSender().getLocalName());
			try {
				t_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)t_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.tankerMap.mergeMap(t_sgreceived);
		}
		// =============================================================================

		// 3) Update all maps with observed nodes and edges
		boolean unloaded = false; // Flag to check if the agent has unloaded the resources
		for (int i = 1; i < lobs.size(); i++) {
			
			Location adjacentNode = lobs.get(i).getLeft();
			String adjacentNodeId = adjacentNode.getLocationId();
			
			// if node is the same as current position, skip it
			if (adjacentNodeId.equals(myPositionId)) {
				continue;
			}

			// Check for observations in adjacent node before adding it to maps
			List<Couple<Observation, String>> nodeObs = lobs.get(i).getRight();
			boolean isTanker = false;
			
			// first check if this is a tanker cell
			for (Couple<Observation, String> o : nodeObs) {
				if (o.getLeft() == Observation.AGENTNAME && o.getRight().equals("Tanker")) {
					isTanker = true;
					break;
				}
			}
			
			// Handle the node based on what it contains
			if (isTanker) {
				// Only add tanker to tankerMap, not to exploration or gold maps
				//System.out.println(this.myAgent.getLocalName() + " detected Tanker at " + adjacentNodeId);
				this.tankerMap.addNode(adjacentNodeId, MapAttribute.open);
				this.tankerMap.addEdge(myPositionId, adjacentNodeId);
				
				// If backpack isn't empty, unload
				if (targetResourceIndex != -1 && (getFreeSpace(me, targetResource) < ((CollectorAgent) me).getCapacity().get(targetResourceIndex))) {
					System.out.println(this.myAgent.getLocalName() + " - Backpack not empty, unloading " + targetResource + " at tanker in " + adjacentNodeId);
					unloadBackpack(me);
					unloaded = true;
				} else {
					//System.out.println(this.myAgent.getLocalName() + " - Backpack empty, nothing to unload. Marking tanker location for future use.");
					// No need to return here - continue exploring
				}
			} else {
				// For non-tanker nodes, add to all maps
				this.exploreMap.addNewNode(adjacentNodeId);
				this.exploreMap.addEdge(myPositionId, adjacentNodeId);
				
				this.goldMap.addEdge(myPositionId, adjacentNodeId);
				this.diamondMap.addEdge(myPositionId, adjacentNodeId);
				
				this.tankerMap.addNode(adjacentNodeId, MapAttribute.closed);
				this.tankerMap.addEdge(myPositionId, adjacentNodeId);
			}
		}

		if (unloaded) {
			return; // If unloaded, skip the rest of the action (unloading takes up one turn)
		}

		// =============== SEND MAPS TO OTHER AGENTS =====================================
		// add an instance of ShareMultiMapBehaviour to this agent's behaviours to send the maps to other agents

		//myAgent.addBehaviour(new ShareMultiMapBehaviour(this.myAgent, this.exploreMap, this.goldMap, this.diamondMap, this.tankerMap, this.receivers));

		ACLMessage e_msg = new ACLMessage(ACLMessage.INFORM);
		e_msg.setProtocol("SHARE-EXPLORE");
		e_msg.setSender(this.myAgent.getAID());
		ACLMessage g_msg = new ACLMessage(ACLMessage.INFORM);
		g_msg.setProtocol("SHARE-GOLD");
		g_msg.setSender(this.myAgent.getAID());
		ACLMessage d_msg = new ACLMessage(ACLMessage.INFORM);
		d_msg.setProtocol("SHARE-DIAMOND");
		d_msg.setSender(this.myAgent.getAID());
		ACLMessage t_msg = new ACLMessage(ACLMessage.INFORM);
		t_msg.setProtocol("SHARE-TANKER");
		t_msg.setSender(this.myAgent.getAID());

		for (String agentName : receivers) {
			e_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			g_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			d_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			t_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
			
		SerializableSimpleGraph<String, MapAttribute> e_sg = this.exploreMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> g_sg = this.goldMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> d_sg = this.diamondMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> t_sg = this.tankerMap.getSerializableGraph();
		
		try {					
			e_msg.setContentObject(e_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(e_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {					
			g_msg.setContentObject(g_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(g_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {					
			d_msg.setContentObject(d_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(d_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {					
			t_msg.setContentObject(t_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(t_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ===============================================================================

		
		// 4) Check for resources in the current location

		if (targetResource != null && targetResourceIndex != -1) {
			for (Couple<Observation, String> o : here) {
				if (o.getLeft() == targetResource) {
					MapRepresentation resourceMap = targetResource == Observation.GOLD ? this.goldMap : this.diamondMap;
					
					System.out.println(this.myAgent.getLocalName() + " - found " + targetResource + " at " + myPositionId);
					System.out.println(this.myAgent.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());
					
					if (getFreeSpace(me, targetResource) == 0) {
						System.out.println(this.myAgent.getLocalName() + " - Backpack full, cannot pick " + targetResource + ", marking spot on map as open for future picking.");
						resourceMap.addNode(myPositionId, MapAttribute.open);
						break;
					}
					
					System.out.println(this.myAgent.getLocalName() + " - Value of the treasure on the current position: " + o.getLeft() + ": " + o.getRight());
					System.out.println(this.myAgent.getLocalName() + " - I try to open the safe");
					
					if (me.openLock(o.getLeft())) {
						System.out.println(this.myAgent.getLocalName() + " - lock is open, trying to pick " + targetResource + "...");
						int picked = me.pick();
						
						System.out.println(this.myAgent.getLocalName() + " - picked: " + picked);
						System.out.println(this.myAgent.getLocalName() + " - remaining backpack capacity: " + me.getBackPackFreeSpace());
						
						if (picked > 0) {
							System.out.println(this.myAgent.getLocalName() + " picked " + picked + " " + targetResource + "!");
							if (getFreeSpace(me, targetResource) > 0) {
								// Still room left in backpack, so there is none left in the cell to pick
								// No more resources here, mark as closed in resource map
								System.out.println(this.myAgent.getLocalName() + " - No more " + targetResource + " at " + myPositionId + ", marking as closed.");
								resourceMap.addNode(myPositionId, MapAttribute.closed);
							} else {
								// Still resources here, mark as open in resource map
								System.out.println(this.myAgent.getLocalName() + " - Still " + targetResource + " left at " + myPositionId + " after picking, leaving as open.");
								resourceMap.addNode(myPositionId, MapAttribute.open);
							}
							return;
						}
					} else {
						System.out.println(this.myAgent.getLocalName() + " - lockpick failed, adding to i_cant_open list.");
						// add this node to the i_cant_open list
						if (!this.i_cant_open.contains(myPositionId)) {
							this.i_cant_open.add(myPositionId);
							System.out.println(this.myAgent.getLocalName() + " - added " + myPositionId + " to i_cant_open list.");
						} else {
							System.out.println(this.myAgent.getLocalName() + " - " + myPositionId + " already in i_cant_open list.");
						}
					}
				}
			}
		}

		// 5) Check if backpack is full and handle unloading to tanker (also if exploration is done but backpack is not emptied)
		if ((targetResourceIndex != -1 && getFreeSpace(me, targetResource) == 0) ||  (!this.exploreMap.hasOpenNode() && targetResourceIndex != -1 && getFreeSpace(me, targetResource) < ((CollectorAgent) me).getCapacity().get(targetResourceIndex))) {
		    System.out.println(this.myAgent.getLocalName() + " - Backpack full, looking for tanker to unload resources.");
		    System.out.println(this.myAgent.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());
		
		    // Find path to nearest tanker if known
		    String tankerNodeId = findClosestTankerNode(myPositionId);
		    if (tankerNodeId != null) {
				List<String> path = this.tankerMap.getShortestPath(myPositionId, tankerNodeId);
				if (!path.isEmpty()) {
					// Move to the first node in the path towards the tanker
					GsLocation newpos = new GsLocation(path.get(0));
					//System.out.println(this.myAgent.getLocalName() + " - tanker path known (tanker at " + tankerNodeId + "), moving to " + path.get(0) + " in route to tanker.");
					
					// if there is another agent in the cell in newpos, move a random direction to avoid collision
					String agentAtNewPos = null;
					for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
						//System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
						if (obs.getLeft().getLocationId().equals(path.get(0))) {
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

					// If there's another agent at newpos, send a GET-OUT-OF-MY-WAY message to the agent
					if (agentAtNewPos != null) {

						System.out.println(this.myAgent.getLocalName() + " - Another agent ("+ agentAtNewPos +") detected in path at " + path.get(0) + ", sending GET-OUT-OF-MY-WAY message to it to avoid collision.");
						// create receivers list that's just the agent at newpos
						List<String> temp_receivers = new ArrayList<>();
						temp_receivers.add(agentAtNewPos);
						// send GET-OUT-OF-MY-WAY message to the agent at newpos
						myAgent.addBehaviour(new GetOutOfMyWayBehaviour(this.myAgent, path.get(0), temp_receivers));
						return;
					}

					me.moveTo(newpos);
					return;
				} else {
					//System.out.println(this.myAgent.getLocalName() + " - No path to tanker found, continuing exploration to find it.");
				}
			}
		} else {
			//System.out.println(this.myAgent.getLocalName() + " - Backpack not full, continuing exploration.");
		}


		// 6) Prioritize known searched resource if not full
		if (targetResourceIndex != -1 && getFreeSpace(me, targetResource) > 0) {
			String resourceNodeId = findClosestResourceNode(myPositionId);
			MapRepresentation resourceMap = targetResource == Observation.GOLD ? this.goldMap : this.diamondMap;
			if (resourceNodeId != null) {
				List<String> path = resourceMap.getShortestPath(myPositionId, resourceNodeId);
				if (!path.isEmpty()) {
					GsLocation newpos = new GsLocation(path.get(0)); 
					//System.out.println(this.myAgent.getLocalName() + " - Moving towards known gold, moving to: " + newpos.getLocationId());

					// if there is another agent in the cell in newpos, move a random direction to avoid collision
					String agentAtNewPos = null;
					for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
						//System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
						if (obs.getLeft().getLocationId().equals(path.get(0))) {
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

					// If there's another agent at newpos, send a GET-OUT-OF-MY-WAY message to the agent
					if (agentAtNewPos != null) {
						System.out.println(this.myAgent.getLocalName() + " - Another agent ("+ agentAtNewPos +") detected in path at " + path.get(0) + ", sending GET-OUT-OF-MY-WAY message to it to avoid collision.");
						// create receivers list that's just the agent at newpos
						List<String> temp_receivers = new ArrayList<>();
						temp_receivers.add(agentAtNewPos);
						// send GET-OUT-OF-MY-WAY message to the agent at newpos
						myAgent.addBehaviour(new GetOutOfMyWayBehaviour(this.myAgent, path.get(0), temp_receivers));
						return;
					}

					me.moveTo(newpos);
					return;
				} else {
					//System.out.println(this.myAgent.getLocalName() + " - No known path to gold, continuing exploration.");
				}
			} else {
				//System.out.println(this.myAgent.getLocalName() + " - No known gold, continuing exploration.");
			}
		}

		// 7) Continue exploration if no gold is targeted
		if (this.exploreMap.hasOpenNode() ) {
			//System.out.println(this.myAgent.getLocalName() + " - No known targets, backpack not full, continuing exploration.");
			List<String> path = this.exploreMap.getShortestPathToClosestOpenNode(myPositionId);
			if (!path.isEmpty()) {
				GsLocation newpos = new GsLocation(path.get(0));
				//System.out.println(this.myAgent.getLocalName() + " - Moving towards next open node, moving to: " + newpos.getLocationId());
				
				// if there is another agent in the cell in newpos, move a random direction to avoid collision
				String agentAtNewPos = null;
				for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
					//System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
					if (obs.getLeft().getLocationId().equals(path.get(0))) {
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

				// If there's another agent at newpos, send a GET-OUT-OF-MY-WAY message to the agent
				if (agentAtNewPos != null) {
					System.out.println(this.myAgent.getLocalName() + " - Another agent ("+ agentAtNewPos +") detected in path at " + path.get(0) + ", sending GET-OUT-OF-MY-WAY message to it to avoid collision.");
					// create receivers list that's just the agent at newpos
					List<String> temp_receivers = new ArrayList<>();
					temp_receivers.add(agentAtNewPos);
					// send GET-OUT-OF-MY-WAY message to the agent at newpos
					myAgent.addBehaviour(new GetOutOfMyWayBehaviour(this.myAgent, path.get(0), temp_receivers));
					return;
				}

				me.moveTo(newpos);
				return;
			} else {
				//System.out.println(this.myAgent.getLocalName() + " - No path to open node found, stopping exploration.");
			}
		} else {
			// if backpack is not empty, unload at tanker before finishing
			if (targetResourceIndex != -1 && getFreeSpace(me, targetResource) < ((CollectorAgent) me).getCapacity().get(targetResourceIndex)) {
				System.out.println(this.myAgent.getLocalName() + " - Backpack not empty, looking for tanker to unload resources before finishing exploration.");
				return;
			}
			// else if every other receiver is not verified experts, keep sending expert messages and updating expert_list
			boolean allExperts = true;
			for (Boolean isExpert : expert_list.values()) {
				if (!isExpert) {
					allExperts = false;
					break;
				}
			}
			
			if (!allExperts) {
				System.out.println(this.myAgent.getLocalName() + " - Exploration done, but not all agents are experts. Moving randomly while sending EXPERT_LIST message to all nearby agents.");
				if (!am_i_expert()) {
					i_am_now_expert();
				}
				// Move randomly to find other agents
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
					me.moveTo(newpos);
				} else {
					System.out.println(this.myAgent.getLocalName() + " - No possible moves to avoid collision, staying in original position: " + myPositionId);
				}
				return;
			}
			System.out.println(this.myAgent.getLocalName() + " - Exploration done, all agents are experts. Stopping agent. (Still listening for GET-OUT-OF-MY-WAY messages)");
			finished = true;
			// add an instance of "StayingOutOfWayBehaviour" to this agent's behaviours
			myAgent.addBehaviour(new StayingOutOfWayBehaviour(me, this.exploreMap, this.goldMap, this.diamondMap, this.tankerMap, this.receivers));
			//System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			//System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println(this.myAgent.getLocalName() + " - Exploration done (no more open nodes).");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			//System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			//System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			// =============== MAKE SURE EVERYONE UP TO SPEED =====================================
			// add an instance of ShareMultiMapBehaviour to this agent's behaviours to send the maps to other agents

			//myAgent.addBehaviour(new ShareMultiMapBehaviour(this.myAgent, this.exploreMap, this.goldMap, this.diamondMap, this.tankerMap, this.receivers));

			// ===============================================================================
		}
	}

	/**
	 * Find the closest node with the resource this agent is looking for
	 */
	private String findClosestResourceNode(String currentPosition) {
		// get a pointer to the map of the resource this agent is looking for
		MapRepresentation resourceMap = null;
		if (targetResource == Observation.GOLD) {
			resourceMap = this.goldMap;
		} else if (targetResource == Observation.DIAMOND) {
			resourceMap = this.diamondMap;
		} else {
			System.out.println(this.myAgent.getLocalName() + " - No resources to look for. ERROR!");
			return null; // No resources to look for
		}

		List<String> openResourceNodes = resourceMap.getOpenNodes();
		// remove nodes that are in the i_cant_open list
		openResourceNodes.removeAll(this.i_cant_open);
		if (openResourceNodes.isEmpty()) return null;
		
		String closest = null;
		int shortestDist = Integer.MAX_VALUE;
		
		for (String resourceNode : openResourceNodes) {
			List<String> path = resourceMap.getShortestPath(currentPosition, resourceNode);
			if (path != null && !path.isEmpty() && path.size() < shortestDist) {
				shortestDist = path.size();
				closest = resourceNode;
			}
		}
		
		return closest;
	}
	
	/**
	 * Find the closest node with tanker
	 */
	private String findClosestTankerNode(String currentPosition) {
		List<String> openTankerNodes = this.tankerMap.getOpenNodes();
		if (openTankerNodes.isEmpty()) return null;
		
		String closest = null;
		int shortestDist = Integer.MAX_VALUE;
		
		for (String tankerNode : openTankerNodes) {
			List<String> path = this.tankerMap.getShortestPath(currentPosition, tankerNode);
			if (path != null && !path.isEmpty() && path.size() < shortestDist) {
				shortestDist = path.size();
				closest = tankerNode;
			}
		}
		
		return closest;
	}

	private void unloadBackpack(AbstractDedaleAgent me) {
		if (me.emptyMyBackPack("Tanker")) {
			//System.out.println(me.getLocalName() + " - unloaded gold!");
			//System.out.println(me.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());

		}
	}

	private int getFreeSpace(AbstractDedaleAgent me, Observation targetResource) {
		for (int i = 0; i < me.getBackPackFreeSpace().size(); i++) {
			if (me.getBackPackFreeSpace().get(i).getLeft() == targetResource) {
				return me.getBackPackFreeSpace().get(i).getRight();
			}
		}
		return -1; // Error: resource not found in backpack
	}

	private void mergeExpertList(HashMap<String, Boolean> new_expert_list) {
		for (Map.Entry<String, Boolean> entry : new_expert_list.entrySet()) {
			String agentName = entry.getKey();
			Boolean newIsExpert = entry.getValue();

			if (this.expert_list.containsKey(agentName)) {
				Boolean currentIsExpert = this.expert_list.get(agentName);
				this.expert_list.put(agentName, newIsExpert || currentIsExpert);
			}
		}
	}

	private boolean am_i_expert() {
		return this.expert_list.get(this.myAgent.getLocalName());
	}

	private void i_am_now_expert() {
		this.expert_list.put(this.myAgent.getLocalName(), true);
		System.out.println(this.myAgent.getLocalName() + " - I am now an expert!");
	}

	@Override
	public boolean done() {
		return finished;
	}
}
