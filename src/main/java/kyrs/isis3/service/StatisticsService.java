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
        // Получаем данные и сортируем методы по имени
        Map<TeachingMethod, List<Double>> methodsWithGrades = getTeachingMethodsWithGrades();
        List<String> methodNames = methodsWithGrades.keySet().stream()
                .map(TeachingMethod::getNameMethod)
                .sorted()
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

        // Post-hoc анализ с передачей всех необходимых параметров
        List<GroupComparisonDto> comparisons = performTukeyHSD(
                new ArrayList<>(methodsWithGrades.values()),
                methodNames,
                withinGroupMeanSquare,
                withinGroupDf);

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

    private List<GroupComparisonDto> performTukeyHSD(List<List<Double>> samples,
                                                     List<String> groupNames,
                                                     double mse,
                                                     int df) {
        List<GroupComparisonDto> comparisons = new ArrayList<>();

        // Упрощенное критическое значение для Tukey HSD
        // В реальном приложении используйте библиотеку или точные расчеты
        double qCritical = 3.67; // Примерное значение для α=0.05 и 3 групп

        // Гарантируем порядок сравнений: 1-2, 1-3, 2-3
        int[][] comparisonPairs = {{0, 1}, {0, 2}, {1, 2}};

        for (int[] pair : comparisonPairs) {
            int i = pair[0];
            int j = pair[1];

            if (i < samples.size() && j < samples.size()) {
                double mean1 = calculateMean(samples.get(i));
                double mean2 = calculateMean(samples.get(j));
                double diff = mean1 - mean2;
                double se = Math.sqrt(mse * (1.0/samples.get(i).size() + 1.0/samples.get(j).size()));
                double criticalValue = qCritical * se;

                // Упрощенный расчет p-value
                double pValue = Math.abs(diff) > criticalValue ? 0.01 : 0.05;

                comparisons.add(new GroupComparisonDto(
                        groupNames.get(i),
                        groupNames.get(j),
                        diff,
                        pValue,
                        pValue < 0.05,
                        criticalValue
                ));
            }
        }

        return comparisons;
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
        // Убедимся, что сравнения упорядочены как 1-2, 1-3, 2-3
        List<GroupComparisonDto> orderedComparisons = result.getGroupComparisons().stream()
                .sorted(Comparator.comparing(comp -> comp.getGroup1() + "-" + comp.getGroup2()))
                .collect(Collectors.toList());

        // Подготовка данных для графика
        List<String> comparisonLabels = orderedComparisons.stream()
                .map(comp -> comp.getGroup1() + "-" + comp.getGroup2())
                .collect(Collectors.toList());

        List<Double> differences = orderedComparisons.stream()
                .map(GroupComparisonDto::getMeanDifference)
                .collect(Collectors.toList());

        double criticalValue = orderedComparisons.get(0).getCriticalValue();

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
