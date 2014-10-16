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

/** Runs a 2 month simulation.*/
public class LongTermSimulationTest implements TimeSignalGenerator, SimulationEventListener {

    private Site site;

    private ScheduleSimulator sim;

    private BasicHistoryModel bhm;

    private BasicChargeAccountingModel bcam;

    private BasicExecutionTimingModel bxtm;
    
    private EnvironmentPredictor ep;

    private BasicStochasticExecutionTimingModel bsem;

    private BasicTimingConstraintWindowCalculator btcwc;


    /** The simulation time - provided to scheduler/simulatotr via TimeModel.*/
    private long time;

    private LogProxy logger;

    // SQM
    int ngs = 0;
    int ncs = 0;

    double sumxt = 0.0;// actual used time
    double ccav; // average contention

    
    // Metric tallies.
    private double rnqm   = 0.0;
    private double ytqm   = 0.0;

    // Utility Calculators.
    private RemainingNightsUtilityCalculator rnuc;
    private YieldTrackingUtilityCalculator   ytuc;

    // Statistics.
    private PopulationStatistics xtstat; // [x]  total execution
    private PopulationStatistics rnstat; // [rn] urgency (1/rn)
    private PopulationStatistics ytstat; // [yt] urgency (yield)


 
    public LongTermSimulationTest(Site site,
			       ScheduleSimulator sim,
			       BasicExecutionTimingModel bxtm,
			       BasicStochasticExecutionTimingModel bsem,
			       EnvironmentPredictor ep,
			       BasicTimingConstraintWindowCalculator btcwc,
			       BasicHistoryModel bhm,
			       BasicChargeAccountingModel bcam,
			       long time) {	
	this.site = site;
	this.sim = sim;
	this.bxtm = bxtm;
	this.bsem = bsem;
	this.ep    = ep;
	this.btcwc = btcwc;
	this.bhm = bhm;
	this.bcam = bcam;
	this .time = time;

	Logger slogger = LogManager.getLogger("SIM");
	logger = new LogProxy("LST", "", slogger);

	// Setup utility measuring tools
	rnuc   = new RemainingNightsUtilityCalculator(btcwc);
	rnqm   = 0.0; 
	ytuc   = new YieldTrackingUtilityCalculator(btcwc, bxtm, bhm, site);
	ytqm   = 0.0;
	xtstat = new PopulationStatistics();
	rnstat = new PopulationStatistics();
	ytstat = new PopulationStatistics();
	
    }

  
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
	    File bsef = new File(config.getProperty("sexm")); // stochastic exec model properties
	    BasicStochasticExecutionTimingModel bsem = new BasicStochasticExecutionTimingModel(bxtm);
	    PropertiesConfigurator.use(bsef).configure(bsem);

	    // TC window calculator.
	    BasicTimingConstraintWindowCalculator btcwc = new BasicTimingConstraintWindowCalculator(bxtm, site, 5*60*1000L);

	    // Charging
	    File bcaf = new File(config.getProperty("cost")); // charge model properties
	    BasicChargeAccountingModel bcam = new BasicChargeAccountingModel();
	    PropertiesConfigurator.use(bcaf).configure(bcam);

	   

	    // Selector
            BasicSelectionHeuristic bsel = new BasicSelectionHeuristic();

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
	    //history.loadHistory(phase2, start);

	    BasicAccountingModel bam = new  BasicAccountingModel();
	    //bam.loadAccounts(phase2, start);


	    // Select a scoring model here - there will be others  
	    ScoringModel sm = null;
	    String scoring = config.getProperty("scoring");
	    if (scoring.equals("basic")) {
		// Scoring model
		File smf = new File(config.getProperty("score")); // bogstandard scoring properties
		BasicScoringModel bsm = new BasicScoringModel(site);
		PropertiesConfigurator.use(smf).configure(bsm);
		sm = bsm;
	    } else if
		(scoring.equals("alternative")) {
		// Scoring model
		File smf = new File(config.getProperty("score")); // 'alternative' scoring properties
		AlternativeScoringModel asm = new AlternativeScoringModel(btcwc,
									  bxtm,
									  history,
									  site);
		PropertiesConfigurator.use(smf).configure(asm);
		sm = asm;
	    }


	    // Rank sequencing...
	    //BasicRankSequencer brs = new BasicRankSequencer(phase2, history, bam, bxtm, sm);

	    // Despatcher
	    BasicDespatcher despatcher = new BasicDespatcher(phase2, history, bam, bxtm, sm, bsel);

	    // setup simulator...
	    ScheduleSimulator sim = new ScheduleSimulator(site, despatcher, bxtm, bep, bwm);
	   
	    // Time synch NOT NEEDED?	   
	  

	    // Setup test controller...
	    LongTermSimulationTest test = new LongTermSimulationTest(site, sim, bxtm, bsem, bep, btcwc, history, bcam, start);

	    slogger.log(1, "Start sim run...");
	    long t1 = System.currentTimeMillis();		
	    history.loadHistory(phase2, start);
	    long t2 = System.currentTimeMillis();
	    bam.loadAccounts(phase2, start);
	    long t3 = System.currentTimeMillis();

