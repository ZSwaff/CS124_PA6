import java.io.*;
import java.util.*;

public class StatisticalMT {
	static String filePrefix = "../es-en/";
	
    public static void main(String[] args) {
        String englishTrainFname = filePrefix + "train/europarl-v7.es-en.en";
        String spanishTrainFname = filePrefix + "train/europarl-v7.es-en.es";
        
        List<String> englishTrain = loadList(englishTrainFname);
        List<String> spanishTrain = loadList(spanishTrainFname);
        
        train();
        
        test();
    }
    
    public static void train() {
    	
    }
    
    public static void test() {
    	
    }
    
    /*
     * Loads text files as lists of lines.
     */
    public static List<String> loadList(String fileName) {
        List<String> list = new ArrayList<String>();
        try {
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            for(String line = input.readLine(); line != null; line = input.readLine()) {
                list.add(line.trim());
            }
            input.close();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
        return list;
    }
}
