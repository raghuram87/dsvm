/*
 * Peersim-Gadget : A Gadget protocol implementation in peersim based on the paper
 * Chase Henzel, Haimonti Dutta
 * GADGET SVM: A Gossip-bAseD sub-GradiEnT SVM Solver   
 * 
 * Copyright (C) 2012
 * Deepak Nayak 
 * Columbia University, Computer Science MS'13
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package peersim.gossip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.jamal.JamalException;
import com.jamal.MatlabCaller;
import com.jamal.client.MatlabClient;

import jnipegasos.PrimalSVMWeights;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.*;
import peersim.cdsim.*;

/**
 * Class GadgetProtocol
 * Implements a cycle based {@link CDProcol}. It implements the Gadget algorithms
 * described in paper:
 * Chase Henzel, Haimonti Dutta
 * GADGET SVM: A Gossip-bAseD sub-GradiEnT SVM Solver   
 * 
 *  @author Deepak Nayak
 */
public class GadgetProtocol4 implements CDProtocol {
	/**
	 * New config option to get the learning parameter lambda for GADGET
	 * @config
	 */
	private static final String PAR_LAMBDA = "lambda";
	/**
	 * New config option to get the number of iteration for GADGET
	 * @config
	 */
	private static final String PAR_ITERATION = "iter";
	
	public static boolean flag = false;
	
	public static int t = 0;
	
	public static boolean optimizationDone = false;	

	/** Linkable identifier */
	protected int lid;
	/** Learning parameter for GADGET, different from lambda parameter in pegasos */
	protected double lambda;
	/** Number of iteration (T in gadget)*/
	protected int T;
	
	private int pushsumflag = 0;
	
	public static double[][] optimalB;
	
	public static boolean end = false;
	
	public static boolean pushsumobserverflag = false;
	
	private PrimalSVMWeights primalSVMWeights;
	
	private PrimalSVMWeights oldWeightVector;
	
	private double oldWeight;
	
	private boolean pushsum2_execute = true;
	
	private String protocol;

	/**
	 * Default constructor for configurable objects.
	 */
	public GadgetProtocol4(String prefix) {
		lambda = Configuration.getDouble(prefix + "." + PAR_LAMBDA, 0.01);
		T = Configuration.getInt(prefix + "." + PAR_ITERATION, 100);
		//T = 0;
		lid = FastConfig.getLinkable(CommonState.getPid());
		primalSVMWeights = new PrimalSVMWeights(new TreeMap<Integer, Double>());
		oldWeightVector = new PrimalSVMWeights(new TreeMap<Integer, Double>());
		protocol = Configuration.getString(prefix + "." + "prot", "pushsum1");
		
	}

	/**
	 * Returns true if it possible to deliver a response to the specified node,
	 * false otherwise.
	 * Currently only checking failstate, but later may be we need to check
	 * the non-zero transition probability also
	 */
	protected boolean canDeliverRequest(Node node) {
		if (node.getFailState() == Fallible.DEAD)
			return false;
		return true;
	}
	/**
	 * Returns true if it possible to deliver a response to the specified node,
	 * false otherwise.
	 * Currently only checking failstate, but later may be we need to check
	 * the non-zero transition probability also
	 */
	protected boolean canDeliverResponse(Node node) {
		if (node.getFailState() == Fallible.DEAD)
			return false;
		return true;
	}

	/**
	 * Clone an existing instance. The clone is considered 
	 * new, so it cannot participate in the aggregation protocol.
	 */
	public Object clone() {
		GadgetProtocol4 gp = null;
		try { gp = (GadgetProtocol4)super.clone(); }
		catch( CloneNotSupportedException e ) {} // never happens
		return gp;
	}
	
