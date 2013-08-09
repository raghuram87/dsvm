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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
public class GadgetProtocol2 implements CDProtocol {
	/**
	 * New config option to get the learning parameter lambda
	 * @config
	 */
	private static final String PAR_LAMBDA = "lambda";
	/**
	 * New config option to get the number of iteration
	 * @config
	 */
	private static final String PAR_ITERATION = "iter";


	/** Linkable identifier */
	protected int lid;
	/** Learning parameter */
	protected double lambda;
	/** Number of iteration (T in gadget)*/
	protected int iter;

	/**
	 * Default constructor for configurable objects.
	 */
	public GadgetProtocol2(String prefix) {
		lambda = Configuration.getDouble(prefix + "." + PAR_LAMBDA, 0.1);
		iter = Configuration.getInt(prefix + "." + PAR_ITERATION);
		lid = FastConfig.getLinkable(CommonState.getPid());
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
		GadgetProtocol gp = null;
		try { gp = (GadgetProtocol)super.clone(); }
		catch( CloneNotSupportedException e ) {} // never happens
		return gp;
	}

	// Comment inherited from interface
	/**
	 * This is the method where actual algorithm is implemented. This method gets 
	 * called in each cycle for each node.
	 */
	public void nextCycle(Node node, int pid) {
		TreeMap<Integer, Double> L = new TreeMap<Integer, Double>();
		PegasosNode pn = (PegasosNode)node;
		
		System.out.println("current node ID: [" + pn.getID() + "]");

		int N = pn.traindataset.length;	// #data points
		double y;	// label
		for (int i = 0; i < iter; i++) { // iteration loop
			// calculate Li, it is sum of y*x for x where
			// y*<w.x> < 1
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
			double alpha = 1.0 / (lambda * (i+1)); // our loop starts from 0
			//TODO calculate w_t1/2, what is ni??
			Iterator<Integer> w_it = pn.wtvector.getWeights().keySet().iterator();
			Iterator<Integer> l_it = L.keySet().iterator();
			// Lots of confusion, so do it in two step
			// inefficient but clean
			while (w_it.hasNext()) {
				Integer index = w_it.next();
				double newval = (1 - lambda * alpha) * N * pn.wtvector.getWeights().get(index);
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
					pn.wtvector.addFeature(index, lossterm);
				}
			} // ~w_t1/2 calculated, now do push sum
			PegasosNode peer = (PegasosNode)selectNeighbor(node, pid);
			System.out.println("Node [" + pn.getID() + "] is gossiping with Node [" + peer.getID() + "]" );
			// now add pn.wtvector and peer.wtvector
			//Iterator<Integer> n_it = pn.wtvector.getWeights().keySet().iterator();
			Iterator<Integer> p_it = peer.wtvector.getWeights().keySet().iterator();
			while (p_it.hasNext()) {
				// w and l are sorted
				Integer index = p_it.next();
				if(pn.wtvector.getWeights().containsKey(index)) {
					pn.wtvector.addFeature(index,  
							peer.wtvector.getWeights().get(index) +
							pn.wtvector.getWeights().get(index));
				}
				else {
					pn.wtvector.addFeature(index, 
							peer.wtvector.getWeights().get(index));
				}
			} // push sum done
			double scale = Math.min(1.0, 1.0 / (Math.sqrt(lambda) * pn.wtvector.getL2Norm()));
			for (Map.Entry<Integer, Double> entry : pn.wtvector.getWeights().entrySet()) {
				pn.wtvector.addFeature(entry.getKey(), (1.0 + scale) * entry.getValue());
				// also set the peers weight same
				peer.wtvector.addFeature(entry.getKey(), (1.0 + scale) * entry.getValue());				
			}
			// now wtvector contains the final weights
		}//iteration loop end
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