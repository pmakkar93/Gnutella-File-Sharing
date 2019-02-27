package com.gfiletransfer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;
import java.util.Map.Entry; 

public class SuperPeerImpl implements SuperPeerInterface {
	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    //get current date time with Date()
    Date date = new Date();

	// Defining a hash map for indexing the file details
    // We are using MultiValue Hash Map Data Structure for storing Multiple Entries for a single filename. 
    // For each Filename(Key), there will be collection of entries(Value).
	private MultivaluedMap<String, ArrayList<String>> fileDictionary = new MultivaluedHashMap<>();
	// Buffer for storing the requests
	private Map<String,ArrayList<String>> myMap = new HashMap<String,ArrayList<String>>();
	private String supPeerId=null;
	
	@Override
	public synchronized void registryFiles(String rd, String filename, String peerid, String port_num, String directory,
			String sPeer) throws RemoteException {
		// TODO Auto-generated method stub
		// Indexing the File details -- "new"
		if(rd.equalsIgnoreCase("new")){
			// checking for duplicate record in Index
			if(this.fileDictionary.containsKey(filename)){
				Collection<ArrayList<String>> delArrFile = this.fileDictionary.get(filename);
				for(ArrayList<String> als : delArrFile){
					if(als.get(1).equalsIgnoreCase(peerid)){
						// Duplicate record for Indexing. so Rejecting it
						return;
					}
				}
			}
			// If no duplicate, then indexing it
			ArrayList<String> arrFileDtl = new ArrayList<String>();
			arrFileDtl.add(filename);
			arrFileDtl.add(peerid);
			arrFileDtl.add(port_num);
			arrFileDtl.add(directory);
			arrFileDtl.add(sPeer);
			//System.out.println(arrFileDtl);
			this.fileDictionary.add(filename, arrFileDtl);
			this.supPeerId = sPeer;
		}
		
		// Updating the Index after deletion of a file -- "del"
		else if(rd.equalsIgnoreCase("del")){
			Collection<ArrayList<String>> delArrFile = new ArrayList<ArrayList<String>>();
			Collection<ArrayList<String>> onceMore = new ArrayList<ArrayList<String>>();
			
			// Checking if deleted File Name is present in Index
			if(this.fileDictionary.containsKey(filename)){
				delArrFile = this.fileDictionary.get(filename);
				for(ArrayList<String> als : delArrFile){
					// Removing the Peer Entry from Index
					if(als.get(1).equalsIgnoreCase(peerid)){
						//System.out.println(this.fileDictionary.get(filename));
						onceMore= this.fileDictionary.remove(filename);
						//System.out.println("BEFORE"+onceMore);
						onceMore.remove(als);
						//System.out.println("AFTER" + onceMore);
						for (ArrayList<String> p : onceMore){
							this.fileDictionary.add(filename,p);							
						}
						System.out.println("Index Server Updated & Specified Record Deleted");
					}
				}
			}
			else{
				System.out.println("Delete Request: No entry detected for filename");
			}
		}
		
		else{
			System.out.println("Invalid Request.");
		}
		// Displaying the Index after every Addition or Removal of Entry.
		System.out.println("####################################");
		System.out.println("THE UPDATED INDEX at " + dateFormat.format(date));
		for (Entry<String, List<ArrayList<String>>> entry : this.fileDictionary.entrySet()) {
		    System.out.println(entry.getKey() + " => " + entry.getValue());
		}
		System.out.println("####################################");
	}

	// Searching specified Filename entry from Index and returns a Collection of ArrayList
	@Override
	public synchronized Collection<ArrayList<String>> searchFile(String filename) throws RemoteException {
		// TODO Auto-generated method stub
		Collection<ArrayList<String>> resultArrFile = new ArrayList<ArrayList<String>>();
		if(this.fileDictionary.containsKey(filename)){
			resultArrFile = this.fileDictionary.get(filename);
		}
		return resultArrFile;
	}

