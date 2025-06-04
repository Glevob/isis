package kyrs.isis3.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnovaResultDto {
    private double fValue;
    private double pValue;
    private boolean significant;
    private double grandMean;
    private double totalSumOfSquares;
    private double betweenGroupSumOfSquares;
    private double withinGroupSumOfSquares;
    private int betweenGroupDf;
    private int withinGroupDf;
    private int totalDf;
    private double betweenGroupMeanSquare;
    private double withinGroupMeanSquare;
    private List<GroupComparisonDto> groupComparisons;
    private double fCritical;  // Критическое значение F-распределения
    private double alpha;      // Уровень значимости

    public AnovaResultDto(double fValue, double pValue, boolean significant, List<GroupComparisonDto> groupComparisons) {
        this.fValue = fValue;
        this.pValue = pValue;
        this.significant = significant;
        this.groupComparisons = groupComparisons;
    }

    public AnovaResultDto() {}



    public AnovaResultDto(double fValue, double pValue, boolean significant, double grandMean,
                          double totalSumOfSquares, double betweenGroupSumOfSquares,
                          double withinGroupSumOfSquares, int betweenGroupDf, int withinGroupDf,
                          int totalDf, double betweenGroupMeanSquare, double withinGroupMeanSquare,
                          List<GroupComparisonDto> comparisons, double fCritical, double alpha) {
        this.fValue = fValue;
        this.pValue = pValue;
        this.significant = significant;
        this.grandMean = grandMean;
        this.totalSumOfSquares = totalSumOfSquares;
        this.betweenGroupSumOfSquares = betweenGroupSumOfSquares;
        this.withinGroupSumOfSquares = withinGroupSumOfSquares;
        this.betweenGroupDf = betweenGroupDf;
        this.withinGroupDf = withinGroupDf;
        this.totalDf = totalDf;
        this.betweenGroupMeanSquare = betweenGroupMeanSquare;
        this.withinGroupMeanSquare = withinGroupMeanSquare;
        this.groupComparisons = comparisons;
        this.fCritical = fCritical;
        this.alpha = alpha;

    }
    // getters, setters, constructor
}


