import ngat.astrometry.*;
import ngat.util.*;
import ngat.oss.simulation.*;
import ngat.oss.simulation.metrics.*;
import java.io.*;
import java.util.*;

public class TestEnvFraction {
    
    public static void main(String args[]) {

	try {
	ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
	
	double lat = config.getDoubleValue("lat");
	double lon = config.getDoubleValue("long");
	
	Site site =  new Site("test", lat, lon);
	
	long start = (ScheduleSimulator.sdf.parse(config.getProperty("start"))).getTime();
	long end   = (ScheduleSimulator.sdf.parse(config.getProperty("end"))).getTime();
	
	File bepf = new File(config.getProperty("env")); // Environment properties	    
	BasicEnvironmentPredictor bep = new BasicEnvironmentPredictor();
	PropertiesConfigurator.use(bepf).configure(bep);  
	
	EnvironmentFractionProfile profile = EnvironmentFractionProfileCalculator.getFractionProfile(site, bep, start, end);

	int nn = profile.poor.length;
	for (int i = 0; i < nn; i++) {

	    long t = profile.start + i*86400000L;
	    
	    double tot =   
		profile.unusable[i]+
		profile.poor[i]+
		profile.average[i]+
		profile.excellent[i];
	
	    System.err.println(ScheduleSimulator.sdf.format(new Date(t))+" "+
			       (profile.unusable[i]/tot)+" "+
			       (profile.poor[i]/tot)+" "+
			       (profile.average[i]/tot)+" "+
			       (profile.excellent[i]/tot));
	}

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
