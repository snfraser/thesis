import ngat.oss.simulation.*;
import ngat.oss.simulation.metrics.*;

import ngat.icm.*;
import ngat.phase2.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import java.io.*;
import java.rmi.*;
import java.util.*;
import java.awt.geom.*;

public class DemandProfileTest {

    public static void main(String args[]) {

	try {

	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

	    // Site.
            Site site = new Site(config.getProperty("site"),
                                 Math.toRadians(config.getDoubleValue("lat")),
                                 Math.toRadians(config.getDoubleValue("long")));

            // Instruments.
            InstrumentRegistry instruments = (InstrumentRegistry)Naming.lookup("rmi://localhost/InstrumentRegistry");

            // exec model
            File bxf = new File(config.getProperty("exec")); // exec model properties
	    BasicExecutionTimingModel bxtm = new BasicExecutionTimingModel(site, instruments);
	    PropertiesConfigurator.use(bxf).configure(bxtm);
	    
	    // typically we want to run from noon to noon on next day, input is 00Z on start date.
	    long start = config.getDateValue("start", "yyyy-MM-dd").getTime();
	    start += 12*3600*1000L;
	    long end = start + 24*3600*1000L;
	    
	    // Allow 365 day cutoff for this test...
	    bxtm.setExternalTimeConstraint(end + 365*24*3600*1000L, "End of simulation");
	    
	    // ODB
	    String root = config.getProperty("root");
	    File dbdir = new File(config.getProperty("base"));
	    
	    Logger logger = LogManager.getLogger("SIM");
	    logger.setLogLevel(config.getIntValue("log-level", 3));
	    ConsoleLogHandler console = new ConsoleLogHandler(new SimulationLogFormatter());
	    console.setLogLevel(config.getIntValue("log-level", 3));
	    logger.addHandler(console);
	    
	    // note we are switching in a specific P2Model provider 
	    // as we are changing the appropriate snapshot thro the caller script (hopefully)
	    BasicPhase2ModelProvider provider = new BasicPhase2ModelProvider(root, dbdir);
	    provider.loadCache();
	    
	    Phase2Model phase2 = provider.getPhase2Model();
	    
	    BasicHistoryModel history = new BasicHistoryModel();
	    history.loadHistory(phase2, start);
	    
	    BasicAccountingModel bam = new  BasicAccountingModel();
	    bam.loadAccounts(phase2, start);
	    
	    TimingConstraintWindowCalculator btcwc = new BasicTimingConstraintWindowCalculator(bxtm, 600000L);
	    
	    BasicMutableEnvironmentPredictor bep = new BasicMutableEnvironmentPredictor();
	    bep.setSeeing(Group.EXCELLENT);
	    bep.setPhotom(true);
	    
	    DemandCalculator dc = new DemandCalculator(site,
						       phase2,
						       history,
						       bam,
						       btcwc,
						       bep,
						       bxtm);

	    LoadCalculator lc = new LoadCalculator(site,
						   phase2,
						   history,
						   bam,
						   btcwc,
						   bep,
						   bxtm);

	    DemandStatistics ds = dc.generateDemandStatistics(start, end);
	    LoadStatistics   ls = lc.generateLoadStatistics(start, end);

	    File outfile = new File(config.getProperty("outfile"));
	    PrintStream pout = new PrintStream(new FileOutputStream(outfile));
	    pout.println("#SDATE "+ScheduleSimulator.sdf.format(new Date(start)));
	    pout.println("#EDATE "+ScheduleSimulator.sdf.format(new Date(end)));
	    pout.println("#C_D_AV "+   ds.average);
	    pout.println("#C_D_MAX "+  ds.maximum);
	    pout.println("#C_CD_AV "+  ds.critAverage);
	    pout.println("#C_CD_MAX "+ ds.critMaximum);
	    pout.println("#C_PD_AV "+  ds.priorityAverage);
            pout.println("#C_PD_MAX "+ ds.priorityMaximum);
	    pout.println("#C_LN "+     ls.count); // number of executables
	    pout.println("#C_CL "+     (ls.crit/ls.astro));  // critical load (rn=1)
	    pout.println("#C_L "+      (ls.total/ls.astro)); // total load
	    pout.println("#C_PL "+     (ls.pw/ls.astro));    // priority weighted load
	    pout.println("#C_UL "+     (ls.uw/ls.astro));    // urgency weighted load
	    // Demand Profile
	    Iterator ip = ds.listPoints();
	    while (ip.hasNext()) {
		Point2D.Double d = (Point2D.Double)ip.next();
		pout.println("C_D "+ScheduleSimulator.sdf.format(new Date((long)d.x))+" "+d.y);
	    }
	    // Crit demand profile
	    Iterator ipc = ds.listCritPoints();
            while (ipc.hasNext()) {
                Point2D.Double d = (Point2D.Double)ipc.next();
                pout.println("C_CD "+ScheduleSimulator.sdf.format(new Date((long)d.x))+" "+d.y);
            }

	    // Priority weighted demand profile
	    Iterator ipp = ds.listPriorityPoints();
            while (ipp.hasNext()) {
                Point2D.Double d = (Point2D.Double)ipp.next();
                pout.println("C_PD "+ScheduleSimulator.sdf.format(new Date((long)d.x))+" "+d.y);
            }

	} catch (Exception e) {
	    e.printStackTrace();
	}


	System.exit(0);

    }

}