	@Override
	// This Query function is for All to All Topology and linear Topology
	public void query(String msgId, int TTL, String filename, String reqPeerId, String reqPortNum)
			throws RemoteException {
		if(TTL>0 && TTL != 0){
			TTL=TTL-1;
			// Inserting request details into HashMap at Server
			ArrayList<String> upStreamDtl = new ArrayList<String>();
			upStreamDtl.add(msgId);
			upStreamDtl.add(Integer.toString(TTL));
			upStreamDtl.add(reqPeerId);
			upStreamDtl.add(reqPortNum);
			this.myMap.put(msgId, upStreamDtl);

			// displaying all the request details 
			for (Entry<String, ArrayList<String>> entry : this.myMap.entrySet()) {
			    System.out.println(entry.getKey() + " => " + entry.getValue());
			}
			// Searching the requested file
			Collection<ArrayList<String>> resultLocal = this.searchFile(filename);
			if(!resultLocal.isEmpty()){
				try{
					// Locating Registry of Requested Super peer or leaf node 	
					Registry regis = LocateRegistry.getRegistry("localhost",Integer.parseInt(this.myMap.get(msgId).get(3)));
					String ref = msgId.substring(0, msgId.indexOf(":"));              // Message id - PeerId:SequenceNumber
					// If caller is leaf node or super node
					if (ref.equalsIgnoreCase(reqPeerId)){
						// Calling Leaf node interface methods
						LeafNodeInterface pInter = (LeafNodeInterface) regis.lookup("root://LeafNode/"+this.myMap.get(msgId).get(3)+"/FS");

						//System.out.println("Status from Leaf Node :" + pInter.queryHit(msgId,TTL,filename,resultLocal));
						if(pInter.queryHit(msgId,TTL,filename,resultLocal)){
							System.out.println("Output Send to Leaf Node");
						}
						else{
							System.out.println("Some exception might have occured at Leaf Node or TTL expired.");
						}
					}
					else{
						System.out.println("Calling Super Peer Caller");
						SuperPeerInterface spInter = (SuperPeerInterface) regis.lookup("root://SuperPeer/"+this.myMap.get(msgId).get(3));
						spInter.queryHit(msgId,TTL,filename,resultLocal);
					}
				}
				catch(Exception e){
						System.out.println("Exception at its own SuperPeer query function : " + e.getMessage());
					}
			}
			else{
				System.out.println("FOUND NOTHING in this SuperPeer");
			}
			
			// Get Local and remote Super peer port number
			String remoteSupPeerPortNum = null;
			String localSupPeerPortNum = null;

			// Reading the config file.
			SetupConfig sc;
			try {
				sc = new SetupConfig();
				// Getting Calling Super Peer Port number
				for (GetSuperPeerDetails sp : sc.arrSPD){
					if(sp.getPeer_ID().equalsIgnoreCase(this.supPeerId)){
						localSupPeerPortNum= sp.getPeer_Port();
						break;
					}
				}
				String callingLeafId = msgId.substring(0, msgId.indexOf(":"));
								
				if(sc.topology.equalsIgnoreCase("ALL")){
					System.out.println("WORKING IN ALL TO ALL TOPOLOGY");
					if(callingLeafId.equalsIgnoreCase(this.myMap.get(msgId).get(2))){
						for (GetTopologyDetails topo : sc.arrTD){
							if(topo.getPeer_ID().equalsIgnoreCase(this.supPeerId)){
								List<String> neighbourArr = Arrays.asList(topo.getAll_Neighbour().split("\\s*,\\s*"));
								System.out.println("Total Number of Neighbours in ALL TOPOLOGY : "+ neighbourArr.size());
							
							// Calling query function of Neighbours in ALL TOPOLOGY
								for (String spName : neighbourArr){
									// Getting Remote Super Peer Port Number
									for(GetSuperPeerDetails speer : sc.arrSPD){
										// Get port number of Super Peer
										if(speer.getPeer_ID().equalsIgnoreCase(spName)){
											remoteSupPeerPortNum = speer.getPeer_Port();
											break;
										}
									}
									// Calling Neighbouring query method
										try{
											Registry regis = LocateRegistry.getRegistry("localhost",Integer.parseInt(remoteSupPeerPortNum));
											SuperPeerInterface spInter = (SuperPeerInterface) regis.lookup("root://SuperPeer/"+remoteSupPeerPortNum);
											System.out.println("Calling Neighbour " + spName + " query() ");
											spInter.query(msgId, TTL, filename,this.supPeerId, localSupPeerPortNum);
										}
										catch(Exception e){
											System.out.println("Exception occured at calling Neighbour Query. Neighbour is : " + spName );
										}
								}
							break;
							}
							else{
								System.out.println("Didnt found Super Peer Info in Config file object.");
							}
						}	
					}
					else{
						System.out.println("No Need of broadcasting query messages to all Super Peers.");
					}
				}
				else{
					System.out.println("WORKING IN LINEAR TOPOLOGY");
					List<String> leafPeerIdArr = null;
					for (GetTopologyDetails topo : sc.arrTD){
						if(topo.getPeer_ID().equalsIgnoreCase(this.supPeerId)){
							String neighbour = topo.getLinear_Neighbour();							
							System.out.println("SuperPeer " + this.supPeerId + " have Neighbour in Linear TOPOLOGY : "+ neighbour);
														
							// Getting Remote Super Peer Port Number
								String spName = neighbour;
								for(GetSuperPeerDetails speer : sc.arrSPD){
									// Get port number of Super Peer
									if(speer.getPeer_ID().equalsIgnoreCase(spName)){
										remoteSupPeerPortNum = speer.getPeer_Port();
										leafPeerIdArr = Arrays.asList(speer.getLeaf_ID().split("\\s*,\\s*"));
										break;
									}
								}
								// Calling Neighbouring query method and making sure it doesn't call Caller's Super Peer back.
								if(!leafPeerIdArr.contains(callingLeafId)){
									try{
										Registry regis = LocateRegistry.getRegistry("localhost",Integer.parseInt(remoteSupPeerPortNum));
										SuperPeerInterface spInter = (SuperPeerInterface) regis.lookup("root://SuperPeer/"+remoteSupPeerPortNum);
										System.out.println("Calling Neighbour " + spName + " query() ");
										spInter.query(msgId, TTL, filename,this.supPeerId, localSupPeerPortNum);
									}
									catch(Exception e){
										System.out.println("Exception occured at calling Neighbour Query. Neighbour is : " + spName );
									}
								}
								else{
									System.out.println("No Need of forwarding query messages to Super Peers.");
								}		
							break;
						}
						else{
							System.out.println("Didnt found Super Peer Info in Config file object.");
						}
					}
					
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("IOException occured while reading the property file in SuperPeer Query.");
			}
		}
		else{
			System.out.println("Time to Live of a Message has expired at its own Super Peer. This Message is no longer valid.");
		}
}

	@Override
	public synchronized void queryHit(String msgId, int TTL, String filename, Collection<ArrayList<String>> resultArr)
			throws RemoteException {
		
		if(TTL>0 && TTL != 0){
			try{
				TTL=TTL-1;
				Registry regis = LocateRegistry.getRegistry("localhost",Integer.parseInt(this.myMap.get(msgId).get(3)));
				String ref = msgId.substring(0, msgId.indexOf(":"));
				
				// to check whether leaf node calling has reached
				if (ref.equalsIgnoreCase(this.myMap.get(msgId).get(2))){
					// Calling Leaf node interface methods
					LeafNodeInterface pInter = (LeafNodeInterface) regis.lookup("root://LeafNode/"+this.myMap.get(msgId).get(3)+"/FS");

//					System.out.println("Status from Leaf Node :" + pInter.queryHit(msgId,TTL,filename,resultArr));
					if(pInter.queryHit(msgId,TTL,filename,resultArr)){
						System.out.println("Output Send to Leaf Node");
					}
					else{
						System.out.println("Some exception might have occured at Leaf Node or TTL expired.");
					}
				}
				else{
					SuperPeerInterface spInter = (SuperPeerInterface) regis.lookup("root://SuperPeer/"+this.myMap.get(msgId).get(3));
					spInter.queryHit(msgId,TTL,filename,resultArr);
				}
			}
			catch(Exception e){
				System.out.println("Exception at Remote SuperPeer queryhit function : " + e.getMessage());
			}
		}
		else{
			System.out.println("Time to Live of a Message has expired at remote SuperNode. This Message is no longer valid.");
		}
	}
}
