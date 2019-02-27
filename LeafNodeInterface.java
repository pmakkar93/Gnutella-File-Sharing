package com.gfiletransfer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

public interface LeafNodeInterface extends Remote{
	public byte[] fileDownload(ArrayList<String> searchedDir) throws RemoteException;
	public boolean queryHit(String msgId, int TTL, String filename,Collection<ArrayList<String>> resultArr) throws RemoteException;
}
