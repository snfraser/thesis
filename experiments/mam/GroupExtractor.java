import java.io.*;
import java.util.*;

import ngat.astrometry.*;

public class GroupExtractor {

    public static void main(String args[]) {

	try {
	    
	    // open an output file
	    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(args[1]));
	    out.flush();

	    // open a file and read the lines
	    BufferedReader bin = new BufferedReader(new FileReader(args[0]));

	    String line = null;
	    while ((line = bin.readLine()) != null) {
		    
		    // display the line and request infos
		    System.err.println("New group for processing");
		    System.err.println(line);
		    
		    Agroup group = new Agroup();

		    // insert parsed info here...
		    StringTokenizer st = new StringTokenizer(line);
		  
		    String sid = st.nextToken();
		    group.id = sid;

		    String stra = st.nextToken();
		    double ra = Position.parseHMS(stra);
		    group.ra = ra;

		    String std = st.nextToken();
		    double dec = Position.parseDMS(std);
		    group.dec = dec;

		    String snam = st.nextToken();
		    group.name = snam;

		    String tdata = st.nextToken();
		    if (tdata.equalsIgnoreCase("M")) {
			String pdata = st.nextToken();
			double p = Double.parseDouble(pdata);
			group.timing = new Timing(Timing.INT, p);
		    } else if 
			(tdata.equalsIgnoreCase("I")) {
			String pdata = st.nextToken();
			double p = Double.parseDouble(pdata);
			group.timing = new Timing(Timing.INT, p);
		    } else if 
			(tdata.equalsIgnoreCase("F")) {
			st.nextToken();
			group.timing = new Timing(Timing.FLEX,0.0);
		    }	   
		    		    
		    // conditions
		    String cdata = st.nextToken();
		    char lc = cdata.charAt(0);
		    if (lc == 'b')
			group.lunar = Agroup.BRIGHT;
		    else
			group.lunar = Agroup.DARK;
	
		    char sc = cdata.charAt(1);
		    if (sc == 'p')
			group.seecat = Agroup.POOR;
		    else if
			(sc == 'a')
			group.seecat = Agroup.AVER;
		    else if
			(sc == 'x')
			group.seecat = Agroup.GOOD;
		    else if
			(sc == 'u')
			group.seecat = Agroup.USAB;

		    char nc =  cdata.charAt(2);
		    if (nc == 'n')
			group.tod = Agroup.NIGHT;
		    else
			group.tod = Agroup.TWI;

		    String pdata = st.nextToken();
		    group.priority = Integer.parseInt(pdata);
		    
		    // xt 
		    String xdata = st.nextToken();
		    group.xt = (long)(60000.0*Double.parseDouble(xdata));

		    // rn
		    String rdata = st.nextToken();
		    group.rn = Integer.parseInt(rdata);
		    
		    System.err.println("\n\nProcessed Group "+group+"\n\n");
		    out.writeObject(group);
		    out.flush();
		    
	    }

	    out.close();
	    System.err.println("CLosed output file");

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
