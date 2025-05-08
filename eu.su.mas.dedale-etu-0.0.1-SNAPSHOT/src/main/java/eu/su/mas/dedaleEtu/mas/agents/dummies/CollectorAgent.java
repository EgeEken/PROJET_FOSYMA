package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.SearchBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMultiMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GetOutOfMyWayBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import dataStructures.tuple.Couple;
import jade.core.behaviours.Behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CollectorAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;
    private MapRepresentation exploreMap;
    private MapRepresentation goldMap;
    private MapRepresentation diamondMap;
    private MapRepresentation tankerMap;

    /* === capacities as declared in resources/agent‑custom.json === */
    private int GOLD_CAPACITY;
    private int DIAMOND_CAPACITY;
    /* ============================================================ */

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
            GOLD_CAPACITY = Integer.parseInt((String)args[2]);
            DIAMOND_CAPACITY = Integer.parseInt((String)args[3]);
            System.out.println(this.getLocalName() + " - GOLD_CAPACITY: " + GOLD_CAPACITY);
            System.out.println(this.getLocalName() + " - DIAMOND_CAPACITY: " + DIAMOND_CAPACITY);

			int i=4;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}
        
        int targetResourceIndex = -1;
        if (GOLD_CAPACITY > 0) {
            targetResourceIndex = 0;
        } else if (DIAMOND_CAPACITY > 0) {
            targetResourceIndex = 1;
        } else {
            System.err.println("Error while creating the agent, no capacity for gold or diamond");
            System.exit(-1);
        }
        List<Behaviour> lb = new ArrayList<>();
        lb.add(new SearchBehaviour(this, exploreMap, goldMap, diamondMap, tankerMap, list_agentNames, targetResourceIndex));
        addBehaviour(new StartMyBehaviours(this, lb));


        if (targetResourceIndex == 0) {
            System.out.println("Gold Collector agent " + getLocalName() + " started");
        }
        else if (targetResourceIndex == 1) {
            System.out.println("Diamond Collector agent " + getLocalName() + " started");
        } else {
            System.err.println("Error while creating the agent, no capacity for gold or diamond");
            System.exit(-1);
        }
    }

    public List<Integer> getCapacity() {
        List<Integer> capacities = new ArrayList<>();
        capacities.add(GOLD_CAPACITY);
        capacities.add(DIAMOND_CAPACITY);
        return capacities;
    }
}
