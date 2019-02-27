package com.gfiletransfer;

import java.io.IOException;
import java.util.ArrayList;

public class demoFile {

	public static void main(String[] args) throws IOException {
		
		SetupConfig scg = new SetupConfig();
		
		
		System.out.println(scg.arrPD.size());
		System.out.println(scg.arrSPD.size());
		System.out.println(scg.arrTD.size());
		System.out.println(scg.topology);
		
		for(GetPeerDetails g : scg.arrPD) {

			System.out.println(g.getPeer_ID());
			System.out.println(g.getPeer_Port());
			System.out.println(g.getSuperPeer());
			System.out.println(g.getDir());
		}
	}

}
