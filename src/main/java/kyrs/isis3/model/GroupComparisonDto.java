package kyrs.isis3.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupComparisonDto {
    private String group1;
    private String group2;
    private double standardError; // было meanDifference
    private double qValue; // было pValue
    private boolean significant;
    private double criticalValue; // теперь это критическое q-значение

//    public GroupComparisonDto(String group1, String group2, double meanDifference, double pValue, boolean isSignificant, double criticalValue) {
//        this.group1 = group1;
//        this.group2 = group2;
//        this.meanDifference = meanDifference;
//        this.pValue = pValue;
//        this.isSignificant = isSignificant;
//        this.criticalValue = criticalValue;
//    }


    public GroupComparisonDto(String group1, String group2, double standardError, double qValue, boolean significant, double criticalValue) {
        this.group1 = group1;
        this.group2 = group2;
        this.standardError = standardError;
        this.qValue = qValue;
        this.significant = significant;
        this.criticalValue = criticalValue;
    }

    public GroupComparisonDto() {}
    // getters, setters, constructor
}
