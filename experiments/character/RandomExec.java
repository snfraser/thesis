import ngat.util.*;
import ngat.phase2.*;
import ngat.oss.simulation.*;
import java.io.*;

public class RandomExec {

    public static void main(String args[]) {

	try {

	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

	    // Read in params to create a group
	    int nobs = config.getIntValue("nobs");
	    double expose = config.getDoubleValue("expose");
	    int mult = config.getIntValue("mult");

	    Group group = new Group("test");
	   
	    for (int i = 0; i < nobs; i++) {
		Observation obs = new Observation("obs-"+i);
		obs.setExposeTime((float)expose);
		obs.setNumRuns(mult);

		CCDConfig ccd = new CCDConfig("ccd");
		ccd.setLowerFilterWheel("lowf-"+i);
		ccd.setUpperFilterWheel("uppf-"+i);
		obs.setInstrumentConfig(ccd);

		ExtraSolarSource tgt = new ExtraSolarSource("ex");
		tgt.setRA(Math.random());
		tgt.setDec(Math.random());
		obs.setSource(tgt);
		
		Mosaic mos = new Mosaic();
		mos.setPattern(Mosaic.SINGLE);
		obs.setMosaic(mos);

		group.addObservation(obs);
	    }

	    // set base model to nullas we are only doing timing not feasibility.
	    BasicStochasticExecutionTimingModel sexm = new BasicStochasticExecutionTimingModel(null);
	    File sexf = new File(config.getProperty("sexf"));
	    PropertiesConfigurator.use(sexf).configure(sexm);
	   

	    // count the bins
	    int nbin = config.getIntValue("nbin");
	    long max = config.getLongValue("max"); // max time ms

	    int[] bin = new int[nbin];
	    for (int i = 0; i < nbin; i++) {
		bin[i] = 0;
	    }
	    double dt = (double)max/(0.5*(double)nbin);

	    sexm.option2 = (config.getProperty("option2") != null);
	    // run a load of tests
	    for (int j = 0; j < 100000; j++) {
		long et = sexm.getExecTime(group);
		//System.err.println("UO1"+(et/1000)+"s");
		int ibin = (int)((double)et/dt) + 1;
		bin[ibin] += 1;
		    
	    }

	    // results
	    for (int k = 0; k < nbin; k++) {
		double xt = (double)k*dt;
		System.err.println(""+(xt/1000)+" "+bin[k]);
	    }


	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