	private void pushsum1(Node node, PegasosNode pn, int pid) {
		PegasosNode peer = (PegasosNode)selectNeighbor(node, pid);
		if(Debug.ON) {
			//System.out.println("Node [" + pn.getID() + "] is gossiping with Node [" + peer.getID() + "]" );
		}
		// now add pn.wtvector and peer.wtvector
		//Iterator<Integer> n_it = pn.wtvector.getWeights().keySet().iterator();
		Iterator<Integer> p_it = peer.wtvector.getWeights().keySet().iterator();
		while (p_it.hasNext()) {
			// w and l are sorted
			Integer index = p_it.next();
			if(pn.wtvector.getWeights().containsKey(index)) {
				pn.wtvector.addFeature(index,  
						(peer.wtvector.getWeights().get(index) +
						pn.wtvector.getWeights().get(index))/2);
			}
			else {
				pn.wtvector.addFeature(index, 
						peer.wtvector.getWeights().get(index)/2);
			}
			peer.wtvector.addFeature(index, pn.wtvector.getWeights().get(index));				
			
		} // push sum done
		//if(node.getID()==0) {
		//}
	}
	protected List<Node> getPeers(Node node) {
		Linkable linkable = (Linkable) node.getProtocol(lid);
		if (linkable.degree() > 0) {
			List<Node> l = new ArrayList<Node>(linkable.degree());			
			for(int i=0;i<linkable.degree();i++) {
				l.add(linkable.getNeighbor(i));
			}
			return l;
		}
		else
			return null;						
	}	
	
	private void pushsum2(Node node, PegasosNode pn, int pid) {

		if(!pushsum2_execute) {
			Iterator<Integer> p_it = oldWeightVector.getWeights().keySet().iterator();
			while (p_it.hasNext()) {
				Integer index = p_it.next();
				pn.wtvector.addFeature(index,oldWeightVector.getWeights().get(index));
			}
			pn.weight = oldWeight;
			pushsum2_execute = !pushsum2_execute;		
			return;
		}
		
		Iterator<Integer> p_it1 = pn.wtvector.getWeights().keySet().iterator();
		while (p_it1.hasNext()) {
			// w and l are sorted
			Integer index = p_it1.next();
			oldWeightVector.addFeature(index,optimalB[(int)node.getID()][(int)node.getID()]*pn.wtvector.getWeights().get(index));
		}
		oldWeight = optimalB[(int)node.getID()][(int)node.getID()]*pn.weight;					
		List<Node> peers = getPeers(node);
		for(Node peer1:peers) {
			PegasosNode peer = (PegasosNode)peer1;
			Iterator<Integer> p_it = peer.wtvector.getWeights().keySet().iterator();
			while (p_it.hasNext()) {
				// w and l are sorted
				Integer index = p_it.next();
				if(oldWeightVector.getWeights().containsKey(index)) {
					oldWeightVector.addFeature(index,  
							optimalB[(int)peer.getID()][(int)node.getID()]*peer.wtvector.getWeights().get(index) +
							oldWeightVector.getWeights().get(index));
				}
				else {
					oldWeightVector.addFeature(index, 
							optimalB[(int)peer.getID()][(int)node.getID()]*peer.wtvector.getWeights().get(index));
				}
			}
			oldWeight += optimalB[(int)peer.getID()][(int)node.getID()]*peer.weight;												
		}// push sum done
		pushsum2_execute = !pushsum2_execute;
		
	}	
	
	private double[][] toMatrix(double[] array, int n) {
		double[][] mat = new double[n][n];
		for(int i=0;i<n;i++) {
			for(int j=0;j<n;j++) {
				mat[i][j] = array[i*n+j];
			}
		}
		return mat;
	}
	
