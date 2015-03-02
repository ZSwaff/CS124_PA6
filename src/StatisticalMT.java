import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class StatisticalMT {
    private static final String filePrefix = "../es-en/";
    //for the repeat until convergence
    private static final int NUM_ITERATIONS = 15;
    
    private static HashSet<String> eVocab;
    private static HashSet<String> fVocab;
    private static HashMap<String, HashMap<String, Double>> tValues; 
    private static HashMap<String, HashMap<String, Double>> countFe;
    private static HashMap<String, Double> totalE;
    
    private static int vocabsize = 0;
    private static long totalUnigramCounts = 0;
    private static HashMap<String, Integer> unigramMap = new HashMap<String, Integer>();
    private static HashMap<String, Integer> bigramMap = new HashMap<String, Integer>();
    
    private static HashMap<String, String> filter;
    private static HashSet<String> commonEnglishWords;

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
        System.out.println("train");
        train(spanishTrain, englishTrain);
        
        String unigramDataFname = "../ngram_data/unigrams.txt";
        List<String> unigramCounts = loadList(unigramDataFname);
        initializeUnigramData(unigramCounts);
        
        String bigramDataFname = "../ngram_data/bigrams.txt";
        List<String> bigramCounts = loadList(bigramDataFname);
        initializeBigramData(bigramCounts);

        String englishTestFname = filePrefix + "test/newstest2013.en";
        String spanishTestFname = filePrefix + "test/newstest2013.es";
        
        List<String> englishTest = loadList(englishTestFname);
        List<String> spanishTest = loadList(spanishTestFname);
        for(int i = 0; i < englishTest.size(); i++){
            englishTest.set(i, englishTest.get(i).toLowerCase());
            spanishTest.set(i, spanishTest.get(i).toLowerCase());
        }
        initFilter(); 
        
        System.out.println("test");
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

                //Normalize
                for(int k = 0; k < es.length; k++){
                    feCounts.put(es[k], 0.0); 
                     fMatches.put(es[k], 1.0/(double)fVocab.size());
                }
                tValues.put(fWord, fMatches);
                countFe.put(fWord, feCounts);
            }
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
            
            bigramMap.put(word, count);
        }
    }

    private static void initFilter(){
        filter = new HashMap<String, String>();
        commonEnglishWords = new HashSet<String>();

        String filterFile = filePrefix + "filter.txt"; 
        List<String> filterData = loadList(filterFile); 
        for(String str : filterData){
            int delim = str.indexOf("#");
            String english = str.substring(0, delim).toLowerCase();
            String spanish = str.substring(delim + 1).toLowerCase();
            filter.put(spanish, english);
            commonEnglishWords.add(english);
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

    private static boolean isPunctuation(String str) {
        String punctuation = ".,:;!?";
        if(punctuation.contains(str))
            return true;
        return false;
    }
    
    public static void train(List<String> f, List<String> e) {
        
        for(int iter = 0; iter < NUM_ITERATIONS; iter++){ 
            System.out.println(iter); 
            //Copy this over to the tvalues at the end so we don't corrupt our probabilities
            for(String fStr : countFe.keySet()){
                HashMap<String, Double> feCounts = countFe.get(fStr);
                for(String eStr : feCounts.keySet()){
                    feCounts.put(eStr, 0.0); 
                    totalE.put(eStr, 0.0);
                }
                countFe.put(fStr, feCounts);
            }
            
            int docSize = f.size();
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
                        total += updateStrPair.get(es[i]);
                    }
                    
                    for(int i = 0; i < es.length; i++){
                        String currE = es[i]; 
                        double updateProb = (updateStrPair.get(currE) + tValues.get(currF).get(currE)/total);
                        updateStrPair.put(currE, updateProb);  
                    }
                    countFe.put(currF, updateStrPair); 
                }
            } 
             //M Step: Normalize the count probabilities, copy the new values into the t probabilities
           for(String fString : countFe.keySet()){
                HashMap<String, Double> eStrs = countFe.get(fString);
                for(String eString : eStrs.keySet()){
                    if(!totalE.containsKey(eString) || Double.isNaN(totalE.get(eString)))
                        totalE.put(eString, 0.0); 
                    double newTotalE = totalE.get(eString) + countFe.get(fString).get(eString);  
                    totalE.put(eString, newTotalE);   
                }
            }
            for(String fString : countFe.keySet()){
                HashMap<String, Double> newTVals = new HashMap<String, Double>();
                HashMap<String, Double> eStrs = countFe.get(fString);
                for(String eString : eStrs.keySet()){
                     double prevProb = eStrs.get(eString);  
                     double normal = totalE.get(eString);
                     newTVals.put(eString, prevProb/normal);
                }
             tValues.put(fString, newTVals);
            }   
        } 
    }
    
    private static void test(List<String> f, List<String> e) { 
        for(int i = 0; i < f.size(); i++){
            String fSent = f.get(i);
            //String targetESent = e.get(i);
            String resultESent = translate(fSent);
            
            //System.out.println("Goal translation: " + targetESent);
            System.out.println(/*"Our translation : " + */resultESent);
        }
    } 
 
    private static String translate(String fSent) {
        ArrayList<ArrayList<String>> translations = new ArrayList<ArrayList<String>>();
        translations.add(new ArrayList<String>());
        
        for(String str : fSent.split(" ")){
            if(tValues.containsKey(str)){
                HashMap<String, Double> currCandidates = tValues.get(str);
                double prob = 0.0;
                String currBest = ""; 
                if(isInteger(str))
                    currBest = str;
                else if(isPunctuation(str))
                    continue;
                else if(filter.containsKey(str))
                    currBest = filter.get(str);
                else{  
                    for(String candStr : currCandidates.keySet()){ 
                        if(!commonEnglishWords.contains(candStr) && unigramMap.containsKey(candStr)){ 
                            double potentialProb = (((double)unigramMap.get(candStr)) / ((double)totalUnigramCounts)) * currCandidates.get(candStr);
                            if(potentialProb > prob){
                                prob = potentialProb;
                                currBest = candStr; 
                            }
                        }
                    }
                }
                translations.get(0).add(currBest);
            }
            else translations.get(0).add(str); 
        } 
        
        ArrayList<String> bestAlignments = new ArrayList<String>();
        for(ArrayList<String> bag : translations){
            ArrayList<String> temp = new ArrayList<String>(bag);
            ArrayList<String> bestAlign = new ArrayList<String>();
            bestAlign.add("<S>");
            while(temp.size() > 0){
                String lastWord = bestAlign.get(bestAlign.size()-1);
                String bestNextWord = "";
                double bestBigramScore = Double.NEGATIVE_INFINITY;
                for(int i = 0 ; i < temp.size() && i < 2; i++){
                    String potNextWord = temp.get(i);
                    double currBigramScore = bigramScore(lastWord, potNextWord);
                    if(currBigramScore >= bestBigramScore){
                        bestBigramScore = currBigramScore;
                        bestNextWord = potNextWord;
                    }
                }
                temp.remove(bestNextWord);
                bestAlign.add(bestNextWord);
            }
            String resSent = "";
            for(int i = 1; i < bestAlign.size(); i++) resSent += " " + bestAlign.get(i);
            bestAlignments.add(resSent.trim());
        }
        
       // String resSent = "";
       // for(int i = 1; i < translations.get(0).size(); i++) resSent += " " + translations.get(0).get(i);
        return bestAlignments.get(0);
    } 
    
    
    private static double bigramScore(String w1, String w2){
        String bigram = w1 + " " + w2;
        double score = 0.0;
        
        if(bigramMap.containsKey(bigram)){
            score += Math.log(bigramMap.get(bigram) + 1);
            if(unigramMap.containsKey(w1)) {
                score -= Math.log(unigramMap.get(w1));
            } else score -= Math.log(vocabsize);
        } else {
            score += Math.log(1);
            if(unigramMap.containsKey(w1)) {
                score -= Math.log(unigramMap.get(w1));
            } else score -= Math.log(vocabsize);
        }
        return score;
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