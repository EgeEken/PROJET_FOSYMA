package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;

import dataStructures.tuple.Couple;
import jade.core.behaviours.Behaviour;
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
public class DummyCollectorAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;

    /* === capacities as declared in resources/agent‑custom.json === */
    private static final int GOLD_CAPACITY    = 20;
    private static final int DIAMOND_CAPACITY = 30;
    /* ============================================================ */

    @Override
    protected void setup() {
        super.setup();

        List<Behaviour> lb = new ArrayList<>();
        lb.add(new RandomWalkCollectBehaviour(this, 600));
        addBehaviour(new StartMyBehaviours(this, lb));

        System.out.println("Collector agent " + getLocalName() + " started");
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

    /* ************************************************************** */
    private static class RandomWalkCollectBehaviour extends TickerBehaviour {
        private static final long serialVersionUID = 1L;
        private final Random rnd = new Random();

        RandomWalkCollectBehaviour(AbstractDedaleAgent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {
            AbstractDedaleAgent me = (AbstractDedaleAgent) myAgent;
            Location myPos = me.getCurrentPosition();
            if (myPos == null || myPos.getLocationId().isEmpty()) return;

            /* 1. Observe */
            List<Couple<Location, List<Couple<Observation, String>>>> obs = me.observe();
            List<Couple<Observation, String>> here = obs.get(0).getRight();

            /* 2. Pick treasure on current node */
            for (Couple<Observation, String> o : here) {
                switch (o.getLeft()) {
                    case GOLD:
                    case DIAMOND:
                        if (me.openLock(o.getLeft())) me.pick();
                        break;
                    default: break;
                }
            }

            /* 3. Log carried load */
            printCarried(me);

            /* 4. If tanker adjacent, unload both pockets */
            boolean tankerAdj = obs.stream().skip(1) // neighbours
                    .anyMatch(c -> c.getRight().stream()
                            .anyMatch(o -> o.getLeft()==Observation.AGENTNAME && o.getRight().equals("Tanker")));
            if (tankerAdj) unloadBoth(me);

            /* 5. Random move */
            if (obs.size() > 1) {
                int id = 1 + rnd.nextInt(obs.size() - 1);
                me.moveTo(obs.get(id).getLeft());
            }
        }

        /* ----- unload helper: drops gold, then diamonds if still full ----- */
        private void unloadBoth(AbstractDedaleAgent me) {
            boolean dumpedSomething = false;

            // first drop matching current spec (usually GOLD after first pick)
            if (me.emptyMyBackPack("Tanker")) dumpedSomething = true;

            // if diamond pocket still full, switch spec and drop again
            boolean diamondFull = me.getBackPackFreeSpace().stream()
                    .anyMatch(c -> c.getLeft()==Observation.DIAMOND && c.getRight()==0);
            if (diamondFull) {
                forceTreasureType(me, "DIAMOND");
                if (me.emptyMyBackPack("Tanker")) dumpedSomething = true;
                forceTreasureType(me, "ANY");   // reset for future picks
            }

            if (dumpedSomething)
                System.out.println(me.getLocalName() + " – unloaded BOTH pockets!");
        }

        /* print carried load (not free slots) */
        private void printCarried(AbstractDedaleAgent me) {
            Map<Observation, Integer> caps = Map.of(
                    Observation.GOLD, GOLD_CAPACITY,
                    Observation.DIAMOND, DIAMOND_CAPACITY);
            int goldFree = GOLD_CAPACITY;
            int diaFree  = DIAMOND_CAPACITY;
            for (Couple<Observation,Integer> c : me.getBackPackFreeSpace()) {
                if (c.getLeft()==Observation.GOLD)    goldFree = c.getRight();
                if (c.getLeft()==Observation.DIAMOND) diaFree  = c.getRight();
            }
            int goldCarried = GOLD_CAPACITY    - goldFree;
            int diaCarried  = DIAMOND_CAPACITY - diaFree;
            System.out.println(me.getLocalName()+" – carrying  Gold:"+goldCarried+"  Diamond:"+diaCarried);
        }
    }
}
