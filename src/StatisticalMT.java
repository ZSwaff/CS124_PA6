import java.io.*;
import java.util.*;

public class StatisticalMT {
	private static final String filePrefix = "../es-en/";
    //for the repeat until convergence
    private static final int NUM_ITERATIONS = 5;
	
	private static HashSet<String> eVocab;
    private static HashSet<String> fVocab;
    private static HashMap<String, HashMap<String, Double>> tValues; 
    private static HashMap<String, HashMap<String, Double>> countFe;
    private static HashMap<String, Double> totalE;
	
    private static int totalWords = 0;
    private static HashMap<String, Integer> unigramMap = new HashMap<String, Integer>();
    private static HashMap<String, HashMap<String, Integer>> bigramMap = new HashMap<String, HashMap<String, Integer>>();
	
    public static void main(String[] args) {
        String englishTrainFname = filePrefix + "mytest/mytest.en";
        String spanishTrainFname = filePrefix + "mytest/mytest.es";
        String englishTestFname = filePrefix + "mytest/mytest.en";
        String spanishTestFname = filePrefix + "mytest/mytest.es";
        
        List<String> englishTrain = loadList(englishTrainFname);
        List<String> spanishTrain = loadList(spanishTrainFname);
        init(spanishTrain, englishTrain);
        train(spanishTrain, englishTrain);
        
        List<String> englishTest = loadList(englishTestFname);
        List<String> spanishTest = loadList(spanishTestFname);
        //test(spanishTest, englishTest);
    }
    
