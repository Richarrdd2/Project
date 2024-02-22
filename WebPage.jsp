<%@ page import="java.util.*"%>
<%@ page import="my_package.*"%>

<html>
<body>

Query:
<form method="post" action="WebPage.jsp"> 
<input type="text" name="query"> 
<input type="submit" value="Enter"> 
</form> 


<%
if(request.getParameter("query") != "" && request.getParameter("query") != null)
{
    out.print("Your queries: ");
    String[] tokens = request.getParameter("query").split(" ");
    Vector<String> queries = new Vector<String>();
    List<Double> queryWeights = new ArrayList<Double>(); 
    Vector<String> phrases = new Vector<String>();
    List<Double> phraseWeights = new ArrayList<Double>(); 

    String phrase = "";
    boolean is_phrase = false;
    String StopStempath = System.getenv("CATALINA_HOME") + "/bin/assets/stopwords.txt";
    StopStem stopStem = new StopStem(StopStempath);
    SearchEngine searchengine = new SearchEngine();
    for(int i = 0; i < tokens.length; i++) {
        String word = tokens[i];
        word = word.toLowerCase();

        if (stopStem.isStopWord(word) || word.length()==0) {
            continue;
        }

        if (is_phrase == true) {
            if (word.charAt(word.length() - 1) == '\"') {
                is_phrase = false;
                phrase = phrase + " "+ stopStem.stem(word);

                int index = phrases.indexOf(phrase);
                if (index != -1) {
                    // is in query
                    double previousValue = phraseWeights.get(index);
                    double newValue = previousValue + 1.0;
                    phraseWeights.set(index, newValue);
                } else {
                // not in queries
                    phrases.add(phrase);
                    phraseWeights.add(1.0);
                }
                phrase = "";
            } else {
                phrase = phrase +" "+ stopStem.stem(word);
            }
        }
        else if (word.charAt(0) == '\"') {
            is_phrase = true;
            word = stopStem.stem(word);
            phrase = word;
        }
        else {
            String stemmed_word = stopStem.stem(word);
            int index = queries.indexOf(stemmed_word);
            if (index != -1) {
                // is in query
                double previousValue = queryWeights.get(index);
                double newValue = previousValue + 1.0;
                queryWeights.set(index, newValue);
            } else {
                // not in queries
                queries.add(stemmed_word);
                queryWeights.add(1.0);
            }
        }
        
    }

    out.println(queries);
    out.println(phrases);
    out.println(queryWeights);
    out.println(phraseWeights + "<br> <br>");
    searchengine.processWords(queries, phrases);
    searchengine.matchTitle(queries, phrases);

    searchengine.calculateScores(queryWeights, phraseWeights);
			
    int[] top50 = searchengine.retrieveTop50();

    for (int i = 0; i < top50.length; i++) {
        String outputString = searchengine.output(top50[i]);
        outputString = outputString.replaceAll("\n", "<br>").replaceAll("\t", "&emsp;&emsp;&emsp;");
        out.print(outputString);
    }
}
else
{
	out.println("You input nothing!");
}


%>
</body>
</html>
