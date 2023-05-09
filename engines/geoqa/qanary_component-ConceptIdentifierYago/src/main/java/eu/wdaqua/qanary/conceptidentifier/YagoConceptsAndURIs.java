package eu.wdaqua.qanary.conceptidentifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class YagoConceptsAndURIs {

	static HashMap<String, ArrayList<String>> getYagoConceptsAndURIs() {
		
		
		BufferedReader reader;
		HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
		try {
			reader = new BufferedReader(new FileReader("qanary_component-ConceptIdentifierYago/src/main/resources/YagoClasses.txt"));
			
			while(true) {
				String classLabel = reader.readLine();
				if(classLabel == null) break;
				String numClassesStr = reader.readLine();
				ArrayList<String> L = new ArrayList<String>();
				map.put(classLabel, L);
				int numClasses = Integer.parseInt(numClassesStr);
				for(int i = 0; i < numClasses; i++) {
					L.add(reader.readLine());
				}
			
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return map;
		
		
	}
}
