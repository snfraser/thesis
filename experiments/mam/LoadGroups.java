import java.io.*;
import java.util.*;

public class LoadGroups {

    public LoadGroups() {}
    
    public Map load(File file) throws Exception {
	
	HashMap map = new HashMap();
	
	ObjectInputStream in = new ObjectInputStream(new FileInputStream(file)); 
	
	boolean loading = true;
	while (loading) {
	    try {
		Object obj = in.readObject();
		Agroup g = (Agroup)obj;
		if (g.rn == 0)
		    g.rn = 1;
		map.put(g.id, g);
	    } catch (EOFException ef) {
		loading = false;
		continue;
	    }
	   
	}

	return map;

    }
    
    public static void main(String args[]) {

	try {
	    
	    LoadGroups l = new LoadGroups();
	    Map map = l.load(new File(args[0]));

	    System.err.println("Got map: "+map);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
