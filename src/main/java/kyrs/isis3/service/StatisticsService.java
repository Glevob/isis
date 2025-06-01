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
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.style.Styler;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
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

    private List<GroupComparisonDto> performTukeyHSD(List<List<Double>> samples,
                                                     List<String> groupNames) {
        // Вычисляем необходимые параметры для теста Тьюки
        double mse = calculateMSE(samples); // Средний квадрат ошибок внутри групп
        int df = calculateDegreesOfFreedom(samples); // Степени свободы

        List<GroupComparisonDto> comparisons = new ArrayList<>();
        double qCritical = getQCriticalValue(samples.size(), df); // Критическое значение q

        // Генерируем все возможные пары для сравнения
        for (int i = 0; i < samples.size(); i++) {
            for (int j = i + 1; j < samples.size(); j++) {
                double mean1 = calculateMean(samples.get(i));
                double mean2 = calculateMean(samples.get(j));

                // Гарантируем положительную разницу (всегда вычитаем меньшее из большего)
                double diff = Math.abs(mean1 - mean2);
                String firstGroup = mean1 > mean2 ? groupNames.get(i) : groupNames.get(j);
                String secondGroup = mean1 > mean2 ? groupNames.get(j) : groupNames.get(i);

                double se = Math.sqrt(mse * (1.0/samples.get(i).size() + 1.0/samples.get(j).size()));
                double criticalValue = qCritical * se;
                boolean significant = diff > criticalValue;
                double pValue = significant ? 0.01 : 0.05; // Упрощенное p-значение

                comparisons.add(new GroupComparisonDto(
                        firstGroup,
                        secondGroup,
                        diff,
                        pValue,
                        significant,
                        criticalValue
                ));
            }
        }

        return comparisons;
    }

    private double calculateMSE(List<List<Double>> samples) {
        double sum = 0;
        int totalSize = 0;

        for (List<Double> sample : samples) {
            double mean = calculateMean(sample);
            for (Double value : sample) {
                sum += Math.pow(value - mean, 2);
            }
            totalSize += sample.size();
        }

        return sum / (totalSize - samples.size()); // SSW / df_within
    }

    private int calculateDegreesOfFreedom(List<List<Double>> samples) {
        int totalSize = samples.stream().mapToInt(List::size).sum();
        return totalSize - samples.size(); // df_within = N - k
    }
    private double getQCriticalValue(int k, int df) {
        // Упрощенная реализация - в реальном проекте используйте таблицы Тьюки или точные вычисления
        if (k == 3) {
            if (df >= 20) return 3.58;
            if (df >= 10) return 3.88;
            return 4.34;
        }
        return 3.0; // Значение по умолчанию
    }










    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
//
//        // Получаем упорядоченные сравнения
//        List<GroupComparisonDto> comparisons = result.getGroupComparisons().stream()
//                .sorted(Comparator.comparing(comp -> comp.getGroup1() + "-" + comp.getGroup2()))
//                .collect(Collectors.toList());
//
//        // Подготавливаем данные для графика
//        List<String> comparisonLabels = Arrays.asList("1-2", "1-3", "2-3");
//        List<Double> differences = comparisons.stream()
//                .map(GroupComparisonDto::getMeanDifference)
//                .collect(Collectors.toList());
//        List<Double> criticalValues = comparisons.stream()
//                .map(GroupComparisonDto::getCriticalValue)
//                .collect(Collectors.toList());
//
//        // Создаем график
//        CategoryChart chart = new CategoryChartBuilder()
//                .width(800)
//                .height(600)
//                .title("Сравнение методов обучения (Tukey HSD)")
//                .xAxisTitle("Парные сравнения")
//                .yAxisTitle("Разница средних")
//                .build();

//        // Получаем упорядоченные сравнения
//        List<GroupComparisonDto> comparisons = result.getGroupComparisons().stream()
//                .sorted(Comparator.comparing(comp -> comp.getGroup1() + "-" + comp.getGroup2()))
//                .collect(Collectors.toList());
//
//        // Подготавливаем данные для графика
//        List<String> comparisonLabels = Arrays.asList("1-2", "1-3", "2-3");
//        List<Double> differences = comparisons.stream()
//                .map(GroupComparisonDto::getMeanDifference)
//                .collect(Collectors.toList());
//        List<Double> criticalValues = comparisons.stream()
//                .map(GroupComparisonDto::getCriticalValue)
//                .collect(Collectors.toList());
//
//        // Создаем график
//        CategoryChart chart = new CategoryChartBuilder()
//                .width(800)
//                .height(600)
//                .title("Сравнение методов обучения (Tukey HSD)")
//                .xAxisTitle("Парные сравнения")
//                .yAxisTitle("Разница средних")
//                .build();

        // Получаем упорядоченные сравнения
        List<GroupComparisonDto> comparisons = result.getGroupComparisons().stream()
                .sorted(Comparator.comparing(comp -> comp.getGroup1() + "-" + comp.getGroup2()))
                .collect(Collectors.toList());

        // Подготавливаем данные для графика
        List<String> comparisonLabels = comparisons.stream()
                .map(comp -> comp.getGroup1() + "-" + comp.getGroup2())
                .collect(Collectors.toList());

        List<Double> differences = comparisons.stream()
                .map(GroupComparisonDto::getMeanDifference)
                .collect(Collectors.toList());

        List<Double> criticalValues = comparisons.stream()
                .map(GroupComparisonDto::getCriticalValue)
                .collect(Collectors.toList());

        // Создаем график
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Сравнение методов обучения (Tukey HSD)")
                .xAxisTitle("Парные сравнения")
                .yAxisTitle("Разница средних")
                .build();

        // Настраиваем стиль графика
        chart.getStyler()
                .setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar)
                .setPlotGridVerticalLinesVisible(false)
                .setLegendPosition(Styler.LegendPosition.InsideNE);

        // Добавляем столбцы с разницами
        chart.addSeries("Difference", comparisonLabels, differences);

        // Добавляем линии критических значений
        chart.addSeries("Critical value", comparisonLabels, criticalValues)
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);

        // Добавляем горизонтальную линию для нуля
        chart.addSeries("Zero line",
                        Arrays.asList("1-2", "2-3"),
                        Arrays.asList(0.0, 0.0))
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);

        // Настраиваем цвета
        chart.getStyler().setSeriesColors(new Color[]{
                new Color(70, 130, 180),  // Difference - steel blue
                new Color(220, 20, 60),    // Critical value - crimson
                new Color(0, 0, 0)         // Zero line - black
        });

        // Сохраняем график в изображение
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.PNG);
        return outputStream.toByteArray();
    }
}
