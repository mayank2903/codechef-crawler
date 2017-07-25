import java.util.ArrayList;
import java.util.List;

public class Contest {

    private String contestName;
    private List<Problem> problemList = new ArrayList<>();

    public String getContestName() {
        return contestName;
    }

    public List<Problem> getProblemList() {
        return problemList;
    }

    public void setContestName(String contestName) {
        this.contestName = contestName;
    }
}