	private void generateOptimalB() {
		int networkSize = Network.size();
		optimalB = new double[networkSize][networkSize];
		int[][] adjM = new int[networkSize][networkSize];
		for(int i=0;i<networkSize;i++) {
			Node n = Network.get(i);
			Linkable l = (Linkable) n.getProtocol(lid);
			for(int j=0;j<l.degree();j++) {
				Node ne = l.getNeighbor(j);
				adjM[i][(int)ne.getID()] = 1;				
			}			
		}
        try {
            MatlabClient matlabClient = new MatlabClient(
                    MatlabCaller.HOST_ADDRESS,
                    "/home/raghuram/MATLAB/R2012a/bin/matlab.exe",40);
            Object[] inArgs = new Object[1];
            inArgs[0] = adjM;
            Object[] outputArgs = matlabClient.executeMatlabFunction("fmmc",
                    inArgs, 2);
            //System.out.println("outputArgs[0]=" + Arrays.toString((double[])outputArgs[0]));
            double[] prob = (double[])outputArgs[0]; 
            optimalB = toMatrix(prob,networkSize);            
            matlabClient.shutDownServer();
 
        } catch (JamalException e) {
            e.printStackTrace();
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	private void printBMatrix() {
		int networkSize = Network.size();		
		for(int i=0;i<networkSize;i++) {
			for(int j=0;j<networkSize;j++) {			
				System.out.print(optimalB[i][j]+"|");
			}
			System.out.println();
		}
	}
	

	// Comment inherited from interface
	/**
	 * This is the method where actual algorithm is implemented. This method gets 
	 * called in each cycle for each node.
	 * NOTE: The Gadget algo's iteration corresponds to the inner loop, so call this 
	 * once only, i.e. keep simulation.cycles 1
	 */
	public void nextCycle(Node node, int pid) {
		
		int resetflagto = pushsumflag;

		PegasosNode pn = (PegasosNode)node;

		if(node.getID()==0 && pushsumflag == 0)	t++;
		
		if(protocol.equals("pushsum2")) {
			if(!optimizationDone) {
				generateOptimalB();
				printBMatrix();
				optimizationDone = true;
				GadgetProtocol3.writeIntoFile(System.currentTimeMillis());
			}		
		}

		if(t>T) {
			end = true;
			for (Map.Entry<Integer, Double> entry : pn.wtvector.getWeights().entrySet()) {
				pn.wtvector.addFeature(entry.getKey(), primalSVMWeights.getWeights().get(entry.getKey())/T);		
			}			
			return;
		}
		else if(pushsumflag == 1 && !pushsumobserverflag) {
			if(protocol.equals("pushsum2"))
				pushsum2(node, pn, pid);
			else
				pushsum1(node, pn, pid);				
			return;
		}
		else if(pushsumflag == 1) {
			double scale = Math.min(1.0, 1.0 / (Math.sqrt(lambda) * pn.wtvector.getL2Norm()));
			for (Map.Entry<Integer, Double> entry : pn.wtvector.getWeights().entrySet()) {
				pn.wtvector.addFeature(entry.getKey(), scale * entry.getValue());
				if(primalSVMWeights.getWeights().containsKey(entry.getKey())) {
					primalSVMWeights.getWeights().put(entry.getKey(), primalSVMWeights.getWeights().get(entry.getKey())+scale * entry.getValue());
				}
				else {
					primalSVMWeights.getWeights().put(entry.getKey(), scale * entry.getValue());					
				}
				// also set the peers weight same
				//peer.wtvector.addFeature(entry.getKey(), (1.0 + scale) * entry.getValue());			
			}
			// normalize both weights
			//pn.wtvector.normalizeWeights();
			//peer.wtvector.normalizeWeights();
			resetflagto = 0;		
			if(protocol.equals("pushsum2")) {
				pushsum2_execute = true;
				pn.weight = pn.traindataset.length;
				Iterator<Integer> p_it = pn.wtvector.getWeights().keySet().iterator();
				while (p_it.hasNext()) {
					Integer index = p_it.next();
					pn.wtvector.addFeature(index,pn.wtvector.getWeights().get(index)/pn.weight);
				}					
			}
		}
		else if(pushsumflag == 0) {

		TreeMap<Integer, Double> L = new TreeMap<Integer, Double>();
		
		//System.out.println("current node ID: [" + pn.getID() + "]");

		int N = pn.traindataset.length;	// #data points
		double y;	// label
		// reset the weights at start as in the first line of GADGET
	        //pn.wtvector.resetWeights();
		if(flag==false) {
			GadgetProtocol.writeIntoFile(String.valueOf(System.currentTimeMillis()));
			flag = true;
		}
			pn.misclassified = 0;	// reset the misclassified count in each iter
			for (int n = 0; n < N; n++) { // data point loop
				y = pn.traindataset[n].getLabel();
				int xsize = pn.traindataset[n].size();
				double dotprod = 0.0;

				// calculate <w,x> using two iterator which moves over x[i] and w
				for (int xiter = 0; xiter < xsize; xiter++) { // dot product loop
					int xdim = pn.traindataset[n].getDimAt(xiter);
					double xval = pn.traindataset[n].getValueAt(xiter);
					if(pn.wtvector.getWeights().containsKey(xdim)) {// wtvector has this dim
						double wval = pn.wtvector.getWeights().get(xdim);
						dotprod += xval * wval;
					}

				}// dot product loop end
				if ((y * dotprod) < 1) { // this point is in Si+
					if((y * dotprod) < 0) pn.misclassified++;
						//pn.misclassified++;
					// Li calculated.
					for(int xiter = 0; xiter < xsize; xiter++) {// xsize loop
						int xkey = pn.traindataset[n].getDimAt(xiter);
						double xval = pn.traindataset[n].getValueAt(xiter);
						if(L.containsKey(xkey)) {
							L.put(xkey, L.get(xkey) + y * xval);
						}
						else
							L.put(xkey, y * xval);
					}//xsize loop
				}

			} // data point loop end
			if(Debug.ON) {
				System.out.println("[DEBUG] #misclassified at node[" + pn.getID() + "] : "
							+ pn.misclassified);
			}
			double alpha = 1.0 / (lambda * t); // our loop starts from 0
			//calculate w_t1/2, what is ni??
			Iterator<Integer> w_it = pn.wtvector.getWeights().keySet().iterator();
			Iterator<Integer> l_it = L.keySet().iterator();
			// Lots of confusion, so do it in two step
			// inefficient but clean
			while (w_it.hasNext()) {
				Integer index = w_it.next();
				// not sure if first term should be multiplied by N
				double newval = (1 - lambda * alpha) * N * pn.wtvector.getWeights().get(index);
				//double newval = (1 - lambda * alpha) * pn.wtvector.getWeights().get(index);
				pn.wtvector.addFeature(index, newval);		
			}
			while (l_it.hasNext()) {
				Integer index = l_it.next();
				double lossterm = L.get(index);
				if(pn.wtvector.getWeights().containsKey(index)) {
					pn.wtvector.addFeature(index, alpha * lossterm + 
							pn.wtvector.getWeights().get(index));
				}
				else {
					pn.wtvector.addFeature(index, alpha * lossterm);
				}
			} // ~w_t1/2 calculated, now do push sum
			//System.out.println(pn.wtvector.getWeights().firstEntry().getValue()+",,,,,,,,,,,,");

			resetflagto = 1;
			pushsumobserverflag = false;
		}
		pushsumflag = resetflagto;
			//System.out.println("[iteration: " + t + "] local weight norm at node [" +
			// 	pn.getID() + "] : " + pn.wtvector.getL2Norm() );				
			//System.out.println("[iteration: " + t + "] local weight norm at node [" +
			// 	peer.getID() + "] : " + peer.wtvector.getL2Norm() );
			// now wtvector contains the final weights
		//}//iteration loop end
		
	}

	/**
	 * Selects a random neighbor from those stored in the {@link Linkable} protocol
	 * used by this protocol.
	 */
	protected Node selectNeighbor(Node node, int pid) {
		Linkable linkable = (Linkable) node.getProtocol(lid);
		if (linkable.degree() > 0) 
			return linkable.getNeighbor(
					CommonState.r.nextInt(linkable.degree()));
		else
			return null;
	}

}
