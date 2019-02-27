package com.gfiletransfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DataStructures {

	public static void main(String[] args) {
		Collection<ArrayList<String>> superpeer1Arr = new ArrayList<ArrayList<String>>();
		Collection<ArrayList<String>> superpeer2Arr = new ArrayList<ArrayList<String>>();

		Collection<ArrayList<String>> finalArr = new ArrayList<ArrayList<String>>();
		
		ArrayList<String> arrFileDtl = new ArrayList<String>();
		arrFileDtl.add("123sp11");
		arrFileDtl.add("1234sp11");
		arrFileDtl.add("12345sp11");
		superpeer1Arr.add(arrFileDtl);
		ArrayList<String> arrFileDtl2 = new ArrayList<String>();
		arrFileDtl2.add("123sp12");
		arrFileDtl2.add("1234sp12");
		arrFileDtl2.add("12345sp12");
		superpeer1Arr.add(arrFileDtl2);
		System.out.println("SUPER PEER 1 REPLY : "+superpeer1Arr.size()+" with value"+superpeer1Arr);
		///
		ArrayList<String> arrFileDtl3 = new ArrayList<String>();
		arrFileDtl3.add("123sp21");
		arrFileDtl3.add("1234sp21");
		arrFileDtl3.add("12345sp21");
		superpeer2Arr.add(arrFileDtl3);
		ArrayList<String> arrFileDtl4 = new ArrayList<String>();
		arrFileDtl4.add("123sp22");
		arrFileDtl4.add("1234sp22");
		arrFileDtl4.add("12345sp22");
		superpeer2Arr.add(arrFileDtl4);
		System.out.println("SUPER PEER 2 REPLY : "+superpeer2Arr.size()+" with value"+superpeer2Arr);
		//
		finalArr.addAll(superpeer1Arr);
		finalArr.addAll(superpeer2Arr);
		
		System.out.println("ALL SUPER PEER REPLIES : "+finalArr.size()+" with value"+finalArr);
		
		ExecutorService service = Executors.newSingleThreadExecutor();
		try {
		    Runnable r = new Runnable() {
		        @Override
		        public void run() {
		        	try {
		        		System.out.println("Now started running the run method");
						TimeUnit.SECONDS.sleep(5);
		        		System.out.println("Still running the run method");
					} catch (InterruptedException e) {
						System.out.println("Haha it got interrupted");
					}
		        }
		    };
		    Future<?> f = service.submit(r);
		    f.get(3, TimeUnit.SECONDS);     // attempt the task for two minutes
		}
		catch (final InterruptedException e) {
		    // The thread was interrupted during sleep, wait or join
			
		}
		catch (final TimeoutException e) {
		    System.out.println("It ran toooo Long");
		}
		catch (final ExecutionException e) {
		    // An exception from within the Runnable task
		}
		finally {
		    service.shutdownNow();
		}
		String msgId= "P02:40";
		String ref = msgId.substring(0, msgId.indexOf(":"));              // Message id - PeerId:SequenceNumber
		System.out.println("RESIDUAL" + ref);
		
		String str = "SP02,SP03,SP04,SP05,SP06,SP07,SP08,SP09,SP10";
		String str1 = "SP02";
		List<String> items = Arrays.asList(str1.split("\\s*,\\s*"));
		System.out.println(items.size());
		int i=0;
		for (String s : items){
			System.out.println("at this position " + i + " the value is " + s); // testing the array
			i = i+1;
		}

	}
}
