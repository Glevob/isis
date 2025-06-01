package kyrs.isis3.service;

import kyrs.isis3.model.*;
import kyrs.isis3.repository.GradeRepository;
import kyrs.isis3.repository.StudentGroupRepository;
import kyrs.isis3.repository.StudentRepository;
import kyrs.isis3.repository.TeachingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final TeachingMethodRepository teachingMethodRepository;

    public AnovaResultDto performAnovaAnalysis() {
        Map<TeachingMethod, List<Double>> methodsWithGrades = getTeachingMethodsWithGrades();
        List<String> methodNames = methodsWithGrades.keySet().stream()
                .map(TeachingMethod::getNameMethod)
                .collect(Collectors.toList());

        // Вычисление общей средней
        double grandMean = methodsWithGrades.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        // Вычисление общей суммы квадратов (SST)
        double totalSumOfSquares = methodsWithGrades.values().stream()
                .flatMap(List::stream)
                .mapToDouble(score -> Math.pow(score - grandMean, 2))
                .sum();

        // Вычисление суммы квадратов между группами (SSB)
        double betweenGroupSumOfSquares = methodsWithGrades.entrySet().stream()
                .mapToDouble(e -> e.getValue().size() * Math.pow(
                        e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) - grandMean, 2))
                .sum();

        // Вычисление суммы квадратов внутри групп (SSW)
        double withinGroupSumOfSquares = methodsWithGrades.values().stream()
                .mapToDouble(groupScores -> {
                    double groupMean = groupScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    return groupScores.stream()
                            .mapToDouble(score -> Math.pow(score - groupMean, 2))
                            .sum();
                })
                .sum();

        // Степени свободы
        int totalDf = (int) methodsWithGrades.values().stream().mapToLong(List::size).sum() - 1;
        int betweenGroupDf = methodsWithGrades.size() - 1;
        int withinGroupDf = totalDf - betweenGroupDf;

        // Средние квадраты
        double betweenGroupMeanSquare = betweenGroupSumOfSquares / betweenGroupDf;
        double withinGroupMeanSquare = withinGroupSumOfSquares / withinGroupDf;

        // F-статистика
        double fValue = betweenGroupMeanSquare / withinGroupMeanSquare;

        // Вычисление p-value
        double pValue = 1 - new FDistribution(betweenGroupDf, withinGroupDf).cumulativeProbability(fValue);

        // Post-hoc анализ
        List<GroupComparisonDto> comparisons = performTukeyHSD(
                new ArrayList<>(methodsWithGrades.values()),
                methodNames);

        return new AnovaResultDto(
                fValue, pValue, pValue < 0.05,
                grandMean, totalSumOfSquares,
                betweenGroupSumOfSquares, withinGroupSumOfSquares,
                betweenGroupDf, withinGroupDf, totalDf,
                betweenGroupMeanSquare, withinGroupMeanSquare,
                comparisons);
    }

    private Map<TeachingMethod, List<Double>> getTeachingMethodsWithGrades() {
        List<TeachingMethod> teachingMethods = teachingMethodRepository.findAll();
        Map<TeachingMethod, List<Double>> result = new HashMap<>();

        teachingMethods.forEach(method -> {
            List<Double> grades = gradeRepository.findByTeachingMethodId(method.getIdTeachingMethod())
                    .stream()
                    .map(grade -> Double.parseDouble(grade.getValueScore()))
                    .collect(Collectors.toList());
            result.put(method, grades);
        });

        return result;
    }

    public Map<String, MethodStats> calculateMethodStatistics() {
        Map<TeachingMethod, List<Double>> methodsWithGrades = getTeachingMethodsWithGrades();

        return methodsWithGrades.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getNameMethod(),
                        e -> {
                            List<Double> grades = e.getValue();
                            long count = grades.size();
                            double mean = calculateMean(grades);
                            double stdDev = calculateStdDev(grades, mean);
                            double stdError = stdDev / Math.sqrt(count);

                            return new MethodStats(count, mean, stdDev, stdError);
                        }
                ));
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    private Map<StudentGroup, List<Double>> getStudentGroupsWithGrades() {
        List<StudentGroup> studentGroups = studentGroupRepository.findAll();
        Map<StudentGroup, List<Double>> result = new HashMap<>();

        studentGroups.forEach(studentGroup -> {
            List<Double> grades = gradeRepository.findByStudentStudentGroupIdStudentGroup(studentGroup.getIdStudentGroup())
                    .stream()
                    .map(grade -> Double.parseDouble(grade.getValueScore()))
                    .collect(Collectors.toList());
            result.put(studentGroup, grades);
        });

        return result;
    }

    private List<GroupComparisonDto> performTukeyHSD(List<List<Double>> samples, List<String> methodNames) {
        List<GroupComparisonDto> comparisons = new ArrayList<>();

        for (int i = 0; i < samples.size(); i++) {
            for (int j = i + 1; j < samples.size(); j++) {
                double mean1 = calculateMean(samples.get(i));
                double mean2 = calculateMean(samples.get(j));
                double diff = mean1 - mean2;

                // Simplified p-value calculation (replace with actual Tukey HSD implementation)
                double pValue = Math.abs(diff) > 5 ? 0.01 : 0.05;

                comparisons.add(new GroupComparisonDto(
                        methodNames.get(i),  // Just use method name directly
                        methodNames.get(j),  // Just use method name directly
                        diff,
                        pValue,
                        pValue < 0.05
                ));
            }
        }

        return comparisons;
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
        // Group comparisons by methods and calculate average differences
        Map<String, Double> methodAverages = result.getGroupComparisons().stream()
                .collect(Collectors.groupingBy(
                        GroupComparisonDto::getGroup1,  // Use method name directly
                        Collectors.averagingDouble(GroupComparisonDto::getMeanDifference)
                ));

        // Sort methods by name
        List<String> methods = methodAverages.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        // Get means in the same order
        List<Double> means = methods.stream()
                .map(methodAverages::get)
                .collect(Collectors.toList());

        // Create chart
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Сравнение средних оценок по методам обучения")
                .xAxisTitle("Методы обучения")
                .yAxisTitle("Средняя оценка")
                .build();

        chart.addSeries("Средние оценки", methods, means);
        chart.getStyler().setXAxisLabelRotation(45);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.PNG);
        return outputStream.toByteArray();
    }
}
