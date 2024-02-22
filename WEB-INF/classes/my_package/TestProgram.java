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

public class TestProgram 
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
	public TestProgram() throws Exception{
		String dbPath = System.getenv("CATALINA_HOME") + "/bin/assets/project";
		recman = RecordManagerFactory.createRecordManager(dbPath);
		long recid = recman.getNamedObject("invertedIndex");
		System.out.println(recid);
		invertedIndex = HTree.load(recman, recid);
		recid = recman.getNamedObject("titleForwardIndex");
		titleForwardIndex = HTree.load(recman, recid);
		recid = recman.getNamedObject("URLtoPageID");
		System.out.println(recid);
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
		// recid = recman.getNamedObject("threeWordForwardIndex");
		// threeWordForwardIndex = HTree.load(recman, recid);
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
	 * A function to produce "spider_result.txt" which include all retrieved pages and 
	 * their information
	 * 
	 * @throws Exception
	 */
	public boolean output()  throws Exception{
		String spider_result_path = System.getenv("CATALINA_HOME") + "/bin/assets/spider_result.txt";
		PrintWriter outputText = new PrintWriter(spider_result_path, "UTF-8");
		for (String url : visitedURLs) {

			int pageID = (int) URLtoPageID.get(url);
			Vector<String> properties = (Vector<String>) pageProperties.get(pageID);
			outputText.println(properties.get(0));
			outputText.println(url);
			outputText.println(new Date(Long.parseLong(properties.get(1))) + "; " + properties.get(2));
			
			HashMap<Integer, Integer> forwardIndexMap = (HashMap<Integer, Integer>) forwardIndex.get(pageID);
			int key_count = 0;
			if (forwardIndexMap != null) {
				for (Integer key : forwardIndexMap.keySet()) {
					String word = (String) wordIDtoWord.get(key);
					int frequency = (int) forwardIndexMap.get(key);
					if (word.matches(".*[a-zA-Z]+.*") == false) {continue;}
					outputText.print(word + " " + frequency + "; ");
					key_count++;
					if (key_count == 10) {break;}
				}
			} else {
				outputText.println("No Content");
			}
			
			outputText.print("\n");

			List<Integer> childList = (List<Integer>) parentToChild.get(pageID);
			if (childList != null) {
				for (int i = 0; i < childList.size(); i++) {
					if (i == 10) {break;}
					int childID = childList.get(i);
					String child = (String) pageIDtoURL.get(childID);
					outputText.println("Child " + (i+1) + ": " + child);
				}
			} else {
				outputText.println("No Child");
			}

			outputText.println("-------------------------------------------");
		}
		outputText.close();
		return true;
	}

	public static void main (String[] args) throws Exception
	{
			Crawler crawler = new Crawler("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm");
            crawler.crawl(crawler.rootURL, 300);

			TestProgram testprogram = new TestProgram();

			testprogram.visitedURLs = crawler.visitedURLs;

			testprogram.output();

	}
}