package kyrs.isis3.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MethodStats {
    private long count;
    private double mean;
    private double stdDev;
    private double stdError;

    public MethodStats(long count, double mean, double stdDev, double stdError) {
        this.count = count;
        this.mean = mean;
        this.stdDev = stdDev;
        this.stdError = stdError;
    }

    public MethodStats() {}
    // constructor, getters, setters
}
