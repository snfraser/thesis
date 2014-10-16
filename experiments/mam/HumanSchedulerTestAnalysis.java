import ngat.util.*;
import ngat.phase2.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import ngat.oss.simulation.*;
import ngat.icm.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.rmi.*;

/** Reads in the results of the HST for analysis.
 */
public class HumanSchedulerTestAnalysis {

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    static {
	sdf.setTimeZone(UTC);
    }

    public static void main(String args[]) {

	try {

	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
	    
	    Logger logger = LogManager.getLogger("SIM");
	    logger.setLogLevel(config.getIntValue("log-level", 3));
	    LogHandler con = new ConsoleLogHandler(new SimulationLogFormatter());
	    con.setLogLevel(config.getIntValue("log-level", 3));
	    logger.addHandler(con);

	    // Site.
	    Site site = new Site(config.getProperty("site"), 
				 Math.toRadians(config.getDoubleValue("lat")), 
				 Math.toRadians(config.getDoubleValue("long")));

	    // Instruments.
	    InstrumentRegistry instruments = (InstrumentRegistry)Naming.lookup("rmi://localhost/InstrumentRegistry");
	    	    
	    // ODB Phase2
	    String root = config.getProperty("root");
	 
	    Phase2ModelProvider provider = (Phase2ModelProvider)Naming.
		lookup("rmi://localhost/"+root+"_Phase2ModelProvider");
	    Phase2Model phase2Model = provider.getPhase2Model();

	    long start = (sdf.parse(config.getProperty("start"))).getTime();

	    // How many nights
	    int nn = config.getIntValue("nights", 1);
	    long end = start + nn*24*3600*1000L;

	    // Exec model
	    File bxf = new File(config.getProperty("exec")); // exec model properties
            BasicExecutionTimingModel execModel = new BasicExecutionTimingModel(site, instruments);
	    PropertiesConfigurator.use(bxf).configure(execModel);

	    // Allow 48 day for this test...
            execModel.setExternalTimeConstraint(end + 48*24*3600*1000L, "End of simulation");
	  
	    // History
            BasicHistoryModel histModel = new BasicHistoryModel();
	    histModel.loadHistory(phase2Model, start);

	    // Accounting
	    BasicAccountingModel accModel = new  BasicAccountingModel();
	    accModel.loadAccounts(phase2Model, start);

	    // Env model.
	    EnvironmentSnapshot env = new EnvironmentSnapshot();
	    env.seeing = Group.EXCELLENT;
	    env.photom = true;

	    // Load mapping info.
	    Properties map = new Properties();
	    File  mfile = new File(config.getProperty("map"));
	    map.load(new FileInputStream(mfile));

// 	    Enumeration e = map.propertyNames();
// 	    while (e.hasMoreElements()) {
// 		String key = (String)e.nextElement();
// 		String gpath = map.getProperty(key);
// 		System.err.println("Map: "+key+" -> "+gpath);

// 		Path path = new Path(gpath);
// 		String ppath = "/"+path.getRootByName()+"/"+path.getTagByName()+"/"+path.getUserByName()+"/"+path.getProposalByName();
// 		System.err.println("Checking for proposal identified by: "+ppath);

// 		Proposal proposal = phase2Model.getProposal(ppath);

// 		System.err.println("Extracted proposal: "+proposal);

// 		// now find that group.

// 		Group group = proposal.findGroup(path.getGroupByName());

// 		System.err.println("Found group: "+group);

// 	    }

	    // Load humanoid schedule info.
	    // <yyyy-MM-dd HH:mm> groupID 

	    File sfile = new File(config.getProperty("schedule"));
	    BufferedReader sin = new BufferedReader(new FileReader(sfile));

	    // Format: <date> <time> <gid>
	    String line = null;
	    int lc = 0;
	    while ((line = sin.readLine()) != null) {
		lc++;
		if (line.trim().equals(""))
		    continue;
		if (line.trim().startsWith("#"))
		    continue;
		StringTokenizer st = new StringTokenizer(line);
		if (st.countTokens() < 3)
		    throw new IllegalArgumentException("Line: "+lc+" missing args "+st.countTokens()+" expected 3");
		
		String sdat = st.nextToken()+" "+st.nextToken();
		long time = (sdf.parse(sdat)).getTime();
		String sgid = st.nextToken();
		
		// find the group in p2model...
		String gpath = map.getProperty(sgid);
		System.err.println("Map: "+sgid+" -> "+gpath);

		Path path = new Path(gpath);
		String ppath = "/"+path.getRootByName()+"/"+path.getTagByName()+"/"+path.getUserByName()+"/"+path.getProposalByName();
		System.err.println("Checking for proposal identified by: "+ppath);

		Proposal proposal = phase2Model.getProposal(ppath);

		System.err.println("Extracted proposal: "+proposal);

		// now find that group.
		System.err.println("Looking for group: "+path.getGroupByName());

		Group group = proposal.findGroup(path.getGroupByName());

		System.err.println("Found group: "+group);

		System.err.println("Exec group: "+group.getFullPath()+" at "+(new Date(time)).toGMTString());

		// is it actually feasible
		ExecutionStatistics hist = histModel.getExecutionStatistics(group);
		
		if (execModel.canDo(group, time, env, hist)) {
		    System.err.println("Execution is feasible at that time");
		} else {
		    System.err.println("Execution is NOT feasible at that time");
		}

		Iterator iob = group.listAllObservations();
		while (iob.hasNext()) {
		    Observation obs = (Observation)iob.next();
		    Source src = obs.getSource();
		    if (src != null) {
			System.err.println("Target: "+src);
			Position tgt = src.getPosition();
			double alt = tgt.getAltitude(time, site);
			System.err.println("Target elev: "+Position.toDegrees(alt,3));
		    }
		}
	    }



	} catch (Exception e) {
	    e.printStackTrace();
	}
	
    }

}
