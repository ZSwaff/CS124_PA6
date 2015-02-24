import java.io.*;
import java.util.*;

public class StatisticalMT {
	static String filePrefix = "../es-en/";
	
	static int totalWords = 0;
	static HashMap<String, Integer> unigramMap = new HashMap<String, Integer>();
	static HashMap<String, HashMap<String, Integer>> bigramMap = new HashMap<String, HashMap<String, Integer>>();
	
	//						span			eng
	private static HashMap<String, HashMap<String, Double>> tValues;
	
    public static void main(String[] args) {
        String englishTrainFname = filePrefix + "train/europarl-v7.es-en.en";
        String spanishTrainFname = filePrefix + "train/europarl-v7.es-en.es";
        
        List<String> englishTrain = loadList(englishTrainFname);
        List<String> spanishTrain = loadList(spanishTrainFname);
        
        train();
    }
    
    public static void train() {
    	
    }
    
    public static void createBigramMap(List<String> englishSentences){
    	for(String sent : englishSentences){
    		String lastWord = null;
    		for(String word : sent.split(" ")){
    			if(lastWord != null) {
	    			if(!unigramMap.containsKey(lastWord)){
	    	    		unigramMap.put(lastWord, 1);
	    				bigramMap.put(lastWord, new HashMap<String, Integer>());
	    			}
	    			if(!bigramMap.get(lastWord).containsKey(word)){
	    				bigramMap.get(lastWord).put(word, 1);
	    			}
	    			unigramMap.put(word, unigramMap.get(word) + 1);
	    			bigramMap.get(lastWord).put(word, bigramMap.get(lastWord).get(word) + 1);
    			}
    			else{
    				if(!unigramMap.containsKey(lastWord)){
    					unigramMap.put(lastWord, 0);
    				}
    				unigramMap.put(word, unigramMap.get(word) + 1);
    			}
    			lastWord = word;
    			totalWords++;
    		}
    	}
    }
    
    public static double calculateProbEnglishSent(String englishSent){ //aka P(E)
		String lastWord = null;
		double prob = 1;
		for(String word : englishSent.split(" ")){
			if(lastWord != null){
				if(!unigramMap.containsKey(lastWord)){
					prob *= (1 / totalWords);
				}
				else if(!bigramMap.get(lastWord).containsKey(word)){
					prob *= (1 / unigramMap.get(lastWord));
				}
				else {
					prob *= (bigramMap.get(lastWord).get(word) / unigramMap.get(lastWord));
				}
			}
			lastWord = word;
		}
    	return prob;
    }
    public static double calculateProbFSentGivenESent(String fSent, String eSent){ //aka P(F|E)
    	//TODO: pick epsilon
    	double epsilon = 1;
    	
    	if(!eSent.startsWith("NULL"))
    		eSent = "NULL " + eSent;
    	
    	double l = eSent.length();
    	double m = fSent.length();
		double prob = epsilon / Math.pow(l+1, m);
		
		for(String fWord : fSent.split(" ")){
			double tempSum = 0;
			for(String eWord : eSent.split(" ")){
				tempSum += tValues.get(fWord).get(eWord);
			}
			prob *= tempSum;
		}
    	return prob;
    }
    
    public static String translate(String fSent) {
    	HashMap<String, Double> candidateScores = new HashMap<String, Double>();
    	
    	int lengthSent = fSent.length(); //aka j
    	
    	//TODO: generate list of candidates, put in hashmap with value 1
    	
    	
    	//calculate P(F|E) for each
    	for(String candSent : candidateScores.keySet()){
    		candidateScores.put(candSent, candidateScores.get(candSent) * calculateProbFSentGivenESent(fSent, candSent));
    	}
    	
    	//calculate P(E) for each
    	for(String candSent : candidateScores.keySet()){
    		candidateScores.put(candSent, candidateScores.get(candSent) * calculateProbEnglishSent(candSent));
    	}
    	
    	//return the best candidate
    	String bestCand = null;
    	double bestScore = 0;
    	for(String candSent : candidateScores.keySet()){
    		if(candidateScores.get(candSent) > bestScore){
    			bestScore = candidateScores.get(candSent);
    			bestCand = candSent;
    		}
    	}
    	return bestCand;
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
