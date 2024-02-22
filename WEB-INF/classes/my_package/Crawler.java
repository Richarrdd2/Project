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

public class Crawler
{
	private Queue<String> urlQueue; // Queue of URLs to crawl by using BFS method
    public List<String> visitedURLs; // List of all the URLs that has been indexed
    public String rootURL; // The first website to be indexed 

    public RecordManager recman; 
	private HTree titleForwardIndex;
	private HTree invertedIndex;
	private HTree URLtoPageID;
	private HTree pageIDtoURL;
	private HTree wordtoWordID;
	private HTree wordIDtoWord;
	private HTree phrasetoPhraseID;
	private HTree phraseIDtoPhrase;
	private HTree forwardIndex;
	private HTree parentToChild;
	private HTree childToParent;
	private HTree pageProperties;
	private HTree nextIDcounter;
	private HTree max_tfs;
	private HTree phraseForwardIndex;
	private int nextWordID; // to assign ID for the next new word
	private int nextPageID; // to assign ID for the next new URL
	private int nextPhraseID; // to assign ID for the next new phrase

	public StopStem stemmer;

	/** 
	 * spider object Constructor
	 * 
	 * @param rootURL the first URL to crawl from
	 * @param count the maximum unique fetched URLs
	 * @throws Exception 
	 */
    public Crawler(String root_url) throws IOException {

        urlQueue = new LinkedList<>();
        visitedURLs = new ArrayList<>();
        this.rootURL = root_url;
		String dbPath = System.getenv("CATALINA_HOME") + "/bin/assets/project";
		String StopStempath = System.getenv("CATALINA_HOME") + "/bin/assets/stopwords.txt";
		stemmer = new StopStem(StopStempath);
		// Create database
		recman = RecordManagerFactory.createRecordManager(dbPath);
		long recid = recman.getNamedObject("URLtoPageID");
		if (recid != 0) { URLtoPageID = HTree.load(recman, recid);		
		} else {
			URLtoPageID = HTree.createInstance(recman);
			recman.setNamedObject( "URLtoPageID", URLtoPageID.getRecid() );
		}
		recid = recman.getNamedObject("wordtoWordID");
		if (recid != 0) { wordtoWordID = HTree.load(recman, recid); System.out.println("loaded");		
		} else {
			wordtoWordID = HTree.createInstance(recman);
			recman.setNamedObject( "wordtoWordID", wordtoWordID.getRecid() );
			System.out.println("created new");
		}
		recid = recman.getNamedObject("invertedIndex");
		if (recid != 0) { invertedIndex = HTree.load(recman, recid);		
		} else {
			invertedIndex = HTree.createInstance(recman);
			recman.setNamedObject( "invertedIndex", invertedIndex.getRecid() );
		}
		recid = recman.getNamedObject("titleForwardIndex");
		if (recid != 0) { titleForwardIndex = HTree.load(recman, recid);		
		} else {
			titleForwardIndex = HTree.createInstance(recman);
			recman.setNamedObject( "titleForwardIndex", titleForwardIndex.getRecid() );
		}
		recid = recman.getNamedObject("pageIDtoURL");
		if (recid != 0) { pageIDtoURL = HTree.load(recman, recid);		
		} else {
			pageIDtoURL = HTree.createInstance(recman);
			recman.setNamedObject( "pageIDtoURL", pageIDtoURL.getRecid() );
		}
		recid = recman.getNamedObject("wordIDtoWord");
		if (recid != 0) { wordIDtoWord = HTree.load(recman, recid);		
		} else {
			wordIDtoWord = HTree.createInstance(recman);
			recman.setNamedObject( "wordIDtoWord", wordIDtoWord.getRecid() );
		}
		recid = recman.getNamedObject("forwardIndex");
		if (recid != 0) { forwardIndex = HTree.load(recman, recid);		
		} else {
			forwardIndex = HTree.createInstance(recman);
			recman.setNamedObject( "forwardIndex", forwardIndex.getRecid() );
		}
		recid = recman.getNamedObject("parentToChild");
		if (recid != 0) { parentToChild = HTree.load(recman, recid);		
		} else {
			parentToChild = HTree.createInstance(recman);
			recman.setNamedObject( "parentToChild", parentToChild.getRecid() );
		}
		recid = recman.getNamedObject("childToParent");
		if (recid != 0) { childToParent = HTree.load(recman, recid);		
		} else {
			childToParent = HTree.createInstance(recman);
			recman.setNamedObject( "childToParent", childToParent.getRecid() );
		}
		recid = recman.getNamedObject("pageProperties");
		if (recid != 0) { pageProperties = HTree.load(recman, recid);		
		} else {
			pageProperties = HTree.createInstance(recman);
			recman.setNamedObject( "pageProperties", pageProperties.getRecid() );
		}
		recid = recman.getNamedObject("nextIDcounter");
		if (recid != 0) { 
			nextIDcounter = HTree.load(recman, recid);
			nextWordID = (int) nextIDcounter.get("nextWordID");
			nextPageID = (int) nextIDcounter.get("nextPageID");	
			nextPhraseID = (int) nextIDcounter.get("nextPhraseID");
		} else {
			nextIDcounter = HTree.createInstance(recman);
			recman.setNamedObject( "nextIDcounter", nextIDcounter.getRecid() );
			nextWordID = 1;
			nextPageID = 1;	
			nextPhraseID = 1;
			nextIDcounter.put("nextWordID", nextWordID);
			nextIDcounter.put("nextPageID", nextPageID);
			nextIDcounter.put("nextPhraseID", nextPhraseID);
		}
		recid = recman.getNamedObject("max_tfs");
		if (recid != 0) { max_tfs = HTree.load(recman, recid);		
		} else {
			max_tfs = HTree.createInstance(recman);
			recman.setNamedObject( "max_tfs", max_tfs.getRecid() );
		}
		recid = recman.getNamedObject("phraseForwardIndex");
		if (recid != 0) { phraseForwardIndex = HTree.load(recman, recid);		
		} else {
			phraseForwardIndex = HTree.createInstance(recman);
			recman.setNamedObject( "phraseForwardIndex", phraseForwardIndex.getRecid() );
		}
		recid = recman.getNamedObject("phraseIDtoPhrase");
		if (recid != 0) { phraseIDtoPhrase = HTree.load(recman, recid);		
		} else {
			phraseIDtoPhrase = HTree.createInstance(recman);
			recman.setNamedObject( "phraseIDtoPhrase", phraseIDtoPhrase.getRecid() );
		}
		recid = recman.getNamedObject("phrasetoPhraseID");
		if (recid != 0) { phrasetoPhraseID = HTree.load(recman, recid);		
		} else {
			phrasetoPhraseID = HTree.createInstance(recman);
			recman.setNamedObject( "phrasetoPhraseID", phrasetoPhraseID.getRecid() );
		}

    }

