## Crawling
To crawl the pages, you first need to compile the test program with the following command:

javac -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;" classes/IRUtilities/Porter.java classes/my_package/StopStem.java classes/my_package/Indexer.java classes/my_package/Crawler.java classes/my_package/TestProgram.java classes/my_package/SearchEngine.java classes/my_package/TestProgramQuery.java

After compiling all the required files, you can run the crawl test with this command:

java -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;./classes" my_package.TestProgram

## WebPage
To run the JSP web page, you need to set the following environment variables:
- CATALINA_HOME = tomcat path (e.g., C:\apache-tomcat-9.0.70)
- JAVA_HOME = JDK path (e.g., C:\Program Files\Java\jdk-19)

Once the environment variables are set, you can run the webpage using the following command in the terminal:
%CATALINA_HOME%\bin\startup.bat

Now the server is open, and you can open the web page by typing the following URL in your browser:
http://localhost:8080/Project/WebPage.html

To shutdown, run the following command:
%CATALINA_HOME%\bin\shutdown.bat

## TestQuery
To run the query testing without using the web page, you need to compile the files using this command:
javac -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;" classes/IRUtilities/Porter.java classes/my_package/StopStem.java classes/my_package/Indexer.java classes/my_package/Crawler.java classes/my_package/TestProgram.java classes/my_package/SearchEngine.java classes/my_package/TestProgramQuery.java

After compiling, you can run the query test program with this command:
java -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;./classes" my_package.TestProgramQuery
