//package ngat.oss.simulation.test;

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

/** Test the Quantum LookaAhead Scheduler concept.*/
public class BasicDespatcherTest {

    private LogProxy logger;

    /** Setup test using supplied config.*/
    public static void main(String args[]) {

	try {

	    CommandTokenizer parser = new CommandTokenizer("--");
	    parser.parse(args);
	    ConfigurationProperties config = parser.getMap();

	    Logger slogger = LogManager.getLogger("SIM");
	    slogger.setLogLevel(config.getIntValue("log-level", 3));
	    ConsoleLogHandler console = new ConsoleLogHandler(new SimulationLogFormatter());
	    console.setLogLevel(config.getIntValue("log-level", 3));
	    slogger.addHandler(console);

	    LogProxy logger = new LogProxy("QST","",slogger);

	    Site site = new Site(config.getProperty("site"), 
				 Math.toRadians(config.getDoubleValue("lat")), 
				 Math.toRadians(config.getDoubleValue("long")));
	    
	    long start = (ScheduleSimulator.sdf.parse(config.getProperty("start"))).getTime();
            long end   = (ScheduleSimulator.sdf.parse(config.getProperty("end"))).getTime();

	    // Instruments.
	    InstrumentRegistry instruments = (InstrumentRegistry)Naming.lookup("rmi://localhost/InstrumentRegistry");
	  
	    // Exec model
	    File bxf = new File(config.getProperty("exec")); // exec model properties
            BasicExecutionTimingModel bxtm = new BasicExecutionTimingModel(site, instruments);
	    PropertiesConfigurator.use(bxf).configure(bxtm);

	    // Allow 2 day for this test...
            bxtm.setExternalTimeConstraint(end+30*24*3600*1000L, "End of simulation");

	    // Stochastic wrapper.
	    //File bsef = new File(config.getProperty("sexm")); // stochastic exec model properties
	    //BasicStochasticExecutionTimingModel bsem = new BasicStochasticExecutionTimingModel(bxtm);
	    //PropertiesConfigurator.use(bsef).configure(bsem);

	    // TC window calculator.
	    BasicTimingConstraintWindowCalculator btcwc = new BasicTimingConstraintWindowCalculator(bxtm, site, 5*60*1000L);

	    // Charging
	    File bcaf = new File(config.getProperty("cost")); // charge model properties
	    BasicChargeAccountingModel bcam = new BasicChargeAccountingModel();
	    PropertiesConfigurator.use(bcaf).configure(bcam);

	    // Selector
            //BasicSelectionHeuristic bsel = new BasicSelectionHeuristic();

	    // Env prediction
	    File bepf = new File(config.getProperty("env")); // Environment properties	    
	    BasicEnvironmentPredictor bep = new BasicEnvironmentPredictor();
	    PropertiesConfigurator.use(bepf).configure(bep);  
	    //BasicMutableEnvironmentPredictor bep = new BasicMutableEnvironmentPredictor();
	    //bep.setPhotom(true);
	    //bep.setSeeing(seeing);


	    // Weather prediction - this will be replaced with Weather scenario model
	    //File swpf = new File(config.getProperty("weather")); // weather properties
	    //StandardWeatherPredictor swp = new StandardWeatherPredictor();
	    //PropertiesConfigurator.use(swpf).configure(swp);
	    // and pre-compute weather for the full run
	    //swp.preCompute(start, end);
	    BasicMutableWeatherModel bwm = new BasicMutableWeatherModel();
	    bwm.setGood(true);

	    // RTC
	    // RemainingTimeCalculator rtc = new DummyRemainingTimeCalculator();

	    // ODB
	    String root = config.getProperty("root");
	 
	    Phase2ModelProvider provider = (Phase2ModelProvider)Naming.
		lookup("rmi://localhost/"+root+"_Phase2ModelProvider");
	    Phase2Model phase2 = provider.getPhase2Model();

	    // History
            BasicHistoryModel history = new BasicHistoryModel();
	    history.loadHistory(phase2, start);

	    BasicAccountingModel bam = new  BasicAccountingModel();
	    bam.loadAccounts(phase2, start);


	    // Select a scoring model here - there will be others  
	    // Scoring model
	    File smf = new File(config.getProperty("score")); // bogstandard scoring properties
	    BasicScoringModel bsm = new BasicScoringModel(site);
	    PropertiesConfigurator.use(smf).configure(bsm);
	    
	    BasicCandidateGenerator bcg = new BasicCandidateGenerator(phase2,
								      history,
								      bam,
								      bxtm,
								      bsm);


	    // Alternative using despath scheduler
	    BasicSelectionHeuristic selector = new BasicSelectionHeuristic();
 	    BasicDespatcher bds = new  BasicDespatcher(bcg,
						       selector);


	    slogger.log(1, "Start sim run...");

	    // fixed env for now
	    EnvironmentSnapshot env = new EnvironmentSnapshot();
	    env.seeing = Group.EXCELLENT;
	    env.photom = true;

	    // setup some stats.
	    PriorityUtilityCalculator        puc;
	    OptimalAirmassUtilityCalculator  oauc;
	    RemainingNightsUtilityCalculator rnuc;
	    ScoringUtilityCalculator         suc;

	    oauc = new OptimalAirmassUtilityCalculator(site, btcwc, bxtm, 5*60*1000L);
	    puc  = new PriorityUtilityCalculator(bxtm);
	    rnuc = new RemainingNightsUtilityCalculator(btcwc);
	    suc  = new ScoringUtilityCalculator(bxtm, bsm, bam);
 
	    double pqm =0.0;
	    double oaqm = 0.0;
	    double rnqm = 0.0;
	    double xtqm = 0.0;
	    double ngqm = 0.0;
	    double sqm  = 0.0;

	    int it = 0;
	    //while (t < end) {

	    int ndays = (int)((end - start)/86400000);
	
	    // run for many days		
	    for (int in = 0; in < ndays; in++) {
		
		long ds = start + in*86400*1000L;
		long de = ds + 86400*1000L;
		
		// work out the night length
		long night = 0L;
		long astro = 0L;
		long t = ds;
		while (t < de) {
		    Position sun = Astrometry.getSolarPosition(t);
		    double sunElev = sun.getAltitude(t, site);
		    if (sunElev < 0.0)
			night += 5*60*1000L;
		    if (sunElev < Math.toRadians(-18.0))
			astro += 5*60*1000L;
		    t += 5*60*1000L;
		}
		
		// DAILY totals
		int    ngs = 0;
		double phm  = 0.0;
		double oahm = 0.0;
		double rnhm = 0.0;
		double xtm  = 0.0;
		double shm  = 0.0;

		// a days worth
		t = ds;
		while (t < de) {
		    
		    Position sun = Astrometry.getSolarPosition(t);
		    if (sun.getAltitude(t, site) > Math.toRadians(-1.0)) {
			t += 5*60*1000L;
			logger.log(1, "SUNUP at "+ScheduleSimulator.sdf.format(new Date(t)));
			continue;
		    }
		    
		    // Using BDS
		    Metric metric = bds.getScheduleItem(t, env);
		    
		    if (metric == null) {
			t += 5*60*1000L;
			logger.log(1, "SELECT "+it+" at "+ScheduleSimulator.sdf.format(new Date(t))+" NONE");
		    } else {
			
			Group group = metric.group;
			ExecutionStatistics hist = history.getExecutionStatistics(group);
			long xt = bxtm.getExecTime(group);
			
			// SQM updates
			ngs++;
			phm  += puc.getUtility(group, t, env, hist);
			oahm += oauc.getUtility(group, t, env, hist);
			rnhm += rnuc.getUtility(group, t, env, hist);		
			xtm  += (double)xt;
			shm  += suc.getUtility(group, t, env, hist);

			logger.log(1, "SELECT "+it+" at "+ScheduleSimulator.sdf.format(new Date(t))+" "+group.getName()+
				   " until "+ScheduleSimulator.sdf.format(new Date(t+xt)));
			t += xt;
			history.updateHistory(group, t-10000L);
			
		    }
		    it++;
		}

		// daily averages
		logger.log(1, "CDAT_NG "+ScheduleSimulator.sdf.format(new Date(t))+" "+ngs);
		logger.log(1, "CDAT_PX "+ScheduleSimulator.sdf.format(new Date(t))+" "+(phm/night));
		logger.log(1, "CDAT_OA "+ScheduleSimulator.sdf.format(new Date(t))+" "+(oahm/(double)ngs));
		logger.log(1, "CDAT_RN "+ScheduleSimulator.sdf.format(new Date(t))+" "+(rnhm/(double)ngs));
		logger.log(1, "CDAT_XT "+ScheduleSimulator.sdf.format(new Date(t))+" "+((double)xtm/night));// hours
		logger.log(1, "CDAT_SU "+ScheduleSimulator.sdf.format(new Date(t))+" "+shm);
		
		ngqm += ngs;
		pqm += phm/night;
		oaqm += oahm/(double)ngs;
		rnqm += rnhm/(double)ngs;
		xtqm += xtm/3600000.0;
		sqm  += shm;

	    } // next day
	    
	    // averages for SQM
	    logger.log(1, "CDAT_SQ_NG "+(ngqm/(double)ndays));
	    logger.log(1, "CDAT_SQ_PX "+(pqm/(double)ndays));
	    logger.log(1, "CDAT_SQ_OA "+(oaqm/(double)ndays));
	    logger.log(1, "CDAT_SQ_RN "+(rnqm/(double)ndays));
	    logger.log(1, "CDAT_SQ_XT "+(xtqm/(double)ndays));// hours
	    logger.log(1, "CDAT_SQ_SU "+(sqm/(double)ndays));
	    
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
	
    }
    
}
