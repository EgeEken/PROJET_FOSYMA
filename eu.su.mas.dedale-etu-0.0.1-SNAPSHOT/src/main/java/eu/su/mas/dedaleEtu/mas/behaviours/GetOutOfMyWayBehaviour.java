package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class GetOutOfMyWayBehaviour extends OneShotBehaviour{
	
	private String path;
	private List<String> receivers;

	public GetOutOfMyWayBehaviour(Agent a, String path, List<String> receivers) {
		super(a);

		this.path = path;
		this.receivers = receivers;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
	public void action() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("GET-OUT-OF-MY-WAY");
		msg.setSender(this.myAgent.getAID());

		for (String agentName : receivers) {
			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}

		System.out.println(myAgent.getLocalName() + " - Sending path: " + path);

		try {					
			msg.setContentObject(path);
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
