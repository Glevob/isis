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

    // Добавлен параметр alpha для выбора уровня значимости
    public AnovaResultDto performAnovaAnalysis(double alpha) {
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

        // Общая средняя = среднее групповых средних
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
                        groupMeans.get(e.getKey()) - grandMean, 2))
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

        // Post-hoc анализ с учетом выбранного alpha
        List<GroupComparisonDto> comparisons = performTukeyHSD(
                new ArrayList<>(methodsWithGrades.values()),
                methodNames,
                alpha);

        // Вычисление критического значения F с учетом выбранного alpha
        FDistribution fDist = new FDistribution(betweenGroupDf, withinGroupDf);
        double fCritical = fDist.inverseCumulativeProbability(1 - alpha);

        return new AnovaResultDto(
                fValue, pValue, pValue < alpha,
                grandMean, totalSumOfSquares,
                betweenGroupSumOfSquares, withinGroupSumOfSquares,
                betweenGroupDf, withinGroupDf, totalDf,
                betweenGroupMeanSquare, withinGroupMeanSquare,
                comparisons,
                fCritical, alpha
        );
    }

    // Перегруженный метод для использования alpha по умолчанию (0.05)
    public AnovaResultDto performAnovaAnalysis() {
        return performAnovaAnalysis(0.05);
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

    // Добавлен параметр alpha в метод Tukey HSD
    private List<GroupComparisonDto> performTukeyHSD(List<List<Double>> samples,
                                                     List<String> groupNames,
                                                     double alpha) {
        // Вычисляем необходимые параметры для теста Тьюки
        double mse = calculateMSE(samples); // Средний квадрат ошибок внутри групп
        int df = calculateDegreesOfFreedom(samples); // Степени свободы
        int k = samples.size(); // Количество групп

        List<GroupComparisonDto> comparisons = new ArrayList<>();
        double qCritical = getQCriticalValue(k, df, alpha); // Критическое значение q с учетом alpha

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
                double se = Math.sqrt(mse * 0.5 * (1.0/samples.get(i).size() + 1.0/samples.get(j).size()));

                // Вычисляем q-значение
                double qValue = diff / se;

                // Определяем значимость
                boolean significant = qValue > qCritical;

                comparisons.add(new GroupComparisonDto(
                        firstGroup,
                        secondGroup,
                        se,
                        qValue,
                        significant,
                        qCritical
                ));
            }
        }

        return comparisons;
    }

    // Модифицированный метод для получения критического значения q с учетом alpha
    private double getQCriticalValue(int k, int df, double alpha) {
        // Таблица критических значений q Тьюки для разных alpha
        // Реализация для alpha=0.05 и alpha=0.01

        if (alpha == 0.05) {
            return getQCriticalValueForAlpha05(k, df);
        } else if (alpha == 0.01) {
            return getQCriticalValueForAlpha01(k, df);
        } else {
            // Интерполяция или использование приближенного значения
            // В реальном приложении следует использовать более точные таблицы
            double q05 = getQCriticalValueForAlpha05(k, df);
            double q01 = getQCriticalValueForAlpha01(k, df);

            // Линейная интерполяция между 0.05 и 0.01
            if (alpha < 0.05 && alpha > 0.01) {
                return q05 + (alpha - 0.05) * (q01 - q05) / (0.01 - 0.05);
            }
            // Экстраполяция для других значений (менее точная)
            else if (alpha > 0.05) {
                return q05 * (0.05 / alpha);
            } else {
                return q01 * (0.01 / alpha);
            }
        }
    }

    // Таблица для alpha=0.05
    private double getQCriticalValueForAlpha05(int k, int df) {
        Map<Integer, Map<Integer, Double>> qTable = new HashMap<>();

        // Заполняем таблицу значений для alpha=0.05
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

    // Таблица для alpha=0.01
    private double getQCriticalValueForAlpha01(int k, int df) {
        Map<Integer, Map<Integer, Double>> qTable = new HashMap<>();

        // Заполняем таблицу значений для alpha=0.01
        Map<Integer, Double> df2 = new HashMap<>();
        df2.put(10, 5.27); df2.put(12, 5.05); df2.put(14, 4.89);
        df2.put(16, 4.77); df2.put(18, 4.67); df2.put(20, 4.60);
        df2.put(24, 4.48); df2.put(30, 4.37); df2.put(40, 4.25);
        df2.put(60, 4.13); df2.put(120, 4.02); df2.put(Integer.MAX_VALUE, 3.89);
        qTable.put(2, df2);

        Map<Integer, Double> df3 = new HashMap<>();
        df3.put(10, 5.77); df3.put(12, 5.47); df3.put(14, 5.25);
        df3.put(16, 5.09); df3.put(18, 4.96); df3.put(20, 4.86);
        df3.put(24, 4.70); df3.put(30, 4.55); df3.put(40, 4.39);
        df3.put(60, 4.24); df3.put(120, 4.10); df3.put(Integer.MAX_VALUE, 3.96);
        qTable.put(3, df3);

        Map<Integer, Double> df4 = new HashMap<>();
        df4.put(10, 6.09); df4.put(12, 5.74); df4.put(14, 5.49);
        df4.put(16, 5.30); df4.put(18, 5.15); df4.put(20, 5.03);
        df4.put(24, 4.85); df4.put(30, 4.68); df4.put(40, 4.49);
        df4.put(60, 4.32); df4.put(120, 4.16); df4.put(Integer.MAX_VALUE, 4.00);
        qTable.put(4, df4);

        // Получаем значения для заданного числа групп
        Map<Integer, Double> groupValues = qTable.get(k);
        if (groupValues == null) {
            // Линейная интерполяция для k > 4 (приблизительно)
            return 3.89 + (0.5 / Math.sqrt(k));
        }

        // Находим ближайшее значение df
        int closestDf = groupValues.keySet().stream()
                .min(Comparator.comparingInt(d -> Math.abs(d - df)))
                .orElse(Integer.MAX_VALUE);

        return groupValues.get(closestDf);
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

        return sum / (totalSize - samples.size());
    }

    private int calculateDegreesOfFreedom(List<List<Double>> samples) {
        int totalSize = samples.stream().mapToInt(List::size).sum();
        return totalSize - samples.size();
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
        List<GroupComparisonDto> comparisons = result.getGroupComparisons().stream()
                .sorted(Comparator.comparing(comp -> comp.getGroup1() + "-" + comp.getGroup2()))
                .collect(Collectors.toList());

        List<String> comparisonLabels = comparisons.stream()
                .map(comp -> comp.getGroup1() + "-" + comp.getGroup2())
                .collect(Collectors.toList());

        List<Double> standardErrors = comparisons.stream()
                .map(GroupComparisonDto::getStandardError)
                .collect(Collectors.toList());

        List<Double> criticalValues = comparisons.stream()
                .map(comp -> comp.getCriticalValue() * comp.getStandardError())
                .collect(Collectors.toList());

        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Сравнение методов обучения (Tukey HSD)")
                .xAxisTitle("Парные сравнения")
                .yAxisTitle("Стандартная ошибка (SE)")
                .build();

        chart.addSeries("Critical SE", comparisonLabels, criticalValues)
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar);

        chart.addSeries("Standard Error", comparisonLabels, standardErrors)
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar);

        chart.getStyler().setSeriesColors(new Color[]{
                new Color(220, 20, 60, 100),
                new Color(70, 130, 180)
        });

        chart.addSeries("Zero line",
                        Arrays.asList(comparisonLabels.get(0), comparisonLabels.get(comparisonLabels.size()-1)),
                        Arrays.asList(0.0, 0.0))
                .setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.PNG);
        return outputStream.toByteArray();
    }
}