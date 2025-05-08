package eu.su.mas.dedaleEtu.mas.agents.dummies;


import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm_states.FSMExplorerBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import dataStructures.tuple.Couple;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.TickerBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;

/**
 * Collector agent that
 * 1. Random‑walks until one of its treasure pockets is full.
 * 2. When the tanker is orthogonally adjacent, unloads both resources.
 * 3. Logs how many resources it is **carrying**, not just free slots.
 */
public class FSMExplorerAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;
    
	private static final String EXPLORE_STATE = "explore";
	private static final String SHARE_STATE = "share";
	private static final String RECEIVE_STATE = "receive";
	private static final String FINISHED_STATE = "finished";
    
	private MapRepresentation exploreMap; // Exploration map, unexplored nodes are open
	private MapRepresentation goldMap; // Gold map, gold nodes with remaining gold are open
	private MapRepresentation diamondMap; // Diamond map, diamond nodes with remaining gold are open
	private MapRepresentation tankerMap; // Tanker map, tanker node is open

    @Override
    protected void setup() {
        super.setup();
        

		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
        
        List<String> list_agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}
		
		FSMExplorerBehaviour fsm = new FSMExplorerBehaviour(this, exploreMap, goldMap, diamondMap, tankerMap, list_agentNames);
		fsm.registerFirstState()
		
        List<Behaviour> lb = new ArrayList<>();
        lb.add(new fsm);
        addBehaviour(new StartMyBehaviours(this, lb));

        System.out.println("Explorer agent " + getLocalName() + " started");
    }
    
    

    /* --------------------------------------------------------------
     *  Helper: hard‑switch agent specialisation via reflection so we
     *  can unload both pockets even though the API only unloads the
     *  pocket matching the current specialisation.
     * -------------------------------------------------------------- */
    private static void forceTreasureType(AbstractDedaleAgent ag, String newType) {
        try {
            java.lang.reflect.Field f =
                    AbstractDedaleAgent.class.getDeclaredField("myTreasureType");
            f.setAccessible(true);
            f.set(ag, newType);                // "GOLD", "DIAMOND", "ANY"
        } catch (Exception e) {
            e.printStackTrace();               // should not occur in TP context
        }
    }
}
