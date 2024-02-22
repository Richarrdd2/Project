Crawling: 
    To crawl the pages, we first need to compile the test program with this command 
    => “javac -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;" classes/IRUtilities/Porter.java classes/my_package/StopStem.java classes/my_package/Indexer.java classes/my_package/Crawler.java classes/my_package/TestProgram.java classes/my_package/SearchEngine.java classes/my_package/TestProgramQuery.java”

    After compiling all the required files, the crawl test can be run with this command 
    => “java -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;./classes" my_package.TestProgram”

WebPage:
    To run the JSP web page, we set a variable name CATALINA_HOME and JAVA_HOME:
        CATALINA HOME  = tomcat path ex. C:\apache-tomcat-9.0.70
        JAVA HOME = jdk path ex. C:\Program Files\Java\jdk-19

    After setting the environment to the apache folder, we can run the webpage using this command in terminal 
    => “%CATALINA_HOME%\bin\startup.bat”

    Now the server is open, we can type this in the browser to open the web page 
    => “http://localhost:8080/Project/WebPage.html ”

    To shutdown, run the following command => 
    %CATALINA_HOME%\bin\shutdown.bat”

TestQuery:
    To run the query testing without using the web page, we first need to compile using this command 
    => “javac -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;" classes/IRUtilities/Porter.java classes/my_package/StopStem.java classes/my_package/Indexer.java classes/my_package/Crawler.java classes/my_package/TestProgram.java classes/my_package/SearchEngine.java classes/my_package/TestProgramQuery.java”

    After compiling, we can run the query test program with this command 
    => “java -cp "./lib/jdbm-1.0.jar;./lib/htmlparser.jar;./classes" my_package.TestProgramQuery”
                                                                                                                                                                                             