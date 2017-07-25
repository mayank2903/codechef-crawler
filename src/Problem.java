public class Problem {

    private String problemId;
    private String problemURL;
    private String language;
    private String problemCode;

    public String getProblemCode() {
        return problemCode;
    }

    public void setProblemCode(String problemCode) {
        this.problemCode = problemCode;
    }

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getProblemURL() {
        return problemURL;
    }

    public void setProblemURL(String problemURL) {
        this.problemURL = problemURL;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
