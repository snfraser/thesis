import ngat.oss.simulation.*;
import ngat.oss.simulation.metrics.*;

import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.phase2.*;
import ngat.icm.*;

import java.io.*;
import java.rmi.*;
import java.util.*;

public class ShowGroup {

    public static void main(String args[]) {

	try {
	    
	    ConfigurationProperties config =  CommandTokenizer.use("--").parse(args);
	    
	    String root = config.getProperty("root");
	    Phase2ModelProvider provider = (Phase2ModelProvider)Naming.
		lookup("rmi://localhost/"+root+"_Phase2ModelProvider");
	    Phase2Model phase2 = provider.getPhase2Model();
	    
	    String ppath = config.getProperty("proposal");

	    Proposal prop = phase2.getProposal(ppath);

	    String gname = config.getProperty("group");

	    Group g = prop.findGroup(gname);

	    System.err.println("Found: "+g);


	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
