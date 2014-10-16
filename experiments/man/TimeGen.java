import ngat.util.*;
import java.io.*;
import java.util.*;
import java.text.*;

public class TimeGen {
    
    public static void main(String args[]) {
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	try {
	    
	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
	    
	    // read points into array
	    
	    int nd = config.getIntValue("nd");
	    BufferedReader in = new BufferedReader(new FileReader(config.getProperty("base")));
	    
	    String outbase = config.getProperty("output");
	    
	    double x[] = new double[nd];
	    double y[] = new double[nd];
	    double s[] = new double[nd];
	    int ii = 0;
	    String line = null;
	    while ( (line = in.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(line);
		double xx = Double.parseDouble(st.nextToken());
		double yy = Double.parseDouble(st.nextToken());
		double ss = Double.parseDouble(st.nextToken());
		x[ii] = xx;
		y[ii] = yy;
		s[ii] = ss;
		ii++;
	    }

	    int np = config.getIntValue("np");
	    
	    long start = (sdf.parse(config.getProperty("start"))).getTime();
	    long step = config.getLongValue("step"); // ms
	    
	    double sy = config.getDoubleValue("sy");
	    
	    double h = config.getDoubleValue("h");
	    
	    double ceil = config.getDoubleValue("ceiling");
	    
	    GaussianRandom gy = new GaussianRandom(0.0, 0.5*sy);
	    
	    double av[] = new double[np];
	    for (int ia = 0; ia < np; ia++) {
		av[ia] = 0.0;
	    }
	    
	    int ng = config.getIntValue("ng");
	    
	    for (int ig = 0; ig < ng; ig++) {
		
		// open new file
		String outfile = outbase+"_"+ig+".dat";
		
		PrintStream pout = null;

		try {
		    
		    pout = new PrintStream(new FileOutputStream(outfile));
		    
		    
		    // compute nd data points
		    for (int i = 0; i < np; i++ ){
			
			double xi = x[0] + (double)i*(x[nd-1] - x[0])/(double)np;
			
			// which x_i are left and right of x
			
			for (int j = 0; j < nd-1; j++) {
			    
			    if ((x[j] < xi) && (x[j+1] >= xi)) {
				
				// here it is....
				double yi = y[j] + (xi - x[j])/(x[j+1] - x[j]) * (y[j+1] - y[j]) + gy.random();
				
				while (yi > ceil)
				    yi = yi - gy.random();
				
				av[i] += yi;
				
				long t = start + i * step;
				pout.println(""+i+" "+sdf.format(new Date(t))+" "+xi+" "+yi);
				
			    }
			    
			}//next j
		    } //next i
		} catch (Exception ee) {
		    
		} finally {
		    try {
			pout.close();
		    } catch (Exception px) {}
		}
		
	    } // next ig

	    PrintStream aout = null;
	    try {
		
		String outfile = outbase+"_avg.dat";
		
		aout = new PrintStream(new FileOutputStream(outfile));
		
		double average = 0.0;
		// compute nd data points
		for (int i = 0; i < np; i++ ){
		    
		    double xi = x[0] + (double)i*(x[nd-1] - x[0])/(double)np;
		    
		    double yi = av[i]/ng;
		    long t = start + i * step;
		    aout.println(""+i+" "+sdf.format(new Date(t))+" "+yi);
		    
		    average += yi;

		} // next i
		
		aout.println("#Time-Average: "+(average/(double)np)); 

	    } catch (Exception ee) {
		
	    } finally {
		try {
		    aout.close();
		} catch (Exception px) {}
	    }
	    
	} catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("Usage: TimeGen --nd <n in base> "+
			       "\n   --base <basefile> "+
			       "\n   --output <base>"+
			       "\n   --start <yyyy-mm-dd hh:mm> "+
			       "\n   --step <dt-ms>"+
			       "\n   --np <n results>"+
			       "\n   --sy <sdev> "+
			       "\n   --h <var> "+
			       "\n   --ceiling <c>");
	}
    }
    
}
