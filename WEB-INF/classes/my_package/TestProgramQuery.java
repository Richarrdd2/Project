package my_package;
import java.io.*;
import java.util.*;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.recman.CacheRecordManager;
import jdbm.helper.FastIterator;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;

public class TestProgramQuery 
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

	public static List<String> visitedURLs; // List of all the URLs that has been indexed

	/** 
	 * Constructor of testprogram, which imports the tables from "project" database
	 * 
	 * @throws Exception
	 */
	public TestProgramQuery() throws Exception{
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
	 * To debug and print all keys and values of a HTree
	 * 
	 * @throws Exception
	 */
	public void printAllString(HTree hashtable) throws IOException
	{
		// Print all the data in the HTree
		FastIterator iter = hashtable.keys();
		String key;	
		while( (key = (String)iter.next())!=null)
		{
			System.out.println(key + " : " + hashtable.get(key));
		}
	}	

	/** 
	 * Imitate a web page to test query scoring
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public static void main (String[] args) throws Exception
	{
			SearchEngine searchEngine = new SearchEngine();

			TestProgramQuery testprogramquery = new TestProgramQuery();

			Vector<String> queries = new Vector<String>();
			queries.add("stone");
			queries.add("movi");

			List<Double> queryWeights = new ArrayList<Double>();
			queryWeights.add(1.0);
			queryWeights.add(1.0);

			Vector<String> phrases = new Vector<String>();
			phrases.add("video clip");
			phrases.add("moral compromist charact");

			List<Double> phraseWeights = new ArrayList<Double>();
			phraseWeights.add(1.0);
			phraseWeights.add(1.0);

			searchEngine.processWords(queries, phrases);

			searchEngine.matchTitle(queries, phrases);
	
			searchEngine.calculateScores(queryWeights, phraseWeights);
			
			int[] top50 = searchEngine.retrieveTop50();

			for (int i = 0; i < top50.length; i++) {
				String outputString = searchEngine.output(top50[i]);
				System.out.print(outputString);
			}
			

	}
}