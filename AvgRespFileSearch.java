package com.gfiletransfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AvgRespFileSearch implements Runnable, LeafNodeInterface{

	String portNo = null; // Port no. of the peer
	String dirName = null; //Directory where the files are to be stored.
	String fileName = "COPYING.txt"; //the file to be searched.
	String remotePeer= null; //Peer from whom file has to be downloaded.
	String superpeer = null; // name of super peer or id
	String peerID = null; //peerID (fetch and set it from property file)
	int seqNum = -1;
	int timeTL = 20; // 3 TTL for All to All Topology and 22 TTL for Linear Topology
	
	Collection<ArrayList<String>> finalRes = new ArrayList<ArrayList<String>>();	
	long start = 0;
	long end = 0;
	long responseTime = 0;
	int seqReq=200;
	
	
	AvgRespFileSearch(String portNo, String dirName, String superpeer, String peerID){
		this.portNo = portNo;
		this.dirName = dirName;
		this.superpeer = superpeer;
		this.peerID = peerID;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		doWork();
	}	
	public void doWork() {
	
		String superPeerPort = null;
		// Reading Super Peer Port details from property file for connecting to Indexing server(SP)
	    SetupConfig scg;
		try {
			scg = new SetupConfig();
			// Getting Calling Super Peer Port number
			for (GetSuperPeerDetails sp : scg.arrSPD){
				if(sp.getPeer_ID().equalsIgnoreCase(this.superpeer)){
					superPeerPort = sp.getPeer_Port();
					break;
				}
			}
		}
		catch (IOException e1) {
			System.out.println("IOException occured while reading the property file at connecting to Super Peer.");
		}
		
		try{
			// Locating Registry of Indexing Server and obtains target address 
			Registry regis = LocateRegistry.getRegistry("localhost", Integer.parseInt(superPeerPort));
			SuperPeerInterface spInter = (SuperPeerInterface) regis.lookup("root://SuperPeer/"+superPeerPort);
			Scanner sc = new Scanner(System.in);
//			System.out.println("Enter Your Peer ID");
//			peerID = sc.nextLine();
			
			//obtain directory name where file is located
			File dirList = new File(dirName);
			//list of all records in the directory
			String[] record = dirList.list();

			// Registering Files in Index Server
			for(int c=0; c < record.length; c++){
				File currentFile = new File(record[c]);
				System.out.println("Registering details of File name " + currentFile.getName() + " in Indexing Server");
				spInter.registryFiles("new",currentFile.getName(), peerID, portNo, dirName,superpeer);
			}	
			// Running Sequential 500 search requests to Index server
			
            start = System.nanoTime();
            System.out.println("start is: " + start);
            for (int i=0;i<seqReq;i++){
    			seqNum=seqNum+1;
				String msgId = peerID + ":" + Integer.toString(seqNum);				
//				spInter.query(msgId, timeTL, fileName, peerID, portNo);
				// Adding a timer for 5 ms of searching
				ExecutorService service = Executors.newSingleThreadExecutor();
				try {
				    Runnable r = new Runnable() {
				        @Override
				        public void run() {
				        	try {
				        		//System.out.println("Now Started Calling the query() from Leaf Node...");
									spInter.query(msgId, timeTL, fileName, peerID, portNo);
				        		//System.out.println("Still running the run method");
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								System.out.println("TimeOut : It ran too long. Need to stop searching and continue.");
							}
				        }
				    };
				    Future<?> f = service.submit(r);
				    f.get(4, TimeUnit.SECONDS);     // attempt the search for 5 seconds
				}
				catch (final InterruptedException e) {
				    // The thread was interrupted during sleep, wait or join								
					System.out.println("Interrupted Exception Occured");
				}
				catch (final TimeoutException e) {
				    System.out.println("TimeOut Exception Occured. It ran too long. Need to stop searching and continue.");
				}
				catch (final ExecutionException e) {
				    // An exception from within the Runnable task
					System.out.println("Execution Exception Occured");
				}
				finally {
				    service.shutdownNow();
				}	
            }
            end= System.nanoTime();
            System.out.println("end is: " + end);
            responseTime= (end - start)/1000000; // Milliseconds
            //System.out.println("ResponseTime for PeerID " +peerID +" is "+ responseTime + " ms");
            float avgRespTime = (float)responseTime/seqReq;
            System.out.println("Avg Response time of PeerID " +peerID +" is "+avgRespTime + "ms");
            
			}catch(Exception e) {
				System.out.println("MultiClientFileSearch exception: " + e);
			}
	
	}
	@Override
	public byte[] fileDownload(ArrayList<String> searchedDir) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean queryHit(String msgId, int TTL, String filename, Collection<ArrayList<String>> resultArr)
			throws RemoteException {
		if(TTL>0 && TTL != 0){
			try{
				this.finalRes.addAll(resultArr);
				
				return true;
			}catch(Exception e){
					System.out.println("Exception at Peer's Interface " + e.getMessage());
					return false;
				}
		}
		else{
			System.out.println("Time to Live of a Message has expired at Leaf Node. This Message is no longer valid.");
			return false;
		}
	}
	}
