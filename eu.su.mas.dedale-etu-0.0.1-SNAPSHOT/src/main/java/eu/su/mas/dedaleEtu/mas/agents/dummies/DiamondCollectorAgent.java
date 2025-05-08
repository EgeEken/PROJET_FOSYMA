package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.DiamondSearchBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import dataStructures.tuple.Couple;
import jade.core.behaviours.Behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Collector agent that
 * 1. Random‑walks until its diamond pocket is full.
 * 2. When the tanker is orthogonally adjacent, unloads diamond.
 * 3. Logs how much diamond it is **carrying**, not just free slots.
 */
public class DiamondCollectorAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;
    private MapRepresentation myMap;

    /* === capacities as declared in resources/agent‑custom.json === */
    private static int DIAMOND_CAPACITY = 20;
    /* ============================================================ */

    @Override
    protected void setup() {
        super.setup();

        List<Behaviour> lb = new ArrayList<>();
        lb.add(new DiamondSearchBehaviour(this, myMap));
        addBehaviour(new StartMyBehaviours(this, lb));

        System.out.println("Diamond Collector agent " + getLocalName() + " started");
    }

    public static int getDiamondCapacity() {
        return DIAMOND_CAPACITY;
    }
}