    private static void init(List<String> f, List<String> e){
        eVocab = new HashSet<>();
        fVocab = new HashSet<>();
        tValues = new HashMap<String, HashMap<String, Double>>();
        countFe = new HashMap<String, HashMap<String, Double>>();
        totalE = new HashMap<String, Double>();
        //Build the vocabularies
        int docSize = f.size();
        for(int i = 0; i < docSize; i++){ 
            String[] fs = f.get(i).split(" ");
            String[] es = e.get(i).split(" ");
            //Add each word in sentence e to its vocab
            //Initalize totalE
            for(int j = 0; j < es.length; j++){  
                eVocab.add(es[j]);  
                totalE.put(es[j], 0.0); 
            }
            //Add each word in sentence f to its vocab
            for(int j = 0; j < fs.length; j++)
                fVocab.add(fs[j]); 
        }
        //Set t to uniform initial values
        for(int i = 0; i < docSize; i++){ 
            String[] fs = f.get(i).split(" ");
            String[] es = e.get(i).split(" ");

            for(int j = 0; j < fs.length; j++){ 
                String fWord = fs[j];

                HashMap<String, Double> feCounts;
                if(countFe.containsKey(fWord))
                    feCounts = countFe.get(fWord);
                else feCounts = new HashMap<>();
                //start t values
                HashMap<String, Double> fMatches;
                if(tValues.containsKey(fWord))
                    fMatches = tValues.get(fWord);
                else fMatches = new HashMap<>();

                double multProb = 1;  
                for(int k = 0; k < es.length; k++){
                    feCounts.put(es[k], 0.0); 
                    double tVal = (double)(1)/(double)eVocab.size();  
                    multProb *= tVal; 
                } 
                //Normalize
                for(int k = 0; k < es.length; k++){  
                    fMatches.put(es[k],multProb/(multProb*es.length));
                }
                tValues.put(fWord, fMatches);
                countFe.put(fWord, feCounts);
            }
        }  
    }
    
    
    public static void train(List<String> f, List<String> e) {
    	createBigramMap(e);
    	
        HashMap<String, Integer> fCounts = new HashMap<>();
        HashMap<String, Integer> eCounts = new HashMap<>(); 
        //Repeat until convergence 
        for(int i = 0; i < NUM_ITERATIONS; i++){
            //set count(f|e) to 0 for all f,e 
            for(String fStr : countFe.keySet()){
                HashMap<String, Double> feCounts = countFe.get(fStr);
                for(String eStr : feCounts.keySet()){ 
                    feCounts.put(eStr, 0.0);
                }
                countFe.put(fStr, feCounts);
            }
            
            //set total(e) to 0 for all e
            for(String str : totalE.keySet()){
                totalE.put(str, 0.0);
            }
            
            int docSize = f.size();
            for(int j = 0; j < docSize; j++){
                String fs = f.get(j);
                String es = e.get(j);  
                //find nf and ne
                fCounts.clear();
                for(String fWord : fs.split(" ")){
                    int fCount = 1;
                    if(fCounts.containsKey(fWord))
                        fCount += fCounts.get(fWord);
                    fCounts.put(fWord, fCount);
                }
                eCounts.clear();
                for(String eWord : es.split(" ")){
                    int eCount = 1;
                    if(eCounts.containsKey(eWord))
                        eCount += eCounts.get(eWord);
                    eCounts.put(eWord, eCount);
                } 
                
                //for all unique words f in f_s
                for(String fWord : fCounts.keySet()){
                    // n_f = count of f in f_s 
                    int nf = fCounts.get(fWord); 
                    //total_s = 0
                    double totalS = 0;
                    
                    //for all unique words e in e_s 
                    for(String eWord : eCounts.keySet()){
                        //total_s += t(f|e) * n_f  
                        totalS += tValues.get(fWord).get(eWord) * nf; 
                    }
                    
                    
                    //for all unique words e in e_s
                    for(String eWord : eCounts.keySet()){
                        //n_e = count of e in e_s
                        double ne = eCounts.get(eWord);
                        //count(f|e) += t(f|e) * n_f * n_e / total_s 
                        HashMap<String, Double> currCountFe = countFe.get(fWord);
                        double newCountFe = currCountFe.get(eWord); 
                        newCountFe += tValues.get(fWord).get(eWord) * nf * ne / totalS;  
                        currCountFe.put(eWord, newCountFe);
                        countFe.put(fWord, currCountFe); 
                        
                        //total(e) += t(f|e) * n_f * n_e / total_s
                        double newTotalE = totalE.get(eWord);
                        newTotalE += tValues.get(fWord).get(eWord) * nf * ne / totalS;
                        totalE.put(eWord, newTotalE);
                    }
                }
            }
            //for all e in domain( total(.) )
            for(String eWord : totalE.keySet()){ 
                //for all f in domain( count(.|e) ) 
                for(String fWord : countFe.keySet()){ 
                    //t(f|e) = count(f|e) / total(e)  
                    //if(countFe.get(fWord).containsKey(eWord)){
                    if(totalE.get(eWord) > 0.0){
                        HashMap<String, Double> currT = tValues.get(fWord);
                        if(currT.containsKey(eWord) && totalE.get(eWord) > 0){ 
                            currT.put(eWord, ((double)countFe.get(fWord).get(eWord))/((double) totalE.get(eWord)));
                            tValues.put(fWord, currT);
                        } 
                    }
                }
            }
        }
    }
    private static void createBigramMap(List<String> englishSentences){
    	for(String sent : englishSentences){
    		String lastWord = null;
    		for(String word : sent.split(" ")){
    			if(lastWord != null) {
	    			if(!bigramMap.containsKey(lastWord)){
	    				bigramMap.put(lastWord, new HashMap<String, Integer>());
	    			}
	    			if(!bigramMap.get(lastWord).containsKey(word)){
	    				bigramMap.get(lastWord).put(word, 1);
	    			}
	    			bigramMap.get(lastWord).put(word, bigramMap.get(lastWord).get(word) + 1);
	    			
	    			if(!unigramMap.containsKey(word)){
	    	    		unigramMap.put(word, 1);
	    			}
	    			unigramMap.put(word, unigramMap.get(word) + 1);
    			}
    			else{
    				if(!unigramMap.containsKey(word)){
    					unigramMap.put(word, 1);
    				}
    				unigramMap.put(word, unigramMap.get(word) + 1);
    			}
    			lastWord = word;
    			totalWords++;
    		}
    	}
    }
    
    
    private static void test(List<String> f, List<String> e) {
    	for(int i = 0; i < f.size(); i++){
    		String fSent = f.get(i);
    		String targetESent = f.get(i);
    		String resultESent = translate(fSent);
    		
    		//TODO: compare targetESent and resultESent
    	}
    }
    private static String translate(String fSent) {
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
    private static double calculateProbEnglishSent(String englishSent){ //aka P(E)
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
    private static double calculateProbFSentGivenESent(String fSent, String eSent){ //aka P(F|E)
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
        
    
    /*
     * Loads text files as lists of lines.
     */
    private static List<String> loadList(String fileName) {
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
