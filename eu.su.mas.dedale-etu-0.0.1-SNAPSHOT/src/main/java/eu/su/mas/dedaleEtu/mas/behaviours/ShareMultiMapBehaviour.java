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

public class ShareMultiMapBehaviour extends OneShotBehaviour{
	
	private MapRepresentation exploreMap;
	private MapRepresentation goldMap;
	private MapRepresentation diamondMap;
	private MapRepresentation tankerMap;
	private List<String> receivers;

	public ShareMultiMapBehaviour(Agent a, MapRepresentation exploreMap,
											MapRepresentation goldMap,
											MapRepresentation diamondMap,
											MapRepresentation tankerMap,
											List<String> receivers) {
		super(a);
		this.exploreMap=exploreMap;
		this.goldMap=goldMap;
		this.diamondMap=diamondMap;
		this.tankerMap=tankerMap;
		this.receivers=receivers;	
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
	public void action() {
		ACLMessage e_msg = new ACLMessage(ACLMessage.INFORM);
		e_msg.setProtocol("SHARE-EXPLORE");
		e_msg.setSender(this.myAgent.getAID());
		ACLMessage g_msg = new ACLMessage(ACLMessage.INFORM);
		g_msg.setProtocol("SHARE-GOLD");
		g_msg.setSender(this.myAgent.getAID());
		ACLMessage d_msg = new ACLMessage(ACLMessage.INFORM);
		d_msg.setProtocol("SHARE-DIAMOND");
		d_msg.setSender(this.myAgent.getAID());
		ACLMessage t_msg = new ACLMessage(ACLMessage.INFORM);
		t_msg.setProtocol("SHARE-TANKER");
		t_msg.setSender(this.myAgent.getAID());

		for (String agentName : receivers) {
			e_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			g_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			d_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			t_msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
			
		SerializableSimpleGraph<String, MapAttribute> e_sg = this.exploreMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> g_sg = this.goldMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> d_sg = this.diamondMap.getSerializableGraph();
		SerializableSimpleGraph<String, MapAttribute> t_sg = this.tankerMap.getSerializableGraph();
		
		try {					
			e_msg.setContentObject(e_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(e_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {					
			g_msg.setContentObject(g_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(g_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {					
			d_msg.setContentObject(d_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(d_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {					
			t_msg.setContentObject(t_sg);
			((AbstractDedaleAgent)this.myAgent).sendMessage(t_msg);
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}

}
