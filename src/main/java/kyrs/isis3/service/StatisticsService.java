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

//    public AnovaResultDto performAnovaAnalysis() {
//        Map<TeachingMethod, List<Double>> methodsWithGrades = getTeachingMethodsWithGrades();
//        List<String> methodNames = methodsWithGrades.keySet().stream()
//                .map(TeachingMethod::getNameMethod)
//                .collect(Collectors.toList());
//
//        // Вычисление общей средней
//        double grandMean = methodsWithGrades.values().stream() //Преобразуем Map в Stream списков оценок: [[4.0,5.0,6.0], [3.0,4.0,5.0], [2.0,3.0,4.0]]
//                .flatMap(List::stream)                         //"Разворачиваем" вложенные списки в один Stream: [4.0, 5.0, 6.0, 3.0, 4.0, 5.0, 2.0, 3.0, 4.0]
//                .mapToDouble(Double::doubleValue)              //Конвертируем Double в примитивный double: [4.0, 5.0, 6.0, 3.0, 4.0, 5.0, 2.0, 3.0, 4.0]
//                .average()                                     //Вычисляем среднее значение: (4.0 + 5.0 + 6.0 + 3.0 + 4.0 + 5.0 + 2.0 + 3.0 + 4.0) / 9 = 36.0 / 9 = 4.0
//                .orElse(0);                              //Если бы список был пуст, вернули бы 0
//
//        // Вычисление общей суммы квадратов (SST)
//        double totalSumOfSquares = methodsWithGrades.values().stream()
//                .flatMap(List::stream)
//                .mapToDouble(score -> Math.pow(score - grandMean, 2))
//                .sum();
//
//        // Вычисление суммы квадратов между группами (SSB)
//        double betweenGroupSumOfSquares = methodsWithGrades.entrySet().stream()
//                .mapToDouble(e -> e.getValue().size() * Math.pow(
//                        e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) - grandMean, 2))
//                .sum();
//
//        // Вычисление суммы квадратов внутри групп (SSW)
//        double withinGroupSumOfSquares = methodsWithGrades.values().stream()
//                .mapToDouble(groupScores -> {
//                    double groupMean = groupScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
//                    return groupScores.stream()
//                            .mapToDouble(score -> Math.pow(score - groupMean, 2))
//                            .sum();
//                })
//                .sum();
//
//        // Степени свободы
//        int totalDf = (int) methodsWithGrades.values().stream().mapToLong(List::size).sum() - 1;
//        int betweenGroupDf = methodsWithGrades.size() - 1;
//        int withinGroupDf = totalDf - betweenGroupDf;
//
//        // Средние квадраты
//        double betweenGroupMeanSquare = betweenGroupSumOfSquares / betweenGroupDf;
//        double withinGroupMeanSquare = withinGroupSumOfSquares / withinGroupDf;
//
//        // F-статистика
//        double fValue = betweenGroupMeanSquare / withinGroupMeanSquare;
//
//        // Вычисление p-value
//        double pValue = 1 - new FDistribution(betweenGroupDf, withinGroupDf).cumulativeProbability(fValue);
//
//        // Post-hoc анализ
//        List<GroupComparisonDto> comparisons = performTukeyHSD(
//                new ArrayList<>(methodsWithGrades.values()),
//                methodNames);
//
//        return new AnovaResultDto(
//                fValue, pValue, pValue < 0.05,
//                grandMean, totalSumOfSquares,
//                betweenGroupSumOfSquares, withinGroupSumOfSquares,
//                betweenGroupDf, withinGroupDf, totalDf,
//                betweenGroupMeanSquare, withinGroupMeanSquare,
//                comparisons);
//    }

    public AnovaResultDto performAnovaAnalysis() {
        Map<TeachingMethod, List<Double>> methodsWithGrades = getTeachingMethodsWithGrades();
        List<String> methodNames = methodsWithGrades.keySet().stream()
                .map(TeachingMethod::getNameMethod)
                .collect(Collectors.toList());

        // Вычисляем средние для каждой группы
        Map<TeachingMethod, Double> groupMeans = methodsWithGrades.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0)
                ));

        // НОВЫЙ РАСЧЕТ: Общая средняя = среднее групповых средних
        double grandMean = groupMeans.values().stream()
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
                        groupMeans.get(e.getKey()) - grandMean, 2)) // Используем предвычисленные средние
                .sum();

        // Вычисление суммы квадратов внутри групп (SSW)
        double withinGroupSumOfSquares = methodsWithGrades.entrySet().stream()
                .mapToDouble(e -> {
                    double groupMean = groupMeans.get(e.getKey());
                    return e.getValue().stream()
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
        int k = samples.size(); // Количество групп

        List<GroupComparisonDto> comparisons = new ArrayList<>();
        double qCritical = getQCriticalValue(k, df); // Критическое значение q

        // Генерируем все возможные пары для сравнения
        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                double mean1 = calculateMean(samples.get(i));
                double mean2 = calculateMean(samples.get(j));

                // Вычисляем разницу (всегда положительную)
                double diff = Math.abs(mean1 - mean2);
                String firstGroup = mean1 > mean2 ? groupNames.get(i) : groupNames.get(j);
                String secondGroup = mean1 > mean2 ? groupNames.get(j) : groupNames.get(i);

                // Вычисляем стандартную ошибку разницы
                double se = Math.sqrt(mse * (1.0/samples.get(i).size() + 1.0/samples.get(j).size()));

                // Вычисляем критическое значение (HSD)
                double hsd = qCritical * se;

                // Определяем значимость
                boolean significant = diff > hsd;

                // Упрощенное вычисление p-value
                double pValue;
                if (significant) {
                    pValue = 0.001; // Значимое различие
                } else {
                    // Аппроксимация p-value на основе расстояния до критического значения
                    double ratio = diff / hsd;
                    pValue = 1.0 - ratio * 0.8; // Эвристическая формула
                    if (pValue < 0.05) pValue = 0.05;
                }

                comparisons.add(new GroupComparisonDto(
                        firstGroup,
                        secondGroup,
                        diff,
                        pValue,
                        significant,
                        hsd // Критическое значение HSD
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
        // Таблица критических значений q Тьюки для α=0.05
        // k - число групп, df - степени свободы внутри групп
        Map<Integer, Map<Integer, Double>> qTable = new HashMap<>();

        // Заполняем таблицу значений
        Map<Integer, Double> df2 = new HashMap<>();
        df2.put(10, 3.88); df2.put(12, 3.77); df2.put(14, 3.70);
        df2.put(16, 3.65); df2.put(18, 3.61); df2.put(20, 3.58);
        df2.put(24, 3.53); df2.put(30, 3.49); df2.put(40, 3.44);
        df2.put(60, 3.40); df2.put(120, 3.36); df2.put(Integer.MAX_VALUE, 3.31);
        qTable.put(2, df2);

        Map<Integer, Double> df3 = new HashMap<>();
        df3.put(10, 4.34); df3.put(12, 4.17); df3.put(14, 4.05);
        df3.put(16, 3.96); df3.put(18, 3.89); df3.put(20, 3.84);
        df3.put(24, 3.76); df3.put(30, 3.69); df3.put(40, 3.61);
        df3.put(60, 3.54); df3.put(120, 3.47); df3.put(Integer.MAX_VALUE, 3.40);
        qTable.put(3, df3);

        Map<Integer, Double> df4 = new HashMap<>();
        df4.put(10, 4.68); df4.put(12, 4.47); df4.put(14, 4.32);
        df4.put(16, 4.20); df4.put(18, 4.11); df4.put(20, 4.04);
        df4.put(24, 3.94); df4.put(30, 3.85); df4.put(40, 3.74);
        df4.put(60, 3.65); df4.put(120, 3.56); df4.put(Integer.MAX_VALUE, 3.47);
        qTable.put(4, df4);

        // Добавить больше значений если будет надо

        // Получаем значения для заданного числа групп
        Map<Integer, Double> groupValues = qTable.get(k);
        if (groupValues == null) {
            // Линейная интерполяция для k > 4 (приблизительно)
            return 3.31 + (0.4 / Math.sqrt(k));
        }

        // Находим ближайшее значение df
        int closestDf = groupValues.keySet().stream()
                .min(Comparator.comparingInt(d -> Math.abs(d - df)))
                .orElse(Integer.MAX_VALUE);

        return groupValues.get(closestDf);
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
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

        // Добавляем столбцы с критическими значениями (фоном)
        chart.addSeries("Critical value", comparisonLabels, criticalValues)
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar);

        // Добавляем столбцы с разницами (передним планом)
        chart.addSeries("Difference", comparisonLabels, differences)
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar);

        // Настраиваем цвета и прозрачность
        chart.getStyler().setSeriesColors(new Color[]{
                new Color(220, 20, 60, 100),  // Critical value - crimson с прозрачностью
                new Color(70, 130, 180)       // Difference - steel blue
        });

        // Добавляем горизонтальную линию для нуля
        chart.addSeries("Zero line",
                        Arrays.asList(comparisonLabels.get(0), comparisonLabels.get(comparisonLabels.size()-1)),
                        Arrays.asList(0.0, 0.0))
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);

        // Сохраняем график в изображение
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.PNG);
        return outputStream.toByteArray();
    }
}
