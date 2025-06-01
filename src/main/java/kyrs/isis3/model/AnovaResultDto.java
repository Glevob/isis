package kyrs.isis3.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnovaResultDto {
    private double fValue;
    private double pValue;
    private boolean isSignificant;
    private List<GroupComparisonDto> groupComparisons;

    public AnovaResultDto(double fValue, double pValue, boolean isSignificant, List<GroupComparisonDto> groupComparisons) {
        this.fValue = fValue;
        this.pValue = pValue;
        this.isSignificant = isSignificant;
        this.groupComparisons = groupComparisons;
    }

    public AnovaResultDto() {}
    // getters, setters, constructor
}


