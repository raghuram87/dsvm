/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package peersim.gossip;

import java.util.Iterator;
import java.util.TreeMap;

import peersim.cdsim.CDProtocol;
import peersim.config.*;
import peersim.core.*;
import peersim.vector.*;
import peersim.util.IncrementalStats;

/**
 * Print statistics for Push Sum computation. Statistics printed
 * are defined by {@link IncrementalStats#toString}
 * 
 * @author Raghuram Nagireddy
 */
public class PushSumObserver implements Control {

    // /////////////////////////////////////////////////////////////////////
    // Constants
    // /////////////////////////////////////////////////////////////////////

    /**
     * Config parameter that determines the accuracy for standard deviation
     * before stopping the simulation. If not defined, a negative value is used
     * which makes sure the observer does not stop the simulation
     * 
     * @config
     */
    private static final String PAR_ACCURACY = "accuracy";

    /**
     * The protocol to operate on.
     * 
     * @config
     */
    private static final String PAR_PROT = "protocol";

    // /////////////////////////////////////////////////////////////////////
    // Fields
    // /////////////////////////////////////////////////////////////////////

    /**
     * The name of this observer in the configuration. Initialized by the
     * constructor parameter.
     */
    private final String name;

    /**
     * Accuracy for standard deviation used to stop the simulation; obtained
     * from config property {@link #PAR_ACCURACY}.
     */
    private final double accuracy;
    
    private String protocol;


    // /////////////////////////////////////////////////////////////////////
    // Constructor
    // /////////////////////////////////////////////////////////////////////

    /**
     * Creates a new observer reading configuration parameters.
     */
    public PushSumObserver(String name) {
        this.name = name;
        accuracy = Configuration.getDouble(name + "." + PAR_ACCURACY, -1);
        protocol = Configuration.getString(name + "." + "prot", "pushsum1");
        
    }

    // /////////////////////////////////////////////////////////////////////
    // Methods
    // /////////////////////////////////////////////////////////////////////

    /**
     * Print statistics for a Push Sum computation. Statistics
     * printed are defined by {@link IncrementalStats#toString}. The current
     * timestamp is also printed as a first field.
     * 
     * @return if the standard deviation is less than the given
     *         {@value #PAR_ACCURACY}.
     */
    public boolean execute() {
        //long time = peersim.core.CommonState.getTime();

//       IncrementalStats[] is = new IncrementalStats[size];
    	if(GadgetProtocol4.end) return true;
    	if(GadgetProtocol4.pushsumobserverflag) return false;

       TreeMap<Integer, IncrementalStats> is1 = new TreeMap<Integer, IncrementalStats>();
       String str = "";
        for (int i = 0; i < Network.size(); i++) {

            //SingleValue protocol = (SingleValue) Network.get(i)
              //      .getProtocol(pid);
        	Node n = Network.get(i);
        	PegasosNode pn1 = (PegasosNode) n;
        	/*if(i==1) {
        		System.out.println(pn1.wtvector.getWeights().firstEntry().getValue()+"...........");
        	}*/
    		Iterator<Integer> p_it = pn1.wtvector.getWeights().keySet().iterator();
    		//str += pn1.wtvector.getL2Norm() +"--";
        	int ct=0;
    		while (p_it.hasNext()) {
    			Integer index = p_it.next();
    			//if(ct==0 && i==0)
    			//str += pn1.wtvector.getWeights().get(index) +"--";
    			ct++;
    			if(!is1.containsKey(index)) {
    				is1.put(index,new IncrementalStats());   				
    			}
    			if(protocol.equals("pushsum2"))
    				is1.get(index).add(pn1.wtvector.getWeights().get(index)/pn1.weight);
    			else
    				is1.get(index).add(pn1.wtvector.getWeights().get(index));
    				
//    			is1.get(index).add(pn1.weight);
    			
        	}
    		str += pn1.weight+"--";
        }

        /* Printing statistics */
        //System.out.println(name + ": " + time + " " + is);

        /* Terminate if accuracy target is reached */
        boolean retVal = true;
        Iterator<Integer> is_it =  is1.keySet().iterator();
        double sum = 0,sum1=0;
        int n=0;
        int n1=0;
        while (is_it.hasNext())  {
			Integer index = is_it.next();
			sum += is1.get(index).getStD();
			//str += is1.get(index).getStD() +"--";
//			System.out.println(sum);
			//if(is1.get(index).getStD()>=0.1)
				//System.out.println(is1.get(index).getStD());
				//n1++;
				//sum1 += is1.get(index).getStD();
			//System.out.println(is1.get(index).getN());
			//if(n==345)
			//	str += is1.get(index).getStD() +"--";
			
			n++;
        	retVal = retVal && (is1.get(index).getStD() <= accuracy);
        }
        System.out.println(sum+"...................");
             
        if(retVal) {
        	System.out.println("Push-Sum converged...###########################!");
        	GadgetProtocol4.pushsumobserverflag = true;
        	return GadgetProtocol4.end;
        }
        else {
        	//System.out.println("Push-Sum not converged...#########!");        	
        	return false;
        }
    }
}
