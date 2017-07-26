import java.util.Comparator;

/**
 * Sorts a list of submissions, on the basis of score,
 * time and memory
 */
public class SortSubmissions implements Comparator<Submission> {
    public int compare(Submission submission1, Submission submission2) {
        double score1 = submission1.getScore();
        double score2 = submission2.getScore();
        if (score1 == score2) {
            double time1 = submission1.getTime();
            double time2 = submission2.getTime();
            if (time1 == time2) {
                double memory1 = submission1.getMemory();
                double memory2 = submission2.getMemory();
                return memory1 < memory2 ? -1 : 1;
            } else {
                return time1 < time2 ? -1 : 1;
            }
        } else {
            return score1 < score2 ? 1 : -1;
        }
    }
}
