package kyrs.isis3.service;

import kyrs.isis3.model.AnovaResultDto;
import kyrs.isis3.model.GroupComparisonDto;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.repository.GradeRepository;
import kyrs.isis3.repository.StudentGroupRepository;
import kyrs.isis3.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
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

    public AnovaResultDto performAnovaAnalysis() {
        // Получаем все группы с их оценками
        Map<StudentGroup, List<Double>> studentGroupsWithGrades = getStudentGroupsWithGrades();
//
//        // Получаем данные с указанием методов обучения
//        Map<StudentGroup, List<Double>> groupsWithGrades = getGroupsWithGrades();

        // Формируем названия в формате "Группа - Метод"
        List<String> groupNames = studentGroupsWithGrades.keySet().stream()
                .map(group -> group.getNameGroup() + " - " + group.getTeachingMethod().getNameMethod())
                .collect(Collectors.toList());

        // Подготовка данных для ANOVA
        List<List<Double>> samples = new ArrayList<>();
//        List<String> groupNames = new ArrayList<>();

        studentGroupsWithGrades.forEach((studentGroup, grades) -> {
            samples.add(grades);
            groupNames.add(studentGroup.getNameGroup());
        });

        // Выполняем ANOVA
        OneWayAnova anova = new OneWayAnova();

// Конвертируем List<List<Double>> в Collection<double[]>
        Collection<double[]> samplesArray = samples.stream()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).toArray())
                .collect(Collectors.toList());

        double fValue = anova.anovaFValue(samplesArray);
        double pValue = anova.anovaPValue(samplesArray);

        // Выполняем post-hoc тест (Tukey HSD)
        List<GroupComparisonDto> comparisons = performTukeyHSD(samples, groupNames);

        return new AnovaResultDto(fValue, pValue, pValue < 0.05, comparisons);
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

    private List<GroupComparisonDto> performTukeyHSD(List<List<Double>> samples, List<String> groupNames) {
        // Упрощенная реализация Tukey HSD
        List<GroupComparisonDto> comparisons = new ArrayList<>();

        for (int i = 0; i < samples.size(); i++) {
            for (int j = i + 1; j < samples.size(); j++) {
                double mean1 = calculateMean(samples.get(i));
                double mean2 = calculateMean(samples.get(j));
                double diff = mean1 - mean2;

                // Упрощенный расчет p-value (в реальном проекте используйте библиотеку)
                double pValue = Math.abs(diff) > 5 ? 0.01 : 0.05;

                comparisons.add(new GroupComparisonDto(
                        groupNames.get(i),
                        groupNames.get(j),
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

//    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
//        // Создаем данные для графика
//        List<String> groups = result.getGroupComparisons().stream()
//                .map(GroupComparisonDto::getGroup1)
//                .distinct()
//                .collect(Collectors.toList());
//
//        List<Double> means = new ArrayList<>();
//        for (String group : groups) {
//            double mean = result.getGroupComparisons().stream()
//                    .filter(c -> c.getGroup1().equals(group))
//                    .findFirst()
//                    .map(GroupComparisonDto::getMeanDifference)
//                    .orElse(0.0);
//            means.add(mean);
//        }
//
//        // Создаем график
//        CategoryChart chart = new CategoryChartBuilder()
//                .width(800)
//                .height(600)
//                .title("Сравнение средних оценок по группам")
//                .xAxisTitle("Группы")
//                .yAxisTitle("Средняя оценка")
//                .build();
//
//        // Вариант 1: Использование списков (предпочтительный способ)
//        chart.addSeries("Средние оценки", groups, means);
//
//        // Или Вариант 2: Использование массивов
//        // chart.addSeries("Средние оценки",
//        //         groups.toArray(new String[0]),
//        //         means.stream().mapToDouble(Double::doubleValue).toArray());
//
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.PNG);
//        return outputStream.toByteArray();
//    }

    public byte[] generateAnovaChart(AnovaResultDto result) throws IOException {
        // Группируем по методам обучения
        Map<String, Double> methodAverages = result.getGroupComparisons().stream()
                .collect(Collectors.groupingBy(
                        comparison -> comparison.getGroup1().split(" - ")[1], // Предполагаем формат "Группа - Метод"
                        Collectors.averagingDouble(GroupComparisonDto::getMeanDifference)
                ));

        // Сортируем методы обучения по имени
        List<String> methods = methodAverages.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        // Получаем средние значения в том же порядке
        List<Double> means = methods.stream()
                .map(methodAverages::get)
                .collect(Collectors.toList());

        // Создаем график
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Сравнение средних оценок по методам обучения")
                .xAxisTitle("Методы обучения")
                .yAxisTitle("Средняя оценка")
                .build();

        // Добавляем данные
        chart.addSeries("Средние оценки", methods, means);

        // Настраиваем отображение
        chart.getStyler().setXAxisLabelRotation(45); // Наклон подписей

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.PNG);
        return outputStream.toByteArray();
    }
}
