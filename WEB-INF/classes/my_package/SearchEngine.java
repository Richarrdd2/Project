package my_package;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

import java.util.*;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Math;

public class SearchEngine
{
	static public RecordManager recman; 
	private static HTree titleForwardIndex;
	private static HTree invertedIndex;
	private static HTree URLtoPageID;
	private static HTree pageIDtoURL;
	private static HTree wordtoWordID;
	private static HTree wordIDtoWord;
	private static HTree phrasetoPhraseID;
	private static HTree phraseIDtoPhrase;
	private static HTree forwardIndex;
	private static HTree parentToChild;
	private static HTree childToParent;
	private static HTree pageProperties;
	private static HTree max_tfs;
	private static HTree phraseForwardIndex;

	private HashMap<Integer, Double> tfs;
	private HashMap<Integer, Double> dfs;
	private HashMap<Integer, List<Double>> tfidfs;
	private HashMap<Integer, List<Double>> tfidfs_phrase;
	private HashMap<Integer, List<Double>> tfidfsAll;
	private HashMap<Integer, Double> scores;

	/** 
	 * Search Engine constructor, initiate the databse fetching
	 * 
	 * 
	 * @throws Exception 
	 */
	public SearchEngine() throws Exception
	{
		String dbPath = System.getenv("CATALINA_HOME") + "/bin/assets/project";
		recman = RecordManagerFactory.createRecordManager(dbPath);
		long recid = recman.getNamedObject("invertedIndex");
		invertedIndex = HTree.load(recman, recid);
		recid = recman.getNamedObject("titleForwardIndex");
		titleForwardIndex = HTree.load(recman, recid);
		recid = recman.getNamedObject("URLtoPageID");
		URLtoPageID = HTree.load(recman, recid);
		recid = recman.getNamedObject("pageIDtoURL");
		pageIDtoURL = HTree.load(recman, recid);
		recid = recman.getNamedObject("wordtoWordID");
		wordtoWordID = HTree.load(recman, recid);
		recid = recman.getNamedObject("wordIDtoWord");
		wordIDtoWord = HTree.load(recman, recid);
		recid = recman.getNamedObject("forwardIndex");
		forwardIndex = HTree.load(recman, recid);
		recid = recman.getNamedObject("parentToChild");
		parentToChild = HTree.load(recman, recid);
		recid = recman.getNamedObject("childToParent");
		childToParent = HTree.load(recman, recid);
		recid = recman.getNamedObject("pageProperties");
		pageProperties = HTree.load(recman, recid);
		recid = recman.getNamedObject("max_tfs");
		max_tfs = HTree.load(recman, recid);
		recid = recman.getNamedObject("phraseForwardIndex");
		phraseForwardIndex = HTree.load(recman, recid);
		recid = recman.getNamedObject("phrasetoPhraseID");
		phrasetoPhraseID = HTree.load(recman, recid);
		recid = recman.getNamedObject("phraseIDtoPhrase");
		phraseIDtoPhrase = HTree.load(recman, recid);

		tfidfs = new HashMap<Integer, List<Double>>(); // pageID -> [query1, query2, query3, ...] 
		tfidfs_phrase = new HashMap<Integer, List<Double>>(); // pageID -> [phrase1, phrase2, phrase3, ...] 
		tfidfsAll = new HashMap<Integer, List<Double>>(); 
		scores = new HashMap<Integer, Double>(); 
	}

	/** 
	 * Processing the word and phrase queries with tf idf calculations
	 * 
	 * @param queries array of single word queries
	 * @param queries array of two or three word phrases
	 * @throws Exception 
	 */
	public void processWords(Vector<String> queries, Vector<String> phrases) throws Exception {

		HashMap<Integer, double[]> temp_tfidfs = new HashMap<Integer, double[]>(queries.size());

		for ( int i = 0; i < queries.size(); i++) {
			String query = queries.get(i);
			FastIterator pageIDIteratorQuery = forwardIndex.keys();
			Object pageIDIter;
			if (wordtoWordID.get(query) == null) {
				while ((pageIDIter = pageIDIteratorQuery.next()) != null) {
					int pageID = (int) pageIDIter;
					if (temp_tfidfs.get(pageID) == null) {
						double[] tempList = new double[queries.size()];
						tempList[i] = 0.0;
						temp_tfidfs.put(pageID, tempList);
					} else {
						double[] tempList = temp_tfidfs.get(pageID);
						tempList[i] = 0.0;
						temp_tfidfs.put(pageID, tempList);
					}
				}
			}
		}


		FastIterator wordIDIterator = invertedIndex.keys();
		Object wordIDObject;
		while ((wordIDObject = wordIDIterator.next()) != null) {
			int wordID = (int) wordIDObject;
			String word = (String) wordIDtoWord.get(wordID);
			FastIterator pageIDIterator = forwardIndex.keys();
			FastIterator pageIDIterator2 = forwardIndex.keys();
			Object pageIDObject;
			int pageCount = 0;
			int inQueriesIndex = queries.indexOf(word);
			tfs = new HashMap<Integer, Double>(); // pageID -> freq
			dfs = new HashMap<Integer, Double>(); // wordID -> freq

			// Iterate over the keys and do something with each PageID
			while ((pageIDObject = pageIDIterator.next()) != null) {
				
				int pageID = (int) pageIDObject;
				HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) forwardIndex.get(pageID);
				if (temp_map.get(wordID) == null) {
					tfs.put(pageID, 0.0); // update tf
				} else {
					double tempFreq = (double) temp_map.get(wordID).intValue();
					tfs.put(pageID, tempFreq);

					// Update dfs
					if (dfs.get(wordID) == null) {
						dfs.put(wordID, 1.0);
					} else {
						dfs.put(wordID, dfs.get(wordID) + 1);
					}

				}
				pageCount++;
			}

			// Calculate idf
			double df = (double) dfs.get(wordID);
			double idf = Math.log(pageCount/ (double) df) / Math.log(2.0);
			
			// Update tfidf
			while ((pageIDObject = pageIDIterator2.next()) != null) {
				
				int pageID = (int) pageIDObject;
				double tf = (double) tfs.get(pageID);
				double tfidf = tf * idf;
				double max_tf = Double.valueOf(max_tfs.get(pageID).toString());
				tfidf = tfidf / max_tf;
				if (tfidfsAll.get(pageID) == null) {
					List<Double> tempList = new ArrayList<>();
					tempList.add(tfidf);
					tfidfsAll.put(pageID, tempList);
				} else {
					List<Double> tempList = tfidfsAll.get(pageID);
					tempList.add(tfidf);
					tfidfsAll.put(pageID, tempList);
				}

				if (temp_tfidfs.get(pageID) == null) {
					if (inQueriesIndex != -1) {
						double[] tempList = new double[queries.size()];
						tempList[inQueriesIndex] = tfidf;
						temp_tfidfs.put(pageID, tempList);
					}
					
				} else {
					if (inQueriesIndex != -1) {
						double[] tempList = temp_tfidfs.get(pageID);
						tempList[inQueriesIndex] = tfidf;
						temp_tfidfs.put(pageID, tempList);
					}
					
				}

			}
			
		}

		
		for (Map.Entry<Integer, double[]> entry : temp_tfidfs.entrySet()) {
			Integer key = entry.getKey();
			double[] values = entry.getValue();
		
			// Create a new List<Double> for the values
			List<Double> newList = new ArrayList<Double>();
			for (double value : values) {
				newList.add(value);
			}
		
			// Add the new List<Double> to tfidfs
			tfidfs.put(key, newList);
		}


		for (String phrase : phrases) {
			FastIterator pageIDIteratorQuery = phraseForwardIndex.keys();
			Object pageIDIter;
			if (phrasetoPhraseID.get(phrase) == null ) {
				while ((pageIDIter = pageIDIteratorQuery.next()) != null) {
					int pageID = (int) pageIDIter;
					if (tfidfs_phrase.get(pageID) == null) {
						List<Double> tempList = new ArrayList<>();
						tempList.add(0.0);
						tfidfs_phrase.put(pageID, tempList);
					} else {
						List<Double> tempList = (List<Double>) tfidfs_phrase.get(pageID);
						tempList.add(0.0);
						tfidfs_phrase.put(pageID, tempList);
					}
				}
				continue;
			}
			int phraseID = (int) phrasetoPhraseID.get(phrase);
			System.out.println(phraseID);
			
			FastIterator pageIDIterator = phraseForwardIndex.keys();
			FastIterator pageIDIterator2 = phraseForwardIndex.keys();
			Object pageIDObject;
			int pageCount = 0;
			tfs = new HashMap<Integer, Double>(); // pageID -> freq
			dfs = new HashMap<Integer, Double>(); // phraseID -> freq
			
			// Iterate over the keys and do something with each PageID
			while ((pageIDObject = pageIDIterator.next()) != null) {
				
				int pageID = (int) pageIDObject;
				
				HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) phraseForwardIndex.get(pageID);
				if (temp_map.get(phraseID) == null) {
					tfs.put(pageID, 0.0); // update tf
				} else {
					double tempFreq = (double) temp_map.get(phraseID).intValue();
					tfs.put(pageID, tempFreq);

					// Update dfs
					if (dfs.get(phraseID) == null) {
						dfs.put(phraseID, 1.0);
					} else {
						dfs.put(phraseID, dfs.get(phraseID) + 1);
					}

				}
				pageCount++;
			}
			// Calculate idf
			double df = (double) dfs.get(phraseID);
			double idf = Math.log(pageCount/ (double) df) / Math.log(2.0);
			