	    test.run(start, end);

	 
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}

    }

    public void run(long start, long end) throws Exception {

	int nday = (int)((end - start)/86400000);
	
	// Prepare stats...	  
	xtstat.clear();
	rnstat.clear();
	ytstat.clear();
	// Setup metric tallies...
	rnqm = 0.0; // record urgen metric this run.
	ytqm = 0.0; // record yield metric this run.
	sumxt = 0L; // record total exec this run.
	
	// run for many days		
	for (int in = 0; in < nday; in++) {
	    
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

	    // Start the simulator framework...	  
	    sim.runSimulation(this, ds, de, this);	

	    // update nightly samples	    
	    rnstat.addSample(rnqm);
	    xtstat.addSample((double)sumxt/(double)night);
	    ytstat.addSample(ytqm);

	    logger.cat("CDAT_XT").log(1, "CDAT_XT "+xtstat);
	    logger.cat("CDAT_RN").log(1, "CDAT_RN "+rnstat);
	    logger.cat("CDAT_YT").log(1, "CDAT_YT "+ytstat);
	}
  	
	logger.cat("CDAT_XT").log(1, "CDAT_XT "+xtstat);
	logger.cat("CDAT_RN").log(1, "CDAT_RN "+rnstat);
	logger.cat("CDAT_YT").log(1, "CDAT_YT "+ytstat);
    }

    public long getTime() { return time;}

    /** Handle time signal request.*/
    public void awaitTimingSignal(TimeSignalListener tsl, long t) {

	// can do some fudging of environment etc here if required
	time = t;
	tsl.timingSignal(t);
   
    }

    /** Handle notification of a simulation group selection event.
     * We need to step the time model forwards and notify sim of this time step.
     */
    public void groupSelected(Metric metric) {
	logger.method("groupSelected(Metric, t)");

	// Update the execution history for time using calculated exec time
	long exec = bsem.getExecTime(metric.group);

	ExecutionStatistics hist = bhm.getExecutionStatistics(metric.group);


	// Update the accounting using calculated charge (under current env !)
	//EnvironmentSnapshot env = ep.predictEnvironment(time+exec);
	EnvironmentSnapshot env = ep.predictEnvironment(time);

	// SQMs
	ngs++;
	sumxt += (double)exec;

	// Calculate RN value for this group and add to stats.
	double rnhm = rnuc.getUtility(metric.group, time, env, hist);
	rnqm += rnhm;

	double ythm = ytuc.getUtility(metric.group, time, env, hist);
	ytqm += ythm;

	logger.log(1,"Received notification of group selection: Time= "+
		   ScheduleSimulator.sdf.format(new Date(time))+
		   " Utility: [RN]: "+rnhm+	
		   " Yield: [YT]: "+ythm+
		   " GroupMetrics = "+metric+
		   " Updating history and accounting...");

	// START RNTEST
	if (Double.isInfinite(rnhm)) {
	    Group agroup = metric.group;
	    logger.log(1,"WARNING- SelectedGroup "+agroup.getFullPath()+
		       " type "+agroup.getClass().getName()+" has INFINITE RN value");
	    logger.log(1,"WARNING- Start enhanced logging...");
	    logger.setLogLevel(3);
	    rnuc.getUtility(metric.group, time, env, hist);
	    logger.log(1,"WARNING- Finish enhanced logging...");
	    logger.setLogLevel(1);
	}
	// END RNTEST

	double xhours = bcam.calculateCost(metric.group, env)/3600000.0;
	
	// Update execution history
	bhm.updateHistory(metric.group, time+exec); 
	logger.log(1,"Updating group history using sexec "+(exec/1000)+" s, completing at: "+
		   ScheduleSimulator.sdf.format(new Date(time+exec)));
	
	metric.accounts.getAccount(AccountingModel.ACCOUNT_TOTAL).debit(xhours);
	logger.log(1,"Updating proposal accounts with costing: "+xhours+" h");
	
	// time step forward.
	time += exec;
	sim.timingSignal(time);

    }
    
    /** Handle notification of contention stats.*/
    public void contentionResults(int contention) {
	logger.method("contentionResults(t,con)");
	logger.log(1,"CDAT_CC_T: "+ScheduleSimulator.sdf.format(new Date(time))+" "+contention);	
	ccav += (double)contention;
	ncs++;
    }
    
    /** Notification that simulation has completed.*/
    public void simulationCompleted() {

	logger.method("simCompleted()").log(1,"Received notification that simulation has completed");
	logger.log(1, "CDAT_NGS: "+ScheduleSimulator.sdf.format(new Date(time))+" "+ngs);
	logger.log(1, "CDAT_CC_AV: "+ScheduleSimulator.sdf.format(new Date(time))+" "+(ccav/(double)ncs));
	logger.log(1, "CDAT_XT: "+ScheduleSimulator.sdf.format(new Date(time))+" "+(sumxt/3600000.0)+" H");
	ngs = 0;
	ncs = 0;
	ccav = 0;
	sumxt = 0.0;
    } 

    
}