	/** 
	 * Main function to start crawling and indexing the child links and keywords
	 * 
	 * @param rootURL the first URL to crawl from
	 * @param count the maximum unique fetched URLs
	 * @throws Exception includes IOException, ParserException
	 */
	public void crawl(String rootURL, int count) throws Exception {
        urlQueue.add(rootURL);
        int current = count - 1;

        while(!urlQueue.isEmpty()){

            // BFS implementation with queue
            String s = urlQueue.remove();
			s = s.replaceAll("[\\s\\n\\t]+", "");

			if (current < 0) { // Passed URL retreived limit
				break;
			} else if (visitedURLs.contains(s)) { // Update URL contents
				
			} else { // New URL 
				int currentPageID;
				if (URLtoPageID.get(s) == null) {
					System.out.println("store new pageID" + nextPageID);
					currentPageID = nextPageID;
					nextPageID  = nextPageID + 1;
					URLtoPageID.put(s, currentPageID);
					pageIDtoURL.put(currentPageID, s);
				} else {
					currentPageID = (int) URLtoPageID.get(s);
				}

				if (forwardIndex.get(currentPageID) == null) {
					indexing(s, currentPageID);
				} else {
					System.out.println("Stored already!");
					URL obj = new URL(s);
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();
					con.setRequestMethod("GET");
					long lastModifiedNow = con.getLastModified();
					Vector<String> tempArray = (Vector<String>) pageProperties.get(currentPageID);
					long lastModifiedThen = Long.parseLong(tempArray.get(1));
					if (lastModifiedNow > lastModifiedThen) {
						System.out.println("Modifying LONGGG TIME");
						forwardIndex.remove(currentPageID);

						//Issue: taking too long to delete
						Enumeration<Object> keys = (Enumeration<Object>) invertedIndex.keys();
						while (keys.hasMoreElements()) {
    						String key = (String) keys.nextElement();
							HashMap<Integer, Integer> temp_map =  (HashMap<Integer, Integer>) invertedIndex.get(key);
							if (temp_map.containsKey(currentPageID)) {
								temp_map.remove(currentPageID);
								invertedIndex.put(key, temp_map);
							}
						}

						indexing(s, currentPageID);
					}
				}
				
				
				visitedURLs.add(s);
		
				LinkBean bean = new LinkBean();
				bean.setURL(s);
				URL[] urls = bean.getLinks();
				System.out.print("-> Setting Child Parent");
				for (URL url : urls) {
					String url_string = url.toString();
					url_string = url_string.replaceAll("[\\s\\n\\t]+", "");
					urlQueue.add(url_string); 
					int tempID;
					
					if (URLtoPageID.get(url_string) == null) {
						tempID = nextPageID;
						nextPageID++;
						URLtoPageID.put(url_string, tempID);
						pageIDtoURL.put(tempID, url_string);
					} else {
						tempID = (int) URLtoPageID.get(url_string);
					}

					if(parentToChild.get(currentPageID) == null) {
						List<Integer> temp_list = new ArrayList<>();
						temp_list.add(tempID);
						parentToChild.put(currentPageID, temp_list);
					} else {
						List<Integer> temp_list = (List<Integer>) parentToChild.get(currentPageID);
						temp_list.add(tempID);
						parentToChild.remove(currentPageID);
						parentToChild.put(currentPageID, temp_list);
					}

					if (childToParent.get(tempID) == null) {
						List<Integer> temp_list = new ArrayList<>();
						temp_list.add(currentPageID);
						childToParent.put(tempID, temp_list);
					} else {
						List<Integer> temp_list = (List<Integer>) childToParent.get(tempID);
						temp_list.add(currentPageID);
						childToParent.remove(tempID);
						childToParent.put(tempID, temp_list);
					}

            	}
				
				setProperties(s);
				current--;
				System.out.println(s);

			}  

			
        }

		store();
    }

	/** 
	 * Extract the keywords in the url and put it in the database which includes 
	 * forward index, inverted index, and the word->wordID conversion
	 * 
	 * @param url the urk source to extract the keywords
	 * @param pageID the pageID of the url source
	 * @throws ParserException
	 * @throws IOException
	 */

    public void indexing(String url, int pageID) throws Exception {
		Vector<String> content = extractWords(url);
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

	/** 
	 * Extract information about the url. The store information include title, last modified date,
	 * and size of the page
	 * 
	 * @param url the url source to extract the properties
	 * @throws Exception
	 */
	public void setProperties(String url) throws Exception {
		System.out.print("-> Properties");
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		
		int pageID = (int) URLtoPageID.get(url);
		int currentWordID;
		String title; 

		try {
			
			Parser parser = new Parser(obj.openConnection());

			TitleTag titleTag = (TitleTag) parser.extractAllNodesThatMatch(node -> node instanceof TitleTag).elementAt(0);
			title = titleTag.getTitle();
			System.out.println("\n" + title);

			// Store in titleForwardIndex
			String[] words = title.split(" ");
			if (titleForwardIndex.get(pageID) == null) {
				System.out.print("-> Title Forward Index \n");
				for (int i = 0; i < words.length; i++) {
					String currentWord = words[i];
					currentWord = currentWord.replaceAll("[^A-Za-z]", "").toLowerCase();
				
					if (stemmer.isStopWord(currentWord) || currentWord.length()==0) {
						continue;
					} else {
						currentWord = stemmer.stem(currentWord);
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
	
					// Insert to Forward Index
					if (titleForwardIndex.get(pageID) != null) {
						HashMap<Integer, Integer> temp_map = (HashMap<Integer, Integer>) titleForwardIndex.get(pageID);
						if (temp_map.get(currentWordID) == null) {
							temp_map.put(currentWordID, 1);
							titleForwardIndex.put(pageID, temp_map);
							// System.out.println(currentWord);
						} else {
							int temp = (int) temp_map.get(currentWordID);
							temp = temp + 1;
							temp_map.remove(currentWordID);
							temp_map.put(currentWordID, temp);
							titleForwardIndex.put(pageID, temp_map);
							// System.out.println(currentWord);
						}
						
					} else {
						HashMap<Integer, Integer> temp_map = new HashMap<>();
						temp_map.put(currentWordID, 1);
						titleForwardIndex.put(pageID, temp_map);
						// System.out.println(currentWord);
					}
				}
			}
			
			
		} catch (Exception e) {
			title = "No Title";
		}

		int contentLengthInt = con.getContentLength();
		String contentLength = Integer.toString(contentLengthInt);
		long lastModified = con.getLastModified();
		long date = con.getDate();
		if (lastModified != 0) {
			// Do nothing
		} else if (date != 0) {
			lastModified = date;
		} else {
			lastModified = 0;
		}

		if (contentLengthInt == -1)
		{
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String input;
				StringBuilder content = new StringBuilder();
				while ((input = reader.readLine()) != null) {
					content.append(input);
				}
				reader.close();

				contentLength = Integer.toString(content.length()) + " chars";

			} catch (IOException e) {

			}
		}

		if (pageProperties.get(pageID) == null) {
			Vector<String> tempArray = new Vector<String>();
			tempArray.add(title);
			tempArray.add(Long.toString(lastModified));
			tempArray.add(contentLength);
			pageProperties.put(pageID, tempArray);
		} else {
			Vector<String> tempArray = (Vector<String>) pageProperties.get(pageID);
			tempArray.set(0, title);
			tempArray.set(1, Long.toString(lastModified));
			tempArray.set(2, contentLength);
			pageProperties.put(pageID, tempArray);
		}
		System.out.print("-> Done \n");
		return;
	}

	/** 
	 * To extract all words in a page
	 * 
	 * @param url the url source to extract the words
	 * @throws ParserException
	 */
	public Vector<String> extractWords(String url) throws ParserException
	{
        Vector<String> result = new Vector<String>();
		StringBean bean = new StringBean();
		bean.setURL(url);
		bean.setLinks(false);
		String contents = bean.getStrings();
		StringTokenizer st = new StringTokenizer(contents);
		while (st.hasMoreTokens()) {
		result.add(st.nextToken());
		}
		return result;
	}

	/** 
	 * To extract all child links of a page
	 * 
	 * @param url the url source to extract the child links
	 * @throws ParserException
	 */
	public Vector<String> extractLinks(String url) throws ParserException
	{
	    Vector<String> result = new Vector<String>();
		LinkBean bean = new LinkBean();
		bean.setURL(url);
		URL[] urls = bean.getLinks();
		for (URL s : urls) {
		result.add(s.toString());
		}
		return result;
	}
	

	/** 
	 * A function to commit and close the database record manager
	 * 
	 * @throws IOException
	 */
	public void store() throws IOException
	{
		nextIDcounter.put("nextWordID", nextWordID);
		nextIDcounter.put("nextPageID", nextPageID);
		nextIDcounter.put("nextPhraseID", nextPhraseID);
		System.out.println("IDs " + nextPageID + " "+ nextWordID + " " + nextPhraseID);
		recman.commit();
		recman.close();				
	} 

}