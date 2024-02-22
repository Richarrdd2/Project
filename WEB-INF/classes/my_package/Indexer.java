package my_package;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.htmlparser.beans.LinkBean;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.StringBean;
import org.htmlparser.Parser;
import org.htmlparser.tags.TitleTag;
import java.util.StringTokenizer;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import java.util.Vector;
import java.io.IOException;
public class Indexer
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

	private HTree nextIDcounter;
	private int nextWordID; // to assign ID for the next new word
	private int nextPageID;
	private int nextPhraseID;

	public StopStem stemmer;

	/** 
	 * Indexer constructor
	 * 
	 * @param recordmanager recordmanager database
	 * 
	 * @throws IOException 
	 */
	Indexer(String recordmanager) throws IOException
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
		
	}

	/** 
	 * Indexing function
	 * 
	 * @param recordmanager recordmanager database
	 * 
	 * @throws IOException 
	 */
	public void indexing(String url, int pageID) throws Exception {
		Vector<String> content = new Vector<>();
		List<String> phraseWords = new ArrayList<String>();
		int currentWordID;
		int max_tf = 1;

		System.out.print("Storing words...");

		for (int i = 0; i < content.size(); i++) {
			
			String currentWord = content.get(i);
			currentWord = currentWord.replaceAll("[^A-Za-z]", "").toLowerCase();
			
			if (stemmer.isStopWord(currentWord) || currentWord.length()==0) {
				continue;
			} else {
				currentWord = stemmer.stem(currentWord);
				phraseWords.add(currentWord);
			} 			

			// Get wordID and update wordID map
			if (wordtoWordID.get(currentWord) == null) {
				wordtoWordID.put(currentWord, nextWordID);
				wordIDtoWord.put(nextWordID, currentWord);
				currentWordID = nextWordID;
				nextWordID++;
			} else {
				currentWordID = (int) wordtoWordID.get(currentWord);
			}

			// Insert to Inverted Index
			if (invertedIndex.get(currentWordID) != null) {
				HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) invertedIndex.get(currentWordID);
				if (temp_map.get(pageID) == null) {
					temp_map.put(pageID, 1);
					invertedIndex.put(currentWordID, temp_map);
				} else {
					int temp = (int) temp_map.get(pageID);
					temp = temp + 1;
					temp_map.remove(pageID);
					temp_map.put(pageID, temp);
					invertedIndex.put(currentWordID, temp_map);
				}
				
			} else {
				HashMap<Integer, Integer> temp_map = new HashMap<>();
				temp_map.put(pageID, 1);
				invertedIndex.put(currentWordID, temp_map);
			}

			// Insert to Forward Index
			if (forwardIndex.get(pageID) != null) {
				HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) forwardIndex.get(pageID);
				if (temp_map.get(currentWordID) == null) {
					temp_map.put(currentWordID, 1);
					forwardIndex.put(pageID, temp_map);
				} else {
					int temp = (int) temp_map.get(currentWordID);
					temp = temp + 1;
					if (temp > max_tf) {
						max_tf = temp;
					}
					temp_map.remove(currentWordID);
					temp_map.put(currentWordID, temp);
					forwardIndex.put(pageID, temp_map);
				}
				
			} else {
				HashMap<Integer, Integer> temp_map = new HashMap<>();
				temp_map.put(currentWordID, 1);
				forwardIndex.put(pageID, temp_map);
			}
			
			
		}

		// Handle indexing bigrams 
		for (int i = 0; i < phraseWords.size() - 1; i++) {
			String currentPhrase = phraseWords.get(i) + " " + phraseWords.get(i + 1);
			int currentPhraseID;
			
			// Get wordID and update wordID map
			if (phrasetoPhraseID.get(currentPhrase) == null) {
				phrasetoPhraseID.put(currentPhrase, nextPhraseID);
				phraseIDtoPhrase.put(nextPhraseID, currentPhrase);
				currentPhraseID = nextPhraseID;
				nextPhraseID++;
			} else {
				currentPhraseID = (int) phrasetoPhraseID.get(currentPhrase);
			}

			// Insert to Forward Index
			if (phraseForwardIndex.get(pageID) != null) {
				HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) phraseForwardIndex.get(pageID);
				if (temp_map.get(currentPhraseID) == null) {
					temp_map.put(currentPhraseID, 1);
					phraseForwardIndex.put(pageID, temp_map);
				} else {
					int temp = (int) temp_map.get(currentPhraseID);
					temp = temp + 1;
					temp_map.remove(currentPhraseID);
					temp_map.put(currentPhraseID, temp);
					phraseForwardIndex.put(pageID, temp_map);
				}
				
			} else {
				HashMap<Integer, Integer> temp_map = new HashMap<>();
				temp_map.put(currentPhraseID, 1);
				phraseForwardIndex.put(pageID, temp_map);
				System.out.println("storing phrase");
			}

		}

		// Handle indexing trigrams
		for (int i = 0; i < phraseWords.size() - 2; i++) {
			String currentPhrase = phraseWords.get(i) + " " + phraseWords.get(i + 1) + " " + phraseWords.get(i + 2);
			int currentPhraseID;

			// Get wordID and update wordID map
			if (phrasetoPhraseID.get(currentPhrase) == null) {
				phrasetoPhraseID.put(currentPhrase, nextPhraseID);
				phraseIDtoPhrase.put(nextPhraseID, currentPhrase);
				currentPhraseID = nextPhraseID;
				nextPhraseID++;
			} else {
				currentPhraseID = (int) phrasetoPhraseID.get(currentPhrase);
			}

			// Insert to Forward Index
			if (phraseForwardIndex.get(pageID) != null) {
				HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) phraseForwardIndex.get(pageID);
				if (temp_map.get(currentPhraseID) == null) {
					temp_map.put(currentPhraseID, 1);
					phraseForwardIndex.put(pageID, temp_map);
				} else {
					int temp = (int) temp_map.get(currentPhraseID);
					temp = temp + 1;
					temp_map.remove(currentPhraseID);
					temp_map.put(currentPhraseID, temp);
					phraseForwardIndex.put(pageID, temp_map);
				}
				
			} else {
				HashMap<Integer, Integer> temp_map = new HashMap<>();
				temp_map.put(currentPhraseID, 1);
				phraseForwardIndex.put(pageID, temp_map);
				System.out.println("storing phrase");
			}
		}

		max_tfs.put(pageID, max_tf);
        return;
    }
}
