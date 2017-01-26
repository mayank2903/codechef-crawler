# codechef-crawler
A web crawler that can download all successful submissions of a user in Codechef. Just provide the username.
Any feedback is welcome! 

Usage Instructions:
===================
1. Download CodechefCrawler.jar from the repository.
2. $ java -jar CodechefCrawler.jar \<username\>

Example:
=========
$ java -jar CodechefCrawler.jar mb1994

Features:
==========
1. Retries fetching code, if it fails (upto 5 times).
2. Fetches a single AC solution for each of the problems.
3. Fetches the latest AC solutions for all problems.
4. It times the entire process, to give exact runtime of the crawling performed.
5. Provides clear output statements to keep the user aware about progress being made, while downloading solutions.
6. Supports downloading JAVA, CPP, C and PYTHON codes, since these are the most used.
   Rest are downloaded as txt files. Support for more languages coming very soon.
