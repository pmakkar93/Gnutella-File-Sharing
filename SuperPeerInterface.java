package com.gfiletransfer;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

public interface SuperPeerInterface extends Remote{
	public void registryFiles(String rd,String filename, String peerid, String port_num, String directory,String sPeer) throws RemoteException;
	public Collection<ArrayList<String>> searchFile(String filename) throws RemoteException;
	public void query(String msgId, int TTL, String filename,String reqPeerId,String reqPortNum) throws RemoteException;
	public void queryHit(String msgId, int TTL, String filename,Collection<ArrayList<String>> resultArr) throws RemoteException;
}
