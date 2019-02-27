package com.gfiletransfer;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

// For Calculating Average Search Response time
public class MultiClient {
public static void main(String [] args) throws IOException{
		
		String single = null;
		Scanner sc = new Scanner(System.in);
		
		System.out.println("Enter the choice for Single Client Evaluation or Multi Client (5 Clients) : ");
		single = sc.nextLine();

		if(single.equalsIgnoreCase("Single")){
			
			String clientPortno1=null;
			String directoryName1 = null;
			String sPeerID1 = null;
			
			System.out.println("Enter Peer ID ");
		    String peerId = sc.nextLine();
		    
		    SetupConfig scg;
			try {
				scg = new SetupConfig();
				// Getting Calling Super Peer Port number
				for (GetPeerDetails p : scg.arrPD){
					if(p.getPeer_ID().equalsIgnoreCase(peerId)){
						clientPortno1 = p.getPeer_Port();
						directoryName1=p.getDir();
						sPeerID1=p.getSuperPeer();
						break;
					}
				}			 
			}
			catch (IOException e1) {
				System.out.println("IOException occured while reading the property file at Leaf Node Initialization.");
			}

			try{ 
			     // Registering the peer on specified port & setting up the remote object
		    	 Registry registry = LocateRegistry.createRegistry(Integer.parseInt(clientPortno1));
		    	 AvgRespFileSearch lnImpl = new AvgRespFileSearch(clientPortno1, directoryName1, sPeerID1,peerId);
		    	 LeafNodeInterface lnInter = (LeafNodeInterface)UnicastRemoteObject.exportObject(lnImpl, 0);
				 registry.rebind("root://LeafNode/"+clientPortno1+"/FS", lnInter);
				 lnImpl.doWork();
			}
			catch(Exception e) {
		         System.err.println("FileServer exception: "+ e.getMessage());
		         e.printStackTrace();
		   }
		}
		// Running 5 Peers at once for performing the load testing.
		else if(single.equalsIgnoreCase("Multi")){
			String clientPortno1="1001";
		    String clientPortno2="1002";
		    String clientPortno3="1003";

			System.out.println("Please Enter a path for 1st Peer");
			String directoryName1 = sc.nextLine();
			System.out.println("Please Enter a path for 2nd Peer");
			String directoryName2 = sc.nextLine();
			System.out.println("Please Enter a path for 3rd Peer");
			String directoryName3 = sc.nextLine();

		    try{	       
		    	 Registry registry1 = LocateRegistry.createRegistry(Integer.parseInt(clientPortno1));
		    	 registry1 = LocateRegistry.createRegistry(Integer.parseInt(clientPortno2));
		    	 registry1 = LocateRegistry.createRegistry(Integer.parseInt(clientPortno3));

		    	 AvgRespFileSearch c1=new AvgRespFileSearch(clientPortno1, directoryName1,"SP01","P01");
		    	 AvgRespFileSearch c2=new AvgRespFileSearch(clientPortno2,directoryName2,"SP01","P02");
		    	 AvgRespFileSearch c3=new AvgRespFileSearch(clientPortno3,directoryName3,"SP01","P03");
		         
		         LeafNodeInterface pdInter1 = (LeafNodeInterface) UnicastRemoteObject.exportObject(c1,0);
		         LeafNodeInterface pdInter2 = (LeafNodeInterface) UnicastRemoteObject.exportObject(c2,0);
		         LeafNodeInterface pdInter3 = (LeafNodeInterface) UnicastRemoteObject.exportObject(c3,0);

				 registry1.rebind("root://LeafNode/"+clientPortno1+"/FS", pdInter1);
				 registry1.rebind("root://LeafNode/"+clientPortno1+"/FS", pdInter2);
				 registry1.rebind("root://LeafNode/"+clientPortno1+"/FS", pdInter3);

				 Thread thread1 = new Thread (c1); 
			     Thread thread2 = new Thread (c2);  
			     Thread thread3 = new Thread (c3);  
			     thread1.start();
			     thread2.start();
			     thread3.start();
			     
		 } catch(Exception e) {
		         System.err.println("FileServer exception: "+ e.getMessage());
		         e.printStackTrace();
		      }
		}
	}
}
