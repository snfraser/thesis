import ngat.phase2.*;
import ngat.util.*;
import ngat.astrometry.*;
import ngat.util.logging.*;
import ngat.oss.simulation.*;
import ngat.oss.simulation.metrics.*;

import java.io.*;
import java.util.*;
import java.text.*;

public class YieldTest {


    public static void main(String args[]) {

	try {

            ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
	    
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
            //InstrumentRegistry instruments = (InstrumentRegistry)Naming.lookup("rmi://localhost/InstrumentRegistry");

            // Exec model
            File bxf = new File(config.getProperty("exec")); // exec model properties
            BasicExecutionTimingModel bxtm = new BasicExecutionTimingModel(site, null);
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
            //File bcaf = new File(config.getProperty("cost")); // charge model properties
            //BasicChargeAccountingModel bcam = new BasicChargeAccountingModel();
            //PropertiesConfigurator.use(bcaf).configure(bcam);

            // Selector
            //BasicSelectionHeuristic bsel = new BasicSelectionHeuristic();

            // Env prediction
            //File bepf = new File(config.getProperty("env")); // Environment properties
            //BasicEnvironmentPredictor bep = new BasicEnvironmentPredictor();
            //PropertiesConfigurator.use(bepf).configure(bep);
            BasicMutableEnvironmentPredictor bep = new BasicMutableEnvironmentPredictor();
            bep.setPhotom(true);
            bep.setSeeing(Group.EXCELLENT);

            BasicHistoryModel bhm = new BasicHistoryModel();
            //history.loadHistory(phase2, start);

            BasicAccountingModel bam = new  BasicAccountingModel();
            //bam.loadAccounts(phase2, start);

	    RepeatableGroup test = new RepeatableGroup("TEST");
	    test.setStartDate(start);
	    test.setEndDate(end);
	    test.setMaximumRepeats(500);
	    test.setMinimumInterval(4*3600*1000L);
	    test.setExpiryDate(end);

	    Observation obs = new Observation("ot");
	    obs.setExposeTime(60000.0f);
	    obs.setNumRuns(10);
	    ExtraSolarSource src = new ExtraSolarSource("src");
	    src.setRA(Math.random()*Math.PI*2.0);
	    src.setDec((Math.random()-0.5)*Math.PI*0.125+site.getLatitude());

	    obs.setSource(src);
	    Mosaic mosaic = new Mosaic("");
	    mosaic.setPattern(Mosaic.SINGLE);
	    obs.setMosaic(mosaic);

	    test.addObservation(obs);

	    EnvironmentSnapshot env = new EnvironmentSnapshot();
	    env.seeing = Group.EXCELLENT;
	    env.photom = true;

	    YieldTrackingUtilityCalculator ytuc   = new YieldTrackingUtilityCalculator(btcwc, bxtm, bhm, site);
	    
	    long time = start;
	    while (time < end) {
		
		ExecutionStatistics hist = bhm.getExecutionStatistics(test);

		double yield = ytuc.getUtility(test, time, env, hist);
		System.err.println(ScheduleSimulator.sdf.format(new Date(time))+"YIELD "+yield);
		if (bxtm.canDo(test, time, env, hist)) {
		    if (Math.random() > 0.995) {
			// ok lets do it.
			bhm.updateHistory(test, time);
			time += (long)bxtm.getExecTime(test);
			System.err.println(ScheduleSimulator.sdf.format(new Date(time))+" EXECUTED");
		    }
		}
		time += 15*60*1000L;
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
