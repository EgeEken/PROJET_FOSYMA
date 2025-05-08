package eu.su.mas.dedaleEtu.mas.behaviours.fsm_states;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.GoldCollectorAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * A behavior similar to ExploSoloBehaviour, but specialized in searching
 * and collecting Gold. If gold is visible, go pick it; otherwise continue
 * normal exploration.
 */
public class FSMCollectorBehaviour extends FSMBehaviour {
	
	private static final long serialVersionUID = 1L;

	private static final int TIMER = 100; // Time to wait between actions (in milliseconds)
	
	private boolean finished = false;
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation goldMap; // Gold map, gold nodes with remaining gold are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open

	private List<String> receivers; // List of agents to send maps to
	
	
	public FSMCollectorBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> receivers) {
		super(myagent);
		
		// =========== To disable the GraphStream UI ==================
		System.setProperty("org.graphstream.ui", "null");
		// ============================================================

		this.exploreMap = myMap; // Initialize the exploration map
		//this.goldMap = new MapRepresentation(myagent.getLocalName()); // Initialize the gold map
		//this.tankerMap = new MapRepresentation(myagent.getLocalName()); // Initialize the tanker map

		this.receivers = receivers; // List of agents to send maps to
	}
	
	@Override
	public void action() {
		System.out.println();
		System.out.println(this.myAgent.getLocalName() + " - NEW ACTION...");
		System.out.println(this.myAgent.getLocalName() + " - Current gold capacity: " + ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace().get(0).getRight());
		System.out.println();
		AbstractDedaleAgent me = (AbstractDedaleAgent) this.myAgent;
		if (me == null) return;

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
		if (this.tankerMap == null) {
			this.tankerMap = new MapRepresentation(me.getLocalName());
		}

		
		String myPositionId = myPosition.getLocationId();

		// 1) Observe
		List<Couple<Location, List<Couple<Observation, String>>>> lobs = me.observe();
		List<Couple<Observation, String>> here = lobs.get(0).getRight();

		// print here
		System.out.println(this.myAgent.getLocalName() + " - Observing from: " + myPositionId + " - " + here);

		//print all lobs
		for (Couple<Location, List<Couple<Observation, String>>> l : lobs) {
			System.out.println(this.myAgent.getLocalName() + " - Observed: " + l.getLeft().getLocationId() + " - " + l.getRight());
		}
		System.out.println();

		// 2) Mark current node as closed on the exploration map
		this.exploreMap.addNode(myPositionId, MapAttribute.closed);
		this.goldMap.addNode(myPosition.getLocationId(),MapAttribute.closed);
		this.tankerMap.addNode(myPosition.getLocationId(),MapAttribute.closed);

		// ===================== RECEIVE MAPS FROM OTHER AGENTS =========================
		MessageTemplate e_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-EXPLORE"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage e_msgReceived=this.myAgent.receive(e_msgTemplate);
		
		MessageTemplate r_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-RESOURCE"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage r_msgReceived=this.myAgent.receive(r_msgTemplate);

		MessageTemplate t_msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TANKER"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage t_msgReceived=this.myAgent.receive(t_msgTemplate);

		if (e_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> e_sgreceived=null;
			System.out.println(this.myAgent.getLocalName() + " - Received explore map from: " + e_msgReceived.getSender().getLocalName());
			try {
				e_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)e_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.exploreMap.mergeMap(e_sgreceived);
		}

		if (r_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> r_sgreceived=null;
			System.out.println(this.myAgent.getLocalName() + " - Received gold map from: " + r_msgReceived.getSender().getLocalName());
			try {
				r_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)r_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.goldMap.mergeMap(r_sgreceived);
		}

		if (t_msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> t_sgreceived=null;
			System.out.println(this.myAgent.getLocalName() + " - Received tanker map from: " + t_msgReceived.getSender().getLocalName());
			try {
				t_sgreceived = (SerializableSimpleGraph<String, MapAttribute>)t_msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.tankerMap.mergeMap(t_sgreceived);
		}
		// =============================================================================

		// 3) Update all maps with observed nodes and edges
		for (int i = 1; i < lobs.size(); i++) {
			Location adjacentNode = lobs.get(i).getLeft();
			String adjacentNodeId = adjacentNode.getLocationId();
			
			// Check for observations in adjacent node before adding it to maps
			List<Couple<Observation, String>> nodeObs = lobs.get(i).getRight();
			boolean isTanker = false;
			boolean hasGold = false;
			
			// First check if this is a tanker cell
			for (Couple<Observation, String> o : nodeObs) {
				if (o.getLeft() == Observation.AGENTNAME && o.getRight().equals("Tanker")) {
					isTanker = true;
					break;
				}
				if (o.getLeft() == Observation.GOLD) {
					hasGold = true;
				}
			}
			
			// Handle the node based on what it contains
			if (isTanker) {
				// Only add tanker to tankerMap, not to exploration or gold maps
				System.out.println(this.myAgent.getLocalName() + " detected Tanker at " + adjacentNodeId);
				this.tankerMap.addNode(adjacentNodeId, MapAttribute.open);
				this.tankerMap.addEdge(myPositionId, adjacentNodeId);
				
				// If backpack isn't empty, move to tanker to unload gold
				if (me.getBackPackFreeSpace().get(0).getRight() < GoldCollectorAgent.getGoldCapacity()) {
					System.out.println(this.myAgent.getLocalName() + " - Backpack not empty, moving to tanker to unload gold.");
					((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(adjacentNodeId));
					unloadGold(me);
					return;
				} else {
					System.out.println(this.myAgent.getLocalName() + " - Backpack empty, nothing to unload. Marking tanker location for future use.");
					// No need to return here - continue exploring
				}
			} else {
				// For non-tanker nodes, add to all maps
				this.exploreMap.addNewNode(adjacentNodeId);
				this.exploreMap.addEdge(myPositionId, adjacentNodeId);
				
				this.goldMap.addEdge(myPositionId, adjacentNodeId);
				
				this.tankerMap.addNode(adjacentNodeId, MapAttribute.closed);
				this.tankerMap.addEdge(myPositionId, adjacentNodeId);
			}
		}

		// =============== SEND MAPS TO OTHER AGENTS =====================================
		ACLMessage e_msg = new ACLMessage(ACLMessage.INFORM);
		e_msg.setProtocol("SHARE-EXPLORE");
		e_msg.setSender(this.myAgent.getAID());
		ACLMessage r_msg = new ACLMessage(ACLMessage.INFORM);
		r_msg.setProtocol("SHARE-RESOURCE");
		r_msg.setSender(this.myAgent.getAID());
		ACLMessage t_msg = new ACLMessage(ACLMessage.INFORM);
		t_msg.setProtocol("SHARE-TANKER");
		t_msg.setSender(this.myAgent.getAID());

		for (String agentName : receivers) {
			e_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			r_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			t_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
			
		SerializableSimpleGraph<String, MapAttribute> e_sg = this.exploreMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> r_sg = this.goldMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> t_sg = this.tankerMap.getSerializableGraph();
		
		try {					
			e_msg.setContentObject(e_sg);
			r_msg.setContentObject(r_sg);
			t_msg.setContentObject(t_sg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(e_msg);
		((AbstractDedaleAgent)this.myAgent).sendMessage(r_msg);
		((AbstractDedaleAgent)this.myAgent).sendMessage(t_msg);

		// ===============================================================================

		
		// 4) Check for gold in the current location
		for (Couple<Observation, String> o : here) {
			if (o.getLeft() == Observation.GOLD) {
				System.out.println(this.myAgent.getLocalName() + " - found gold at " + myPositionId);
				System.out.println(this.myAgent.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());
				if (me.getBackPackFreeSpace().get(0).getRight() == 0) {
					System.out.println(this.myAgent.getLocalName() + " - Backpack full, cannot pick gold, marking gold spot on map as open for future picking.");
					this.goldMap.addNode(myPositionId, MapAttribute.open);
					break;
				}
				System.out.println(this.myAgent.getLocalName() + " - Value of the treasure on the current position: " + o.getLeft() + ": " + o.getRight());
				System.out.println(this.myAgent.getLocalName() + " - I try to open the safe");
				if (me.openLock(o.getLeft())) {
					System.out.println(this.myAgent.getLocalName() + " - lock is open, trying to pick gold...");
					int picked = me.pick();

					System.out.println(this.myAgent.getLocalName() + " - picked: " + picked);
					System.out.println(this.myAgent.getLocalName() + " - remaining backpack capacity: " + me.getBackPackFreeSpace());

					if (picked > 0) {
						System.out.println(this.myAgent.getLocalName() + " picked " + picked + " gold!"); 
						if (o.getRight().equals("0")) {
							// No more gold here, mark as closed in gold map
							System.out.println(this.myAgent.getLocalName() + " - No more gold at " + myPositionId + ", marking as closed.");
							this.goldMap.addNode(myPositionId, MapAttribute.closed);
						} else {
							// Still gold here, mark as open in gold map
							System.out.println(this.myAgent.getLocalName() + " - Still gold left at " + myPositionId + " after picking, leaving as open.");
							this.goldMap.addNode(myPositionId, MapAttribute.open);
						}
						return;
					}
				}
			} else {
				System.out.println(this.myAgent.getLocalName() + " - No gold at " + myPositionId + ", moving on.");
			}
		}

		// 5) Check if backpack is full and handle unloading to tanker
		if (me.getBackPackFreeSpace().get(0).getRight() == 0) {
			System.out.println(this.myAgent.getLocalName() + " - Backpack full, looking for tanker to unload gold.");
			System.out.println(this.myAgent.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());
			// Find path to nearest tanker if known
			String tankerNodeId = findClosestTankerNode(myPositionId);
			if (tankerNodeId != null) {
				List<String> path = this.tankerMap.getShortestPath(myPositionId, tankerNodeId);
				if (!path.isEmpty()) {
					// Move to the first node in the path towards the tanker
					GsLocation newpos = new GsLocation(path.get(0));
					System.out.println(this.myAgent.getLocalName() + " - tanker path known (tanker at " + tankerNodeId + "), moving to " + path.get(0) + " in route to tanker.");
					
					// if there is another agent in the cell in newpos, move a random direction to avoid collision
					boolean agentAtNewPos = false;
					for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
						System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
						if (obs.getLeft().getLocationId().equals(path.get(0))) {
							// Check if there's an agent at this location
							for (Couple<Observation, String> o : obs.getRight()) {
								System.out.println("Observation: " + o.getLeft() + " - " + o.getRight());
								if (o.getLeft() == Observation.AGENTNAME) {
									System.out.println(this.myAgent.getLocalName() + " - Another agent detected at " + path.get(0) + ", avoiding collision.");
									agentAtNewPos = true;
									break;
								}
							}
							break;
						}
					}

					// If there's another agent at newpos, pick a random adjacent node that isn't newpos
					if (agentAtNewPos) {
						List<String> possibleMoves = new ArrayList<>();
						for (int i = 1; i < lobs.size(); i++) {
							String adjacentNodeId = lobs.get(i).getLeft().getLocationId();
							if (!adjacentNodeId.equals(path.get(0))) {
								possibleMoves.add(adjacentNodeId);
							}
						}
						
						if (!possibleMoves.isEmpty()) {
							int randomIndex = new Random().nextInt(possibleMoves.size());
							newpos = new GsLocation(possibleMoves.get(randomIndex));
							System.out.println(this.myAgent.getLocalName() + " - Avoiding collision, moving to random position: " + newpos.getLocationId());
						}
					}

					((AbstractDedaleAgent) this.myAgent).moveTo(newpos);
					
					// If we've reached tanker's position, unload gold
					if (path.get(0).equals(tankerNodeId)) {
						unloadGold(me);
					}
					return;
				} else {
					System.out.println(this.myAgent.getLocalName() + " - No path to tanker found, continuing exploration to find it.");
				}
			}
		} else {
			System.out.println(this.myAgent.getLocalName() + " - Backpack not full, continuing exploration.");
		}


		// 6) Prioritize known gold if not full
		if (me.getBackPackFreeSpace().get(0).getRight() > 0) {
			String goldNodeId = findClosestGoldNode(myPositionId);
			if (goldNodeId != null) {
				List<String> path = this.goldMap.getShortestPath(myPositionId, goldNodeId);
				if (!path.isEmpty()) {
					GsLocation newpos = new GsLocation(path.get(0)); 
					System.out.println(this.myAgent.getLocalName() + " - Moving towards known gold, moving to: " + newpos.getLocationId());
					// if there is another agent in the cell in newpos, move a random direction to avoid collision
					boolean agentAtNewPos = false;

					for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
						System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
						if (obs.getLeft().getLocationId().equals(path.get(0))) {
							// Check if there's an agent at this location
							for (Couple<Observation, String> o : obs.getRight()) {
								if (o.getLeft() == Observation.AGENTNAME) {
									System.out.println(this.myAgent.getLocalName() + " - Another agent detected at " + path.get(0) + ", avoiding collision.");
									agentAtNewPos = true;
									break;
								}
							}
							break;
						}
					}

					// If there's another agent at newpos, pick a random adjacent node that isn't newpos
					if (agentAtNewPos) {
						List<String> possibleMoves = new ArrayList<>();
						for (int i = 1; i < lobs.size(); i++) {
							String adjacentNodeId = lobs.get(i).getLeft().getLocationId();
							if (!adjacentNodeId.equals(path.get(0))) {
								possibleMoves.add(adjacentNodeId);
							}
						}
						
						if (!possibleMoves.isEmpty()) {
							int randomIndex = new Random().nextInt(possibleMoves.size());
							newpos = new GsLocation(possibleMoves.get(randomIndex));
							System.out.println(this.myAgent.getLocalName() + " - Avoiding collision, moving to random position: " + newpos.getLocationId());
						}
					}
					((AbstractDedaleAgent) this.myAgent).moveTo(newpos);
					return;
				} else {
					System.out.println(this.myAgent.getLocalName() + " - No known path to gold, continuing exploration.");
				}
			}
		}

		// 7) Continue exploration if no gold is targeted
		if (this.exploreMap.hasOpenNode()) {
			System.out.println(this.myAgent.getLocalName() + " - No known targets, backpack not full, continuing exploration.");
			List<String> path = this.exploreMap.getShortestPathToClosestOpenNode(myPositionId);
			if (!path.isEmpty()) {
				GsLocation newpos = new GsLocation(path.get(0));
				System.out.println(this.myAgent.getLocalName() + " - Moving towards next open node, moving to: " + newpos.getLocationId());
				
				// if there is another agent in the cell in newpos, move a random direction to avoid collision
				boolean agentAtNewPos = false;

				for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
					System.out.println("Observation: " + obs.getLeft().getLocationId() + " - " + obs.getRight());
					if (obs.getLeft().getLocationId().equals(path.get(0))) {
						// Check if there's an agent at this location
						for (Couple<Observation, String> o : obs.getRight()) {
							System.out.println("Observation: " + o.getLeft() + " - " + o.getRight());
							if (o.getLeft() == Observation.AGENTNAME) {
								System.out.println(this.myAgent.getLocalName() + " - Another agent detected at " + path.get(0) + ", avoiding collision.");
								agentAtNewPos = true;
								break;
							}
						}
						break;
					}
				}

				// If there's another agent at newpos, pick a random adjacent node that isn't newpos
				if (agentAtNewPos) {
					List<String> possibleMoves = new ArrayList<>();
					for (int i = 1; i < lobs.size(); i++) {
						String adjacentNodeId = lobs.get(i).getLeft().getLocationId();
						if (!adjacentNodeId.equals(path.get(0))) {
							possibleMoves.add(adjacentNodeId);
						}
					}
					
					if (!possibleMoves.isEmpty()) {
						int randomIndex = new Random().nextInt(possibleMoves.size());
						newpos = new GsLocation(possibleMoves.get(randomIndex));
						System.out.println(this.myAgent.getLocalName() + " - Avoiding collision, moving to random position: " + newpos.getLocationId());
					}
				}
				((AbstractDedaleAgent) this.myAgent).moveTo(newpos);
				return;
			} else {
				System.out.println(this.myAgent.getLocalName() + " - No path to open node found, stopping exploration.");
			}
		} else {
			finished = true;
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println(this.myAgent.getLocalName() + " - Exploration done (no more open nodes).");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
	}

	/**
	 * Find the closest node with gold
	 */
	private String findClosestGoldNode(String currentPosition) {
		List<String> openGoldNodes = this.goldMap.getOpenNodes();
		if (openGoldNodes.isEmpty()) return null;
		
		String closest = null;
		int shortestDist = Integer.MAX_VALUE;
		
		for (String goldNode : openGoldNodes) {
			List<String> path = this.goldMap.getShortestPath(currentPosition, goldNode);
			if (path != null && !path.isEmpty() && path.size() < shortestDist) {
				shortestDist = path.size();
				closest = goldNode;
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

	private void unloadGold(AbstractDedaleAgent me) {
		if (me.emptyMyBackPack("Tanker")) {
			System.out.println(me.getLocalName() + " - unloaded gold!");
			System.out.println(me.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());

		}
	}

	@Override
	public boolean done() {
		return finished;
	}
}