			// Update tfidf
			while ((pageIDObject = pageIDIterator2.next()) != null) {
				
				int pageID = (int) pageIDObject;
				double tf = (double) tfs.get(pageID);
				double tfidf = tf * idf;
				double max_tf = Double.valueOf(max_tfs.get(pageID).toString());
				tfidf = tfidf / max_tf;
				if (tfidfs_phrase.get(pageID) == null) {
					List<Double> tempList = new ArrayList<>();
					tempList.add(tfidf);
					tfidfs_phrase.put(pageID, tempList);
				} else {
					List<Double> tempList = tfidfs_phrase.get(pageID);
					tempList.add(tfidf);
					tfidfs_phrase.put(pageID, tempList);
				}

			}
		}

		return;

    }
	
	/** 
	 * Matching with title for each page, boosting the score by 0.2 every word/phrase
	 * 
	 * @param queries array of single word queries
	 * @param queries array of two or three word phrases
	 * @throws Exception 
	 */
	public void matchTitle(Vector<String> queries, Vector<String> phrases) throws Exception {

		for (String query : queries) {
			if (wordtoWordID.get(query) == null) {continue;}
			int wordID = (int) wordtoWordID.get(query);
			FastIterator pageIDIterator = forwardIndex.keys();
			Object pageIDObject;

			while ((pageIDObject = pageIDIterator.next()) != null) {
				int pageID = (int) pageIDObject;
				HashMap<Integer, Integer> title_map = (HashMap<Integer, Integer>)titleForwardIndex.get(pageID);
				if (title_map == null) {
					System.out.println("NULL TITLE -> " + pageID);
					continue;
				}
				if (title_map.get(wordID) != null) {
					double tempFreq = (double) title_map.get(wordID).intValue();
					
					if (scores.get(pageID) == null ) {
						scores.put(pageID, (0.2));
					} else {
						double temp = (double) scores.get(pageID);
						scores.put(pageID, (0.2) + temp);
					}
				} 

			}

		}

		for (String phrase : phrases) {
			if (wordtoWordID.get(phrase) == null) {continue;}
			int phraseID = (int) phrasetoPhraseID.get(phrase);
			FastIterator pageIDIterator = titleForwardIndex.keys();
			Object pageIDObject;

			while ((pageIDObject = pageIDIterator.next()) != null) {
				int pageID = (int) pageIDObject;
				HashMap<Integer, Integer> title_map = (HashMap<Integer, Integer>)titleForwardIndex.get(pageID);
				if (title_map == null) {
					System.out.println("NULL TITLE -> " + pageID);
					continue;
				}
				if (title_map.get(phraseID) != null) {
					double tempFreq = (double) title_map.get(phraseID).intValue();
					
					if (scores.get(pageID) == null ) {
						scores.put(pageID, (0.2));
					} else {
						double temp = (double) scores.get(pageID);
						scores.put(pageID, (0.2) + temp);
					}
				} 

			}

		}

	}

	public void calculateScores(List<Double> queryWeights, List<Double> phraseWeights) throws Exception {

		FastIterator pageIDIterator = forwardIndex.keys();
		Object pageIDObject;

		while ((pageIDObject = pageIDIterator.next()) != null) {

			int pageID = (int) pageIDObject;
			List<Double> tfidfList = (List<Double>) tfidfs.get(pageID);
			List<Double> tfidfListPhrase = (List<Double>) tfidfs_phrase.get(pageID);
			List<Double> tfidfListAll = (List<Double>) tfidfsAll.get(pageID);
			double dotProduct = 0.0;
			double magnitude1 = 0.0;
			double magnitude2 = 0.0;
			double cosineSimilarity = 0.0;

			for (int i = 0; i < tfidfs.size(); ++i) 
			{
				if (i == queryWeights.size()) {break;}
				dotProduct += queryWeights.get(i) * tfidfList.get(i);  //a.b
				magnitude1 += Math.pow(queryWeights.get(i), 2);  //(a^2)
			}

			for (int i = 0; i < tfidfs_phrase.size(); ++i) 
			{
				if (i == phraseWeights.size()) {break;}
				dotProduct += phraseWeights.get(i) * tfidfListPhrase.get(i);  //a.b
				magnitude1 += Math.pow(phraseWeights.get(i), 2);  //(a^2)
			}

			for (int i = 0; i < tfidfsAll.size(); ++i) {
				magnitude2 += Math.pow(tfidfListAll.get(i), 2); //(b^2)
			}

			magnitude1 = Math.sqrt(magnitude1);//sqrt(a^2)
			magnitude2 = Math.sqrt(magnitude2);//sqrt(b^2)
			if (magnitude1 != 0.0 || magnitude2 != 0.0) 
			{
				cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
				System.out.println(pageID + " -> " + cosineSimilarity);
			} 
			else 
			{
				cosineSimilarity = 0.0;
			}

			if (scores.get(pageID) == null ) {
				scores.put(pageID, cosineSimilarity);
			} else {
				scores.put(pageID, cosineSimilarity + scores.get(pageID));
			}
		}

		System.out.print(scores + "\n");
		return;
	}

	/** 
	 * Retrieve the top 50 pages for the query 
	 */
	public int[] retrieveTop50() {
		int[] top50Keys = scores.entrySet().stream()
						.filter(e -> !Double.isNaN(e.getValue()))
						.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
						.limit(50)
						.mapToInt(Map.Entry::getKey)
						.toArray();
		
		return top50Keys;
	}

	public String output(int pageID) throws Exception {

		String resultString = "";
		double currentScore = (double) scores.get(pageID);
		if (currentScore < 0.00001) {
			return resultString;
		}

		resultString += String.format("%.5f", currentScore); 

		resultString += "  ";

		String url = (String) pageIDtoURL.get(pageID);
		Vector<String> properties = (Vector<String>) pageProperties.get(pageID);
		resultString += "<a href=\""+url+"\">" + properties.get(0) + "</a>\n";
		resultString += "\t" + url + "\n";
		String dateAndSize = new Date(Long.parseLong(properties.get(1))) + ", " + properties.get(2);
		resultString += "\t" + dateAndSize.toString() + "\n \t";
		
		HashMap<Integer, Integer> forwardIndexMap = (HashMap<Integer, Integer>) forwardIndex.get(pageID);
		int key_count = 0;
		if (forwardIndexMap != null) {
			List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(forwardIndexMap.entrySet());
			Collections.sort(entries, Map.Entry.<Integer, Integer>comparingByValue().reversed());
			for (Map.Entry<Integer, Integer> entry : entries) {
				Integer key = entry.getKey();
				Integer value = entry.getValue();
				String word = (String) wordIDtoWord.get(key);
				if (!word.matches(".*[a-zA-Z]+.*")) {continue;}
				resultString += word + " " + value + "; ";
				key_count++;
				if (key_count == 5) {break;}
			}
		} else {
			resultString += "No Content";
		}

		resultString += "\n";

		List<Integer> parentList = (List<Integer>) childToParent.get(pageID);
		if (parentList != null) {
			for (int i = 0; i < parentList.size(); i++) {
				if (i == 10) {break;}
				int parentID = parentList.get(i);
				String parent = (String) pageIDtoURL.get(parentID);
				resultString += "\tParent " + (i+1) + ": " + parent + "\n";
			}
		} else {
			resultString += "\tNo Parent \n";
		}

		List<Integer> childList = (List<Integer>) parentToChild.get(pageID);
		if (childList != null) {
			for (int i = 0; i < childList.size(); i++) {
				if (i == 10) {break;}
				int childID = childList.get(i);
				String child = (String) pageIDtoURL.get(childID);
				resultString += "\tChild " + (i+1) + ": " + child + "\n";
			}
		} else {
			resultString += "\tNo Child \n";
		}

		resultString += "\n ------------------------------------------------------- \n";
		return resultString;
	}	
}

