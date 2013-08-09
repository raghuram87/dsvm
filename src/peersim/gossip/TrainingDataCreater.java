package peersim.gossip;

import java.io.FileReader;
import java.io.LineNumberReader;

public class TrainingDataCreater {
	private static String resourcepath = "/home/raghuram/Downloads/pegasos/data";
	public static void main(String[] args) {
	String trainfilename = resourcepath + "/" + "train" + ".dat";
		
    final int start = 10;
    final int end = 50;
    try {
    final LineNumberReader in = new LineNumberReader(new FileReader(trainfilename));
    String line=null;
    while ((line = in.readLine()) != null && in.getLineNumber() <= end) {
        if (in.getLineNumber() >= start) {
            System.out.println(line);
        }
    }
	}
    catch(Exception e) {
    	
    }
	}
}
