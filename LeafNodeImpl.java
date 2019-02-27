package com.gfiletransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Timer;

public class LeafNodeImpl implements LeafNodeInterface {

	String portNo = null; // Port no. of the peer
	String dirName = null; //Directory where the files are to be stored.
	String fileName = null; //the file to be searched.
	String remotePeer= null; //Peer from whom file has to be downloaded.
	String superpeer = null; // name of super peer or id
	String peerID = null; //peerID (fetch and set it from property file)
	int seqNum = -1;
	int timeTL = 20; // 3 TTL for All to All Topology and 22 TTL for Linear Topology
	
	Collection<ArrayList<String>> finalRes = new ArrayList<ArrayList<String>>();
	
	LeafNodeImpl(String portNo, String dirName, String superpeer, String peerID){
		this.portNo = portNo;
		this.dirName = dirName;
		this.superpeer = superpeer;
		this.peerID = peerID;
	}
	
	// ###########################
	
	public void doWork() throws IOException {
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
			System.out.println("Do you want to Search a File, Delete File or Exit? (Search/Delete/Exit)");
			String sd= sc.nextLine();
			while(!sd.equalsIgnoreCase("Exit")){
				if(sd.equalsIgnoreCase("Delete")){
					//Deleting a File from local peer's Directory
					String wantToDel="";
					while(!wantToDel.equalsIgnoreCase("No")){
						System.out.println("Enter the file name which you want to delete");
						String fname = sc.nextLine();
						if(fname!=null){
							File fileToDel = new File(dirName+"\\"+fname);
							// Delete the specified file from local peer's directory
							if(fileToDel.delete()){
								System.out.println("File deleted Successfully.");
								// Updating the index server about the deleted file
								spInter.registryFiles("del",fname, peerID, portNo, dirName,superpeer);
							}
							else{
								System.out.println("Failed to delete the File");
							}
						}
						else{
							System.out.println("Please Enter a Filename");
						}
						System.out.println("Do you want to delete more files? (Yes/No)");
						wantToDel=sc.nextLine();
					}
				}
				else if(sd.equalsIgnoreCase("Search")){
					//Searching and downloading a File Code
					String ans= "";
					while(!ans.equalsIgnoreCase("No")){
						// Searching the file in Indexing server
						seqNum=seqNum+1;
						System.out.println("Enter the file name which you want to search");
						fileName = sc.nextLine();
						if(fileName!=null){
							String msgId = peerID + ":" + Integer.toString(seqNum);	
							
//							spInter.query(msgId, timeTL, fileName, peerID, portNo);
							
						// Adding a timer for 5 ms of searching
							ExecutorService service = Executors.newSingleThreadExecutor();
							try {
							    Runnable r = new Runnable() {
							        @Override
							        public void run() {
							        	try {
							        		System.out.println("Now Started Calling the query() from Leaf Node...");
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
						else{
							System.out.println("Please Enter a Filename");
						}		
						
						if(!finalRes.isEmpty()){
							// Displaying Peers List which can provide the requested file
							System.out.println("#################################################");
							System.out.println("\n");
							for(ArrayList<String> als : finalRes){
								if(als.get(0).equalsIgnoreCase(fileName)){
									System.out.println("Peer providing the file with Peer ID is "+ als.get(1)+ " which is in Super Peer :" + als.get(4));									
								}
							}
							System.out.println("\n");
							System.out.println("#################################################");
							// Choosing one of the returned Peer
							System.out.println("Enter Peer ID you wish to take the file from");
							remotePeer = sc.nextLine();
							
							// Downloading the file from Specified Peer
							int co=finalRes.size();
							if(remotePeer!=null){
								for(ArrayList<String> als : finalRes){
									if(als.get(1).equalsIgnoreCase(remotePeer)){
										
										// Looking up from the Registry for Selected Peer
										Registry regis2 = LocateRegistry.getRegistry("localhost", Integer.parseInt(als.get(2)));
										LeafNodeInterface lnInter = (LeafNodeInterface) regis2.lookup("root://LeafNode/"+als.get(2)+"/FS");	
										
										// Calling Remote File Download method of Selected Peer
										byte[] output= lnInter.fileDownload(als);
										System.out.println(output.length);
										
										// Converting Downloaded byte array into file
										if(output.length!=0){
											FileOutputStream ostream = null;
											try {
												ostream = new FileOutputStream(dirName+"\\"+fileName);
											    ostream.write(output);
											    System.out.println("File Downloading Successful.");
											    System.out.println("Display File " + fileName);
											    //Updating the IndexServer Indexes after downloading the file.
											    spInter.registryFiles("new",fileName, peerID, portNo, dirName,superpeer);
											}
											catch(Exception e){
												System.out.println("Exception in bytearray to file conversion. " + e.getMessage());
											}
											finally {
											    ostream.close();
											}											
										}
										else{
											System.out.println("File is not present at Remote Location.");
										}
										break;
									}
									else{
										if(co==1)
											System.out.println("Peer with that ID " + remotePeer + " does not exist. Please choose proper PeerId.");
									}
								co--;
								}
							}
							else{
								System.out.println("Please enter proper Peer ID");
							}
						}
						else{
							System.out.println("Sorry, File which you are searching doesnt exist in our Server.");
						}
						System.out.println("Do you want to search again ? (Yes/No)");
						ans=sc.nextLine();
					}				
				}
				else{
					System.out.println("Please select appropriate choice");
				}				
				System.out.println("Do you want to Search a File, Delete File or Exit? (Search/Delete/Exit)");
				sd= sc.nextLine();
			}
			System.exit(0);
		}catch(Exception e) {
			System.out.println("Exception at Client Interface: " + e.getMessage());
		}
	}

	@Override
	public byte[] fileDownload(ArrayList<String> searchedDir) throws RemoteException{
		//0 filename, 1 peerid, 2 port_num, 3 direct, 4 superpeer id
		String fname = searchedDir.get(0);
		String remoteDir=searchedDir.get(3);
		try {
	         File file = new File(remoteDir+"\\"+fname);
//	         System.out.println(file.exists());
	         if(file.exists()){
	     		byte buffer[] = Files.readAllBytes(file.toPath());
		         return buffer;	        	 
	         }
	      }
		catch(Exception e){
	         System.out.println("Error in File download part " + e.getMessage());
	         e.printStackTrace();
	         return new byte[0];
	      }
		return new byte[0];
	}
	@Override
	public synchronized boolean queryHit(String msgId, int TTL, String filename, Collection<ArrayList<String>> resultArr)
			throws RemoteException {
		if(TTL>0 && TTL != 0){
			try{
				System.out.println("this result is going in finalRes "+ resultArr);
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
