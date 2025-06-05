package kyrs.isis3.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupComparisonDto {
    private String group1;
    private String group2;
    private double standardError;
    private double qValue;
    private boolean significant;
    private double criticalValue;
    private double expectedProbability; // Ожидаемая вероятность (1 - alpha)
    private double observedProbability;   // Наблюдаемая вероятность (1 - p-value)

    public GroupComparisonDto(String group1, String group2, double standardError,
                              double qValue, boolean significant, double criticalValue,
                              double expectedProbability, double observedProbability) {
        this.group1 = group1;
        this.group2 = group2;
        this.standardError = standardError;
        this.qValue = qValue;
        this.significant = significant;
        this.criticalValue = criticalValue;
        this.expectedProbability = expectedProbability;
        this.observedProbability = observedProbability;
    }

    public GroupComparisonDto() {}
}
