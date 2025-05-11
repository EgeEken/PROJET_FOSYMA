# PROJET_FOSYMA
The final project for my FOSYMA (Fondaments des Systèmes Multi-Agents) class in the second semester of my first year in the ANDROIDE / AI2D Master at Sorbonne University (UPMC).

## Report
The report is in the 13-EKEN.pdf file. The latex code for the report is in EKEN_RAPPORT.tex.

## Presentation
[Slides link](https://docs.google.com/presentation/d/1EE5kmaVsPP6BmzAg_GiKd_IMBgNK9K2X0YrcVUpuoKE/edit?slide=id.gc6f919934_0_0#slide=id.gc6f919934_0_0)

## Example Execution:

https://github.com/user-attachments/assets/c65a5825-ee63-4eb1-96be-c095eda69585

## Code
The code is written in Java, using the JADE / DEDALE framework. All of the code is in the eu.su.mas.dedale-eu-0.0.1-SNAPSHOT folder. To run the code, you need to initialize the project with Maven on Eclipse, then run the Principal.java file in the src/main/java/eu/su/mas/dedaleEtu/princ/ folder.

Principal.java is the main simulation, which features 2 Explorer agents, 2 Gold Collector agents, 2 Diamond Collector agents, and 1 Tanker (Silo) agent. 
Principal2.java is a simulation with 1 of each agent type instead of 2.
Principal2_diffmap.java is a simulation like Principal2, but with a different topology (map).

## Agents

The agents are in the eu.su.mas.dedaleEtu.agents package. The agents are:

- ExplorerAgent.java: The explorer agent, which explores the map and collects information about it to share with other agents.
- CollectorAgent.java: The collector agent, which collects gold or diamonds (depending on its args specified in resources). This agent also explores, but prioritizes collecting resources over exploring when it can.
- DummyTankerAgent.java: The tanker agent, which is stationary and collector agents bring resources to it. It is a dummy agent, which means it does not store any information or share information with the other agents. (I think that was the point of naming it "dummy"? I didn't change anything about it)

## Behaviours

The behaviours are in the eu.su.mas.dedaleEtu.behaviours package. The behaviours are:

- SearchBehaviour.java: The behaviour used by the explorer and collector agents to explore the map, and if the agent is a collector, to collect resources and bring them to the tanker agent. This behaviour is used by all agents, but the conditions in the action() method make it so that the two agent types function differently.
- ShareMultiMapBehaviour.java: This behaviour is used by both the explorer and collector agents to share information about the map with other agents. It is used by all agents, each agent holds 4 maps, will be explained in the Maps section below.
- GetOutOfMyWayBehaviour.java: This behaviour is used by any agent that encounters another agent in its path, it sends it a message to tell it to "get out of its way". Any agent that receives this message will immediately attempt to get out of the way, and if it is not possible, it will stand still and ask the first agent to get out of the way instead.  
- StayingOutOfWayBehaviour.java: This behaviour is used by any agent that has already finished exploring, and made sure every other agent is an "expert" (i.e. has the full exploreMap, no open nodes left), it will normally stand still and quietly wait, but if it receives a GetOutOfMyWay message, it will attempt to get out of the way same as in SearchBehaviour.

## Maps

- exploreMap: the map that stores the information about what parts of the map have been explored. Open nodes are unexplored, closed nodes are explored.
- goldMap: the map that stores the information about where gold is located. Open nodes mean remaining gold, closed nodes mean no gold (ever or left).
- diamondMap: same as goldMap, but for diamonds.
- tankerMap: the map that stores the information about where the tanker agent is located. Open nodes mean the tanker agent is there, closed nodes mean it is not there.

## Example maps from the perspective of an Explorer agent:
### Example of a real simulation:

<img src="https://github.com/user-attachments/assets/acf77af0-6cb7-45fc-aaf2-c3efeb91ad7e" alt="Real Simulation" width="50%" />

### Explore map example 
(in this map, the entire topology has been explored, the tanker node is not added to the map for pathfinding purposes):

<img src="https://github.com/user-attachments/assets/87209069-7f12-485a-bdcc-b2727e7737cc" alt="Explore Map" width="50%" />

### Resource map example
(in this map, nodes where to the agent's latest knowledge there are remaining resources are marked open, the tanker node is not added to the map for pathfinding purposes)

<img src="https://github.com/user-attachments/assets/00c27217-a5cc-4014-a0bd-ef46eae678f3" alt="Resource Map" width="50%" />

### Tanker map example
(in this map, the tanker node is marked open, the rest are closed)

<img src="https://github.com/user-attachments/assets/8ab29a48-ab5b-47ff-8083-079462e7020a" alt="Tanker Map" width="50%" />

### Other features and notes are explained in the report. (13-EKEN.pdf)
