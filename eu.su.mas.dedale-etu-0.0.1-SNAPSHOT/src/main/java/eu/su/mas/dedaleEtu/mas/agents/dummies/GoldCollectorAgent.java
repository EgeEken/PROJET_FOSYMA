package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.GoldSearchBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import dataStructures.tuple.Couple;
import jade.core.behaviours.Behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Collector agent that
 * 1. Random‑walks until its gold pocket is full.
 * 2. When the tanker is orthogonally adjacent, unloads gold.
 * 3. Logs how much gold it is **carrying**, not just free slots.
 */
public class GoldCollectorAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;
    private MapRepresentation exploreMap;
    private MapRepresentation goldMap;
    private MapRepresentation diamondMap;
    private MapRepresentation tankerMap;

    /* === capacities as declared in resources/agent‑custom.json === */
    private static int GOLD_CAPACITY = 20;
    private static int DIAMOND_CAPACITY = 0;
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
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}

        List<Behaviour> lb = new ArrayList<>();
        lb.add(new GoldSearchBehaviour(this, exploreMap, goldMap, diamondMap, tankerMap, list_agentNames));
        addBehaviour(new StartMyBehaviours(this, lb));



        System.out.println("Gold Collector agent " + getLocalName() + " started");
    }

    public static List<Integer> getCapacity() {
        List<Integer> capacities = new ArrayList<>();
        capacities.add(GOLD_CAPACITY);
        capacities.add(DIAMOND_CAPACITY);
        return capacities;
    }
}
