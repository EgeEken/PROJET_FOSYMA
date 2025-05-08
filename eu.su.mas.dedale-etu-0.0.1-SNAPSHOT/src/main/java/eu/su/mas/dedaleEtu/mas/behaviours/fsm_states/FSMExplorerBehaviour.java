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
import eu.su.mas.dedaleEtu.mas.agents.dummies.FSMExplorerAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class FSMExplorerBehaviour extends FSMBehaviour {
	
	private static final long serialVersionUID = 1L;

	private static final int TIMER = 100; // Time to wait between actions (in milliseconds)
	
	private boolean finished = false;
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation goldMap; // Gold map, gold nodes with remaining gold are open
	private MapRepresentation diamondMap; // Diamond map, diamond nodes with remaining diamond are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open

	private List<String> receivers; // List of agents to send maps to
	
	
	public FSMExplorerBehaviour(final AbstractDedaleAgent myagent, MapRepresentation exploreMap, MapRepresentation goldMap, MapRepresentation diamondMap, MapRepresentation tankerMap, List<String> receivers) {
		super(myagent);
		
		// =========== To disable the GraphStream UI ==================
		//System.setProperty("org.graphstream.ui", "null");
		// ============================================================

		this.exploreMap = exploreMap; // Initialize the exploration map
		this.goldMap = goldMap; // Initialize the gold map
		this.diamondMap = diamondMap; // Initialize the diamond map
		this.tankerMap = tankerMap; // Initialize the tanker map

		this.receivers = receivers; // List of agents to send maps to
	}

	public int onEnd() {
		return super.onEnd();
	}
}
