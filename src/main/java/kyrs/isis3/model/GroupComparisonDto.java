package kyrs.isis3.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupComparisonDto {
    private String group1;
    private String group2;
    private double meanDifference;
    private double pValue;
    private boolean isSignificant;
    private double criticalValue;

    public GroupComparisonDto(String group1, String group2, double meanDifference,
                              double pValue, boolean isSignificant, double criticalValue) {
        this.group1 = group1;
        this.group2 = group2;
        this.meanDifference = meanDifference;
        this.pValue = pValue;
        this.isSignificant = isSignificant;
        this.criticalValue = criticalValue;
    }

    public GroupComparisonDto() {}
}
