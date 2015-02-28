import java.io.*;
import java.lang.reflect.Array;
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
	
    private static int vocabsize = 0;
    private static int totalUnigramCounts = 0;
    private static HashMap<String, Integer> unigramMap = new HashMap<String, Integer>();
    private static int totalBigramCounts = 0;
    private static HashMap<String, Integer> bigramMap = new HashMap<String, Integer>();
    private static int totalTrigramCounts = 0;
    private static HashMap<String, Integer> trigramMap = new HashMap<String, Integer>();
	
    public static void main(String[] args) {
        String englishTrainFname = filePrefix + "train/europarl-v7.es-en.en";
        String spanishTrainFname = filePrefix + "train/europarl-v7.es-en.es";
        
        List<String> englishTrain = loadList(englishTrainFname);
        List<String> spanishTrain = loadList(spanishTrainFname);
        for(int i = 0; i < englishTrain.size(); i++){
        	englishTrain.set(i, englishTrain.get(i).toLowerCase());
        	spanishTrain.set(i, spanishTrain.get(i).toLowerCase());
        }
        init(spanishTrain, englishTrain);
        train(spanishTrain, englishTrain);
        
        
        String unigramDataFname = "../ngram_data/unigrams.txt";
        List<String> unigramCounts = loadList(unigramDataFname);
        initializeUnigramData(unigramCounts);
        
        String bigramDataFname = "../ngram_data/bigrams.txt";
        List<String> bigramCounts = loadList(bigramDataFname);
        initializeBigramData(bigramCounts);
        

        String englishTestFname = filePrefix + "mytest/mytest.en";
        String spanishTestFname = filePrefix + "mytest/mytest.es";
        
        List<String> englishTest = loadList(englishTestFname);
        List<String> spanishTest = loadList(spanishTestFname);
        for(int i = 0; i < englishTest.size(); i++){
        	englishTest.set(i, englishTest.get(i).toLowerCase());
        	spanishTest.set(i, spanishTest.get(i).toLowerCase());
        }
        initializeTrigramData(englishTest);
        test(spanishTest, englishTest);
    }
    
    private static void init(List<String> f, List<String> e){
        eVocab = new HashSet<String>();
        fVocab = new HashSet<String>();
        tValues = new HashMap<String, HashMap<String, Double>>();
        countFe = new HashMap<String, HashMap<String, Double>>();
        totalE = new HashMap<String, Double>();

        //Add "NULL" to the beginning of each f sentence
        int docSize = f.size();
        for(int i =0; i < docSize; i++){
            String newSent = "NULL " + f.get(i);
            f.set(i, newSent);
        }
        
        //Build the vocabularies
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
                else feCounts = new HashMap<String, Double>();
                //start t values
                HashMap<String, Double> fMatches;
                if(tValues.containsKey(fWord))
                    fMatches = tValues.get(fWord);
                else fMatches = new HashMap<String, Double>();

                double multProb = 1;  
                for(int k = 0; k < es.length; k++){
              //      feCounts.put(es[k], 0.0);
                    multProb *= 1.0/(double)eVocab.size();
                }
                //Normalize
                for(int k = 0; k < es.length; k++){
                    feCounts.put(es[k], 0.0);
                    //fMatches.put(es[k],multProb/(multProb*es.length));
                    fMatches.put(es[k], multProb);
                }
                tValues.put(fWord, fMatches);
                countFe.put(fWord, feCounts);
            }
        }
        //Normalize (M Step)
        for(String str : tValues.keySet()){
            HashMap<String, Double> eStrs = tValues.get(str);
            double sum = 0;
            for(String eStr : eStrs.keySet()){
                sum += eStrs.get(eStr);
            }
            for(String eStr : eStrs.keySet()){
                double prevProb = eStrs.get(eStr);
                eStrs.put(eStr, prevProb/sum);
            }
            tValues.put(str, eStrs);
        }
    }
    private static void initializeUnigramData(List<String> unigramCounts){
    	for(String str : unigramCounts){
    		String[] parts = str.split("\t");
    		String word = parts[0];
    		int count = Integer.MAX_VALUE;
            if (isInteger(parts[1])) {
                count = Integer.parseInt(parts[1]);
            }
    		
            vocabsize++;
    		totalUnigramCounts += count;
    		unigramMap.put(word, count);
    	}
    }
    private static void initializeBigramData(List<String> bigramCounts){
    	for(String str : bigramCounts){
    		String[] parts = str.split("\t");
    		String word = parts[0];
            int count = Integer.MAX_VALUE;
            if (isInteger(parts[1])) {
                count = Integer.parseInt(parts[1]);
            }
            
            totalBigramCounts += count;
    		bigramMap.put(word, count);
    	}
    }
    private static void initializeTrigramData(List<String> englishSentences) {
        int numSentences = englishSentences.size();
        for (int i = 0; i < numSentences; i++) {
            String sentence = englishSentences.get(i);
            String[] words = sentence.split(" ");
            if (words.length > 2) {
                for (int j = 2; j < words.length; j++) {
                    String trigram = words[j-2] + " " + words[j - 1] + " " + words[j];
                    if (trigramMap.containsKey(trigram)) {
                        trigramMap.put(trigram, trigramMap.get(trigram) + 1);
                    } else {
                        trigramMap.put(trigram, 1);
                    }
                }
            }
        }
    }
    
    private static boolean isInteger(String string) {
        try {
            Integer.valueOf(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static void train(List<String> f, List<String> e) {
        
        for(int iter = 0; iter < NUM_ITERATIONS; iter++){
            System.out.println(iter);
            //Copy this over to the tvalues at the end so we don't corrupt our probabilities
            for(String fStr : countFe.keySet()){
                HashMap<String, Double> feCounts = countFe.get(fStr);
                for(String eStr : feCounts.keySet()){
                    feCounts.put(eStr, 0.0);
                }
                countFe.put(fStr, feCounts);
            }
            
            int docSize = f.size();
            /*for(int line = 0; line < docSize; line++){
             String[] fs = f.get(line).split(" ");
             String[] es = e.get(line).split(" ");
             double epsilon = 50.0; //decide later on value
             double l = es.length;
             double m = fs.length;
             //create inner sum values
             HashMap<String, Double> innerSums = new HashMap<>();
             for(int jprime = 0; jprime < m; jprime ++){
             double innerSum = 0.0;
             for(int iprime = 0; iprime < l; iprime ++){
             innerSum += tValues.get(fs[jprime]).get(es[iprime]);
             }
             innerSums.put(fs[jprime], innerSum);
             }
             //System.out.println(innerSums);
             
             //for every sentence pair
             for(int j = 0; j < m; j++){
             double alignmentProb = epsilon/Math.pow((l + 1), m);
             HashMap<String, Double> eStrs = countFe.get(fs[j]);
             
             for(int i = 0; i < l; i++){
             //System.out.println(tValues.get(fs[j]).get(es[i]));
             alignmentProb *= tValues.get(fs[j]).get(es[i]);
             //Calculate the other alignments
             for(int jprime = 0; jprime < m; jprime ++){
             double innerSum = 0.0;
             if(jprime != j){
             if(innerSums.get(fs[jprime]) > 0)
             alignmentProb *= (innerSums.get(fs[jprime]));
             }
             }
             eStrs.put(es[i], alignmentProb);
             }
             countFe.put(fs[j], eStrs);
             }
             }
             System.out.println(countFe);
             //M Step: Normalize the count probabilities, copy the new values into the t probabilities
             for(String str : countFe.keySet()){
             HashMap<String, Double> newTVals = new HashMap<>();
             HashMap<String, Double> eStrs = countFe.get(str);
             double sum = 0;
             for(String eStr : eStrs.keySet()){
             sum += eStrs.get(eStr);
             }
             for(String eStr : eStrs.keySet()){
             double prevProb = eStrs.get(eStr);
             if(prevProb != 0)
             newTVals.put(eStr, prevProb/sum);
             else newTVals.put(eStr, 0.0);
             }
             tValues.put(str, newTVals);
             }
             */
            //Iterate through every sentence pair
            for(int line = 0; line < docSize; line++){
                String[] fs = f.get(line).split(" ");
                String[] es = e.get(line).split(" ");
                
                //E Step: Calculate count probabilities for each alignment in the sentence pairs
                for(int j = 0; j < fs.length; j++){
                    String currF = fs[j];
                    double total  = 0.0;
                    HashMap<String, Double> updateStrPair = tValues.get(currF);
                    for(int i = 0; i < es.length; i++){
                        //if(updateStrPair.containsKey(es[i]))
                        total += updateStrPair.get(es[i]);
                    }
                    
                    for(int i = 0; i < es.length; i++){
                        String currE = es[i];
                        // if(updateStrPair.containsKey(es[i])){
                        double updateProb = (updateStrPair.get(currE) + tValues.get(currF).get(currE)/total);
                        updateStrPair.put(currE, updateProb);
                        //}
                    }
                    countFe.put(currF, updateStrPair);
                }
            }
            //M Step: Normalize the count probabilities, copy the new values into the t probabilities
            for(String str : countFe.keySet()){
                HashMap<String, Double> newTVals = new HashMap<String, Double>();
                HashMap<String, Double> eStrs = countFe.get(str);
                double sum = 0;
                for(String eStr : eStrs.keySet()){
                    sum += eStrs.get(eStr);
                }
                for(String eStr : eStrs.keySet()){
                    double prevProb = eStrs.get(eStr);
                    newTVals.put(eStr, prevProb/sum);
                }
                tValues.put(str, newTVals);
            }
            /*
             //Iterate through every sentence pair
             for(int line = 0; line < docSize; line++){
             String[] fs = f.get(line).split(" ");
             String[] es = e.get(line).split(" ");
             
             //E Step: Calculate count probabilities for each alignment in the sentence pairs
             for(int j = 0; j < fs.length; j++){
             String currF = fs[j];
             HashMap<String, Double> updateStrPair = countFe.get(currF);
             for(int i = 0; i < es.length; i++){
             String currE = es[i];
             double updateProb = (updateStrPair.get(currE) + tValues.get(currF).get(currE));
             updateStrPair.put(currE, updateProb);
             }
             countFe.put(currF, updateStrPair);
             }
             }
             //M Step: Normalize the count probabilities, copy the new values into the t probabilities
             for(String str : countFe.keySet()){
             HashMap<String, Double> newTVals = new HashMap<>();
             HashMap<String, Double> eStrs = countFe.get(str);
             double sum = 0;
             for(String eStr : eStrs.keySet()){
             sum += eStrs.get(eStr);
             }
             for(String eStr : eStrs.keySet()){
             double prevProb = eStrs.get(eStr);
             newTVals.put(eStr, prevProb/sum);
             }
             tValues.put(str, newTVals);
             }*/
        }
        
        //checking values of casa
        
//        HashMap<String, Double> casaCandidates = tValues.get("casa");
//        double house = casaCandidates.get("house");
//        double as = casaCandidates.get("as");
//        System.out.println("house probability for casa: " + house);
//        System.out.println("as probability for casa: " + as);
    }
    
    private static void test(List<String> f, List<String> e) {
    	for(int i = 0; i < f.size(); i++){
    		String fSent = f.get(i);
    		String targetESent = e.get(i);
    		String resultESent = translate(fSent);
    		
    		//TODO: compare targetESent and resultESent
    		System.out.println("Goal translation: " + targetESent);
    		System.out.println("Our translation : " + resultESent);
    	}
    }
    
    private static String translate(String fSent) {
    	int lengthSent = fSent.length(); //aka j
    	//TODO try other lengths
    	
    	ArrayList<ArrayList<String>> translations = new ArrayList<ArrayList<String>>();
        translations.add(new ArrayList<String>());
        
        for(String str : fSent.split(" ")){
            if(tValues.containsKey(str)){
                HashMap<String, Double> currCandidates = tValues.get(str);
                double prob = -1.0;
                String currBest = "";
                
                for(String candStr : currCandidates.keySet()){
                	if(unigramMap.containsKey(candStr)){
	                	double potentialProb = (double)unigramMap.get(candStr) * currCandidates.get(candStr);
	                    if(potentialProb > prob){
	                        prob = potentialProb;
	                        currBest = candStr;
	                    }
                	}
                }
                translations.get(0).add(currBest);
            }
        }
        
//        System.out.println("fsent: " + fSent);
//        System.out.println("bag of words: ");
//        ArrayList<String> bagOfWords = translations.get(0);
//        for (int i = 0; i < bagOfWords.size(); i++) {
//            System.out.println(bagOfWords.get(i));
//        }
        
        ArrayList<String> bestAlignments = new ArrayList<String>();
        for(ArrayList<String> bag : translations){
        	ArrayList<String> temp = new ArrayList<String>(bag);
        	ArrayList<String> bestAlign = new ArrayList<String>();
        	bestAlign.add("<S>");
            
            //can't use trigrams if there are only 2 words
            if (temp.size() == 2) {
                String word1 = temp.get(0);
                String word2 = temp.get(1);
                
                double word1word2BigramScore = bigramScore(word1, word2);
                double word2word1BigramScore = bigramScore(word2, word1);
                
                if (word1word2BigramScore > word2word1BigramScore) {
                    bestAlign.add(word1);
                    bestAlign.add(word2);
                } else {
                    bestAlign.add(word2);
                    bestAlign.add(word1);
                }
                temp.remove(word1);
                temp.remove(word2);
            }
            
            while(temp.size() > 0){
                String lastWord = bestAlign.get(bestAlign.size()-1);
                String bestNextWord = "";
                String bestNextNextWord = "";
                int bestCount = -1;
                double bestScore = 0.0;
                for(String potNextWord : temp){
                    for (String potNextNextWord : temp){
                        if (potNextNextWord.equals(potNextWord)) continue;
                        String potTrigram = lastWord + " " + potNextWord + " " + potNextNextWord;
                        String potBigram = lastWord + " " + potNextWord;
                        double tempScore = trigramScore(potTrigram, potBigram);

                        if(tempScore >= bestScore){
                            bestScore = tempScore;
                            bestNextWord = potNextWord;
                            bestNextNextWord = potNextNextWord;
                        }
                    }
                }
                temp.remove(bestNextWord);
                temp.remove(bestNextNextWord);
                bestAlign.add(bestNextWord);
                bestAlign.add(bestNextNextWord);
            }
            
            String resSent = "";
        	for(int i = 1; i < bestAlign.size(); i++) resSent += " " + bestAlign.get(i);
        	bestAlignments.add(resSent.trim());
        }
        
        return bestAlignments.get(0);
    }
    
    
    private static double trigramScore(String trigram, String bigram){
        double score = 0.0;
        if(trigramMap.containsKey(trigram)){
            score += Math.log(trigramMap.get(trigram) + 1);
            score -= Math.log(bigramMap.get(bigram) + vocabsize);
        } else {
            score += Math.log(1);
            if(bigramMap.containsKey(bigram)) {
                score -= Math.log(bigramMap.get(bigram) + vocabsize);
            } else score -= Math.log(vocabsize);
        }
        return 0.50 * score;
    }
    
    private static double bigramScore(String w1, String w2){
        String bigram = w1 + " " + w2;
        double score = 0.0;
        
        if(bigramMap.containsKey(bigram)){
            score += Math.log(bigramMap.get(bigram) + 1);
            score -= Math.log(unigramMap.get(w1) + vocabsize);
        } else {
            score += Math.log(1);
            if(unigramMap.containsKey(w1)) {
                score -= Math.log(unigramMap.get(w1) + vocabsize);
            } else score -= Math.log(vocabsize);
        }
        return 0.35 * score;
    }
    
    /*private static String translate(String fSent) {
    	int lengthSent = fSent.length(); //aka j
    	
    	//go through each each word, choose the most probable english equivalent, from t probabilities
        ArrayList<String> translations = new ArrayList<String>();
        translations.add("");
        for(String str : fSent.split(" ")){
            if(tValues.containsKey(str)){
                HashMap<String, Double> currCandidates = tValues.get(str);
                double prob = 0.0;
                String currBest = "";
                
                for(String candStr : currCandidates.keySet()){
                    if(currCandidates.get(candStr) > prob){
                        prob = currCandidates.get(candStr);
                        currBest = candStr;
                    }
                }
                translations.set(0, translations.get(0) + " " +currBest);
            }
        }
        
    	return calculateBestAlignement(fSent, lengthSent, translations.get(0));
    }*/
    private static int[] calculateBestAlignement(String fSent, int numFWords, String eSent){
        int numEWords = eSent.split(" ").length;
    	int[] alignment = new int[numEWords];

        List<String> eWords = Arrays.asList(eSent.split(" "));
        List<String> fWords = Arrays.asList(fSent.split(" "));
        
        for(int i = 0; i < numEWords; i++) {
          String eWord = eWords.get(i);
          int maxIndex = -1;
          double maxProb = 0.0;
          for(int j = 0; j < numFWords; j++) {
            String fWord = fWords.get(j);
            double prob = tValues.get(fWord).get(eWord);
            if(prob >= maxProb) {
              maxProb = prob;
              maxIndex = j;
            }  
          }
          alignment[i] = maxIndex;
        }
        
        return alignment;
    }
    
    /*private static double calculateProbEnglishSent(String englishSent){ //aka P(E)
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
    }*/
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
