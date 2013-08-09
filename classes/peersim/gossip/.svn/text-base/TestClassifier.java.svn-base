package peersim.gossip;

import java.io.File;
import java.net.MalformedURLException;

import jnipegasos.JNIPegasosInterface;
import jnipegasos.PrimalSVMWeights;
import jnisvmlight.LabeledFeatureVector;
import jnisvmlight.SVMLightInterface;

import com.martiansoftware.jsap.*;

public class TestClassifier {
	
	public static void main(String[] args) throws Exception {
		// Commandline parsing, get the model file name and datafile name
		// in commandline
		JSAP jsap = new JSAP();
		// longFlag datafile, longFlag modelfile
		FlaggedOption opt1 = new FlaggedOption("data")
									.setLongFlag("dataFile")
									.setRequired(true)
									.setStringParser(JSAP.STRING_PARSER);
		jsap.registerParameter(opt1);
		FlaggedOption opt2 = new FlaggedOption("model")
									.setLongFlag("modelFile")
									.setRequired(true)
									.setStringParser(JSAP.STRING_PARSER);
		jsap.registerParameter(opt2);
		FlaggedOption opt3 = new FlaggedOption("lambda")
									.setLongFlag("lambda")
									.setRequired(false)
									.setDefault("1.0")
									.setStringParser(JSAP.DOUBLE_PARSER);
		jsap.registerParameter(opt3);
		JSAPResult config = jsap.parse(args);
		
		String dataFile = config.getString("data");
		String modelFile = config.getString("model");
		double lambda = config.getDouble("lambda");	
		JNIPegasosInterface trainer = new JNIPegasosInterface();
		LabeledFeatureVector[] dataset;
		PrimalSVMWeights model;
		double objValue = 0.0;
		int zeroOneError = 0;
		double lossValue = 0.0;
		double normValue = 0.0;
		try {
			dataset = SVMLightInterface.getLabeledFeatureVectorsFromURL(new File(dataFile).toURL(), 0);
			int N = dataset.length;
			model = trainer.getWeightsfromFile(modelFile);
			// in test_objective or test_classify normValue is actually sqr of norm	
			normValue = model.getL2Norm();
			normValue *= normValue;
			objValue = normValue * lambda / 2.0;
			for(int n=0; n<N; n++) { // data point loop
				//curr_loss = 1 - model * data[i]
				double y = dataset[n].getLabel(); // label of nth data point
				int xsize = dataset[n].size(); // dimension of nth data point
				double dotprod = 0.0;
				for (int xiter = 0; xiter < xsize; xiter++) { // dot product loop
					int xdim = dataset[n].getDimAt(xiter);
					double xval = dataset[n].getValueAt(xiter);
					if(model.getWeights().containsKey(xdim)) {// wtvector has this dim
						double wval = model.getWeights().get(xdim);
						dotprod += xval * wval;
					}
				}// dot product loop end
				// calculate objective, loss, error, norm, etc
				double curr_loss = 1 - y*dotprod;
				if(curr_loss < 0) curr_loss = 0.0;
				lossValue += curr_loss;
				if((y*dotprod) < 0) zeroOneError++;
			}// data point loop end
			lossValue = lossValue/N;
			objValue += lossValue;
			System.out.println("Weights Norm Value:\t" + normValue);
			System.out.println("Objective Value:\t" + objValue);
			System.out.println("Loss Value:\t" + lossValue);
			System.out.println("Zero One Error:\t(" + zeroOneError + "/" + N + ") " + (1.0 * zeroOneError/N));
				
		}
		catch (MalformedURLException me) {
			me.printStackTrace();
		}
				
	}
		
}
