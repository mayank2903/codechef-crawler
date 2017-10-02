import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Created by Mayank Bhura on 5/2/15.
 */

// TODO: Save all print statements in the form of logs.

/**
 * Web crawler for codechef.com.
 * Crawls www.codechef.com for AC solutions by a given user, and downloads them to the user's home directory.
 */
public class CodechefCrawler {
    private final String FILE_SEPARATOR = System.getProperty("file.separator");
    private final String CODECHEF_DIR = System.getProperty("user.home") + FILE_SEPARATOR + "CodechefCodes";
    private final String CODECHEF_URL = "https://www.codechef.com";
    private final int MAX_RETRIES = 5;

    private String username;
    private List<String> problemList;
    private List<String> problemURLs;
    private List<String> problemIDs;
    private List<String> languages;
    private HttpURLConnection conn;

    public CodechefCrawler(String username) {
        this.username = username;
        this.conn = null;
        this.problemList = new ArrayList<>();
        this.problemURLs = new ArrayList<>();
        this.problemIDs = new ArrayList<>();
        this.languages = new ArrayList<>();
    }

    /**
     * The crawling method. This performs all the required tasks:
     * 1. Find the problems which are solved by the user.
     * 2. Find the submission ID for an AC solution of each solved problem.
     * 3. Finally, fetch the solutions from the submission IDs.
     * @throws Exception
     */
    void crawl() throws Exception {
        findSolvedQs();
        fetchSubmissionIDs();
        fetchSolutions();
    }

    /**
     * Finds the problems which have been solved by the user on CodeChef.
     * @throws IOException
     * @throws InterruptedException
     */
    private void findSolvedQs() throws IOException, InterruptedException {
        // Go to user page.
        String content = null;
        content = getPageContent(CODECHEF_URL + "/users/" + username);
        if (content == null) {
            return;
        }
        Document doc = Jsoup.parse(content);

        Elements cells = doc.select("p");
        for (int i = 0; i < cells.size(); i++) {
            Element temp = cells.get(i);
            Elements contests = temp.getAllElements();
            for (Element contest : contests) {
                String typeOfContest = contest.select("strong").text();
                if (typeOfContest.equals("")) {
                    return;
                }
                System.out.println("Contest Type :" + typeOfContest);
                Elements problems = contest.select("a");
                for (Element problem : problems) {
                    System.out.println("Solved:" + problem.text());
                    problemList.add(problem.text());
                    problemURLs.add(problem.attr("href"));
                }
                break;
            }
            System.out.println("-----------------------------");
        }
        System.out.println("\n");
    }

    /**
     * Fetches the submission IDs for AC solutions of a given user.
     * @throws IOException
     * @throws InterruptedException
     */
    private void fetchSubmissionIDs() throws IOException, InterruptedException {
        int tries;
        String url;
        boolean found;
        boolean isHidden;

        // Traverse through the problem list.
        for (int i = 0; i < problemList.size(); i++) {
            // For each problem:
            tries = 0;
            found = false;
            isHidden = false;

            System.out.println("Finding submission ID for problem: " + problemList.get(i));
            while (!found && !isHidden) {
                url = CODECHEF_URL + problemURLs.get(i) + "?page=" + Integer.toString(tries);
                Document doc = Jsoup.parse(getPageContent(url));

                // For each page of submission for this problem:
                Elements rows = doc.select("tr[class=\\\"kol\\\"]");
                for (Element row : rows) {
                    // For each row, there are 8 fields.
                    Elements fields = row.select("td");
                    if (fields.toString().contains("No Recent Activity")) {
                        System.out.println("This problem has hidden solution or no solution. Skipping...");
                        isHidden = true;
                        break;
                    }

                    // First check if this row is AC.
                    if (fields.get(3).toString().contains("tick-icon.gif")) {
                        problemIDs.add(fields.get(0).text());
                        languages.add(fields.get(6).text());
                        System.out.println("Found!");
                        found = true;
                        break;
                    }
                }

                tries++;
            }
            System.out.println("Completed " + (i + 1) + "/" + problemList.size());
            System.out.println();
        }
    }

    /**
     * Fetches the problem solutions submitted by the user to CodeChef; one AC solution for each problem.
     * @throws IOException
     * @throws InterruptedException
     */
    private void fetchSolutions() throws IOException, InterruptedException {
        String offset = CODECHEF_URL + "/viewplaintext/";
        System.out.println("Fetching codes:");
        System.out.println("================================");

        for (int i = 0; i < problemIDs.size(); i++) {
            String url = offset + problemIDs.get(i);
            String filename = problemList.get(i) + findExtension(languages.get(i));
            String pageContent = getPageContent(url);
            if (pageContent == null) {
                // Private problem situation. Skipping...
                System.out.println("Unable to fetch solution for problem " + problemList.get(i) + ".");
                System.out.print(problemList.get(i) + " seems to be a private contest problem. " +
                        "It can only be fetched with user credentials. Skipping...");
                continue;
            }
            String code = Jsoup.parse(pageContent).select("pre").text();

            String filePath = CODECHEF_DIR + FILE_SEPARATOR + username + FILE_SEPARATOR + filename;
            File file = new File(filePath);
            File parentDirectory = file.getParentFile();
            if (parentDirectory != null) {
                parentDirectory.mkdirs();
            }

            FileWriter fileWriter = new FileWriter(file);
            System.out.println("path=" + filePath);
            System.out.println("Completed : " + (i + 1) + "/" + problemIDs.size());
            System.out.println();

            StringTokenizer tokenizer = new StringTokenizer(code, "\n");
            while (tokenizer.hasMoreTokens()) {
                fileWriter.write(tokenizer.nextToken() + "\n");
            }
            fileWriter.close();
        }
    }

    /**
     * Fetches source code of the webpage whose URL is provided.
     * @param url Link to the webpage whose source page is to be fetched.
     * @return The webpage response for the given URL.
     * @throws IOException
     * @throws InterruptedException
     */
    private String getPageContent(String url) throws IOException, InterruptedException {
        int sleepTime = 100;
        int responseCode = 0;
        int numRetries = 0;

        do {
            URL obj = new URL(url);
            conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);

            Thread.sleep(sleepTime);
            System.out.println("Sending GET request to URL: " + url);
            try {
                responseCode = conn.getResponseCode();
            } catch(IOException e) {
                System.out.println("Oops! Looks like Codechef is hanging again :-).\n Please re-try the command.");
                return null;
            }
            System.out.println("Response code:" + responseCode);

            if (responseCode == 403) {
                // Permission denied. Private solution, cannot be fetched.
                return null;
            } else if (responseCode != 200) {
                System.out.println("Response code is not 'OK'. Re-trying...");
                sleepTime += 1000;
            }

        } while (responseCode != 200 && numRetries++ < MAX_RETRIES);

        if (responseCode != 200) {
            return null;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        String response = new String();
        while ((inputLine = in.readLine()) != null) {
            if (url.contains("viewplaintext")) {
                response += inputLine + "\n";
            } else {
                response += inputLine;
            }
        }
        in.close();

        return response;
    }

    /**
     * Finds the appropriate file extension for a given input language.
     * @param lang The programming language whose file extension is to be fetched.
     * @return Required file extension.
     */
    private String findExtension(String lang) {
        if (lang.contains("JAVA"))
            return ".java";
        else if (lang.contains("C++"))
            return ".cpp";
        else if (lang.contains("C"))
            return ".c";
        else if (lang.contains("PY"))
            return ".py";
        else
            return ".txt";
    }
}