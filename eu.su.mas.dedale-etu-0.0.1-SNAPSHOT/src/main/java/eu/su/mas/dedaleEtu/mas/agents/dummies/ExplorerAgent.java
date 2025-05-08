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


public class ExplorerAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;
    private MapRepresentation exploreMap;
    private MapRepresentation goldMap;
    private MapRepresentation diamondMap;
    private MapRepresentation tankerMap;

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
        
        int targetResourceIndex = -1; // Explorer agent doesn't collect resources

        List<Behaviour> lb = new ArrayList<>();
        lb.add(new SearchBehaviour(this, exploreMap, goldMap, diamondMap, tankerMap, list_agentNames, targetResourceIndex));
        addBehaviour(new StartMyBehaviours(this, lb));


        System.out.println("Explorer agent " + getLocalName() + " started");
    }

    public List<Integer> getCapacity() {
        List<Integer> capacities = new ArrayList<>();
        capacities.add(0);
        capacities.add(0);
        return capacities;
    }
}
