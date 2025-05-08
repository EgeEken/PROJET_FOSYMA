package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.DiamondCollectorAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;

/**
 * A behavior similar to ExploSoloBehaviour, but specialized in searching
 * and collecting Diamond. If diamond is visible, go pick it; otherwise continue
 * normal exploration.
 */
public class DiamondSearchBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 1L;
	
	private boolean finished = false;
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation diamondMap; // Diamond map, diamond nodes with remaining diamond are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open
	
	
	public DiamondSearchBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap) {
		super(myagent);
		
		//System.setProperty("org.graphstream.ui", "null");

		this.exploreMap = myMap; // Initialize the exploration map
		//this.diamondMap = new MapRepresentation(myagent.getLocalName()); // Initialize the diamond map
		//this.tankerMap = new MapRepresentation(myagent.getLocalName()); // Initialize the tanker map
	}
	
	@Override
	public void action() {
		System.out.println();
		System.out.println(this.myAgent.getLocalName() + " - NEW ACTION...");
		System.out.println(this.myAgent.getLocalName() + " - Current diamond capacity: " + ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace().get(0).getRight());
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
			this.myAgent.doWait(100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// initialize maps if needed
		if (this.exploreMap == null) {
			this.exploreMap = new MapRepresentation(me.getLocalName());
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
		System.out.println(this.myAgent.getLocalName() + " - Observing from: " + myPositionId + " - " + here);

		//print all lobs
		for (Couple<Location, List<Couple<Observation, String>>> l : lobs) {
			System.out.println(this.myAgent.getLocalName() + " - Observed: " + l.getLeft().getLocationId() + " - " + l.getRight());
		}
		System.out.println();

		// 2) Mark current node as closed on the exploration map
		this.exploreMap.addNode(myPositionId, MapAttribute.closed);
		this.diamondMap.addNode(myPosition.getLocationId(),MapAttribute.closed);
		this.tankerMap.addNode(myPosition.getLocationId(),MapAttribute.closed);

		// 3) Update all maps with observed nodes and edges
		for (int i = 1; i < lobs.size(); i++) {
			Location adjacentNode = lobs.get(i).getLeft();
			String adjacentNodeId = adjacentNode.getLocationId();
			
			// Check for observations in adjacent node before adding it to maps
			List<Couple<Observation, String>> nodeObs = lobs.get(i).getRight();
			boolean isTanker = false;
			boolean hasDiamond = false;
			
			// First check if this is a tanker cell
			for (Couple<Observation, String> o : nodeObs) {
				if (o.getLeft() == Observation.AGENTNAME && o.getRight().equals("Tanker")) {
					isTanker = true;
					break;
				}
				if (o.getLeft() == Observation.DIAMOND) {
					hasDiamond = true;
				}
			}
			
			// Handle the node based on what it contains
			if (isTanker) {
				// Only add tanker to tankerMap, not to exploration or diamond maps
				System.out.println(this.myAgent.getLocalName() + " detected Tanker at " + adjacentNodeId);
				this.tankerMap.addNode(adjacentNodeId, MapAttribute.open);
				this.tankerMap.addEdge(myPositionId, adjacentNodeId);
				
				// If backpack isn't empty, move to tanker to unload diamond
				if (me.getBackPackFreeSpace().get(0).getRight() < DiamondCollectorAgent.getDiamondCapacity()) {
					System.out.println(this.myAgent.getLocalName() + " - Backpack not empty, moving to tanker to unload diamond.");
					((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(adjacentNodeId));
					unloadDiamond(me);
					return;
				} else {
					System.out.println(this.myAgent.getLocalName() + " - Backpack empty, nothing to unload. Marking tanker location for future use.");
					// No need to return here - continue exploring
				}
			} else {
				// For non-tanker nodes, add to all maps
				this.exploreMap.addNewNode(adjacentNodeId);
				this.exploreMap.addEdge(myPositionId, adjacentNodeId);
				
				this.diamondMap.addEdge(myPositionId, adjacentNodeId);
				
				this.tankerMap.addNode(adjacentNodeId, MapAttribute.closed);
				this.tankerMap.addEdge(myPositionId, adjacentNodeId);
			}
		}
		
		// 4) Check for diamond in the current location
		for (Couple<Observation, String> o : here) {
			if (o.getLeft() == Observation.DIAMOND) {
				System.out.println(this.myAgent.getLocalName() + " - found diamond at " + myPositionId);
				System.out.println(this.myAgent.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());
				if (me.getBackPackFreeSpace().get(0).getRight() == 0) {
					System.out.println(this.myAgent.getLocalName() + " - Backpack full, cannot pick diamond, marking diamond spot on map as open for future picking.");
					this.diamondMap.addNode(myPositionId, MapAttribute.open);
					break;
				}
				System.out.println(this.myAgent.getLocalName() + " - Value of the treasure on the current position: " + o.getLeft() + ": " + o.getRight());
				System.out.println(this.myAgent.getLocalName() + " - I try to open the safe");
				if (me.openLock(o.getLeft())) {
					System.out.println(this.myAgent.getLocalName() + " - lock is open, trying to pick diamond...");
					int picked = me.pick();

					System.out.println(this.myAgent.getLocalName() + " - picked: " + picked);
					System.out.println(this.myAgent.getLocalName() + " - remaining backpack capacity: " + me.getBackPackFreeSpace());

					if (picked > 0) {
						System.out.println(this.myAgent.getLocalName() + " picked " + picked + " diamond!"); 
						if (o.getRight().equals("0")) {
							// No more diamond here, mark as closed in diamond map
							System.out.println(this.myAgent.getLocalName() + " - No more diamond at " + myPositionId + ", marking as closed.");
							this.diamondMap.addNode(myPositionId, MapAttribute.closed);
						} else {
							// Still diamond here, mark as open in diamond map
							System.out.println(this.myAgent.getLocalName() + " - Still diamond left at " + myPositionId + " after picking, leaving as open.");
							this.diamondMap.addNode(myPositionId, MapAttribute.open);
						}
						return;
					}
				}
			} else {
				System.out.println(this.myAgent.getLocalName() + " - No diamond at " + myPositionId + ", moving on.");
			}
		}

		// 5) Check if backpack is full and handle unloading to tanker
		if (me.getBackPackFreeSpace().get(0).getRight() == 0) {
			System.out.println(this.myAgent.getLocalName() + " - Backpack full, looking for tanker to unload diamond.");
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
					
					// If we've reached tanker's position, unload diamond
					if (path.get(0).equals(tankerNodeId)) {
						unloadDiamond(me);
					}
					return;
				} else {
					System.out.println(this.myAgent.getLocalName() + " - No path to tanker found, continuing exploration to find it.");
				}
			}
		} else {
			System.out.println(this.myAgent.getLocalName() + " - Backpack not full, continuing exploration.");
		}


		// 6) Prioritize known diamond if not full
		if (me.getBackPackFreeSpace().get(0).getRight() > 0) {
			String diamondNodeId = findClosestDiamondNode(myPositionId);
			if (diamondNodeId != null) {
				List<String> path = this.diamondMap.getShortestPath(myPositionId, diamondNodeId);
				if (!path.isEmpty()) {
					GsLocation newpos = new GsLocation(path.get(0)); 
					System.out.println(this.myAgent.getLocalName() + " - Moving towards known diamond, moving to: " + newpos.getLocationId());
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
					System.out.println(this.myAgent.getLocalName() + " - No known path to diamond, continuing exploration.");
				}
			}
		}

		// 7) Continue exploration if no diamond is targeted
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
	 * Find the closest node with diamond
	 */
	private String findClosestDiamondNode(String currentPosition) {
		List<String> openDiamondNodes = this.diamondMap.getOpenNodes();
		if (openDiamondNodes.isEmpty()) return null;
		
		String closest = null;
		int shortestDist = Integer.MAX_VALUE;
		
		for (String diamondNode : openDiamondNodes) {
			List<String> path = this.diamondMap.getShortestPath(currentPosition, diamondNode);
			if (!path.isEmpty() && path.size() < shortestDist) {
				shortestDist = path.size();
				closest = diamondNode;
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
			if (!path.isEmpty() && path.size() < shortestDist) {
				shortestDist = path.size();
				closest = tankerNode;
			}
		}
		
		return closest;
	}

	private void unloadDiamond(AbstractDedaleAgent me) {
		if (me.emptyMyBackPack("Tanker")) {
			System.out.println(me.getLocalName() + " - unloaded diamond!");
			System.out.println(me.getLocalName() + " - My current backpack capacity is: " + me.getBackPackFreeSpace());

		}
	}

	@Override
	public boolean done() {
		return finished;
	}
}
