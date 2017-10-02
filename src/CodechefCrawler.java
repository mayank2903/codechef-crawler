import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private List<Contest> contestList = new ArrayList<>();

    private String username;
    private HttpURLConnection conn;

    public CodechefCrawler(String username) {
        this.username = username;
        this.conn = null;
    }

    /**
     * The crawling method. This performs all the required tasks:
     * 1. Find the problems which are solved by the user.
     * 2. Find the submission ID for an AC solution of each solved problem.
     * 3. Finally, fetch the solutions from the submission IDs.
     *
     * @throws Exception
     */
    void crawl() throws Exception {
        findSolvedQs();
        fetchSubmissionIDs();
        fetchSolutions();
    }

    /**
     * Finds the problems which have been solved by the user on CodeChef.
     *
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
                Contest contestObject = new Contest();
                System.out.println("Contest Type :" + typeOfContest);
                contestObject.setContestName(typeOfContest);
                Elements problems = contest.select("a");
                for (Element problem : problems) {
                    Problem problemObject = new Problem();
                    System.out.println("Solved:" + problem.text());
                    problemObject.setProblemCode(problem.text());
                    problemObject.setProblemURL(problem.attr("href"));
                    contestObject.getProblemList().add(problemObject);
                }
                contestList.add(contestObject);
                break;
            }
            System.out.println("-----------------------------");
        }
        System.out.println("\n");
    }

    private int getNumberOfPages(Elements pageInfo) {
        int noOfPages;
        if (pageInfo.text().equals("")) {
            noOfPages = 1;
        } else {
            noOfPages = Integer.parseInt(
                    pageInfo
                            .select("tbody")
                            .select("tr")
                            .select("td[align=center]")
                            .select("div.pageinfo")
                            .text()
                            .split("of")[1].
                            trim()
            );
        }
        return noOfPages;
    }

    /**
     * Fetches the submission IDs for AC solutions of a given user.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private Pair<String, String> getLanguageAndProblemId(String problemCode, String problemURL) throws IOException, InterruptedException {
        System.out.println("Finding submission ID for problem: " + problemCode);
        String statusPageURL = CODECHEF_URL + problemURL;
        Document doc = Jsoup.parse(getPageContent(statusPageURL));

        ArrayList<Submission> listOfACSubmissions = new ArrayList<>();
        // For each page of submission for this problem:
        Elements pageInfo = doc.select("table[align=center]");
        int noOfPages = getNumberOfPages(pageInfo);
        System.out.println(noOfPages);
        for (int i = 0; i < noOfPages; i++) {
            String url = statusPageURL + "?page=" + i;
            doc = Jsoup.parse(getPageContent(url));
            Elements rows = doc.select("tr[class=\\\"kol\\\"]");
            System.out.println("---------------------------------");
            for (Element row : rows) {
                // For each row, there are 8 fields.
                Elements fields = row.select("td");
                if (fields.toString().contains("No Recent Activity")) {
                    System.out.println("This problem has hidden solution or no solution. Skipping...");
                    break;
                }
                // First check if this row is AC.
                if (fields.get(3).toString().contains("tick-icon.gif")) {
                    Submission submission = new Submission();
                    String str = fields.get(3).toString();
                    System.out.println(str);
                    Pattern pattern = Pattern.compile("\\[.*pts]");
                    Matcher matcher = pattern.matcher(str);
                    String points = "";
                    if (matcher.find())
                        points = matcher.group(0);
                    if (!points.equals("")) {
                        points = points.substring(1);
                        points = points.substring(0, points.length() - 4).trim();
                        submission.setScore(Double.parseDouble(points));
                    } else {
                        submission.setScore(100.0);
                    }
                    submission.setSubmissionId(Integer.parseInt(fields.get(0).text().trim()));
                    submission.setLanguage(fields.get(6).text());
                    String memory = fields.get(5).text().trim();
                    submission.setMemory(Double.parseDouble(memory.substring(0, memory.length() - 1)));
                    submission.setTime(Double.parseDouble(fields.get(4).text().trim()));
                    listOfACSubmissions.add(submission);
                }
            }
        }
        Collections.sort(listOfACSubmissions, new SortSubmissions());
        return new Pair<>(listOfACSubmissions.get(0).getSubmissionId() + "", listOfACSubmissions.get(0).getLanguage());
    }

    private void fetchSubmissionIDs() throws IOException, InterruptedException {

        for (Contest contest : contestList) {
            List<Problem> problemList = contest.getProblemList();
            for (Problem problem : problemList) {
                String problemCode = problem.getProblemCode();
                String problemURL = problem.getProblemURL();
                Pair<String, String> pair = getLanguageAndProblemId(problemCode, problemURL);
                problem.setProblemId(pair.getKey());
                problem.setLanguage(pair.getValue());
            }
        }
    }

    /**
     * Fetches the problem solutions submitted by the user to CodeChef; one AC solution for each problem.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void fetchSolutions() throws IOException, InterruptedException {
        String offset = CODECHEF_URL + "/viewplaintext/";
        System.out.println("Fetching codes:");
        System.out.println("================================");

        for (Contest contest : contestList) {
            String contestName = contest.getContestName();
            String filePath = CODECHEF_DIR + FILE_SEPARATOR + username + FILE_SEPARATOR
                    + contestName.substring(0, contestName.length() - 1);
            System.out.println(filePath);
            File file = new File(filePath);
            if (file != null) {
                file.mkdirs();
            }
            List<Problem> problemList = contest.getProblemList();
            for (Problem problem : problemList) {
                String url = offset + problem.getProblemId();
                String problemCode = problem.getProblemCode();
                String fileName = problemCode + findExtension(problem.getLanguage());
                String pageContent = getPageContent(url);
                System.out.println(fileName);
                if (pageContent == null) {
                    System.out.println("Unable to fetch solution for problem " + problemCode + ".");
                    System.out.print(problemCode + " seems to be a private contest problem. " +
                            "It can only be fetched with user credentials. Skipping...");
                    continue;
                }
                String innerFilePath = filePath + FILE_SEPARATOR + fileName;
                file = new File(innerFilePath);
                File parentDirectory = file.getParentFile();
                if (parentDirectory != null) {
                    parentDirectory.mkdirs();
                }

                String code = Jsoup.parse(pageContent).select("pre").text();
                FileWriter fileWriter = new FileWriter(file);
                System.out.println("path=" + filePath);
                System.out.println("Completed : ");
                System.out.println();

                StringTokenizer tokenizer = new StringTokenizer(code, "\n");
                while (tokenizer.hasMoreTokens()) {
                    fileWriter.write(tokenizer.nextToken() + "\n");
                }
                fileWriter.close();
            }
        }
    }

    /**
     * Fetches source code of the webpage whose URL is provided.
     *
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
            } catch (IOException e) {
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
     *
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