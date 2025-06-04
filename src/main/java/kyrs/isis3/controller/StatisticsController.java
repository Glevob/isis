package kyrs.isis3.controller;

import kyrs.isis3.model.AnovaResultDto;
import kyrs.isis3.model.MethodStats;
import kyrs.isis3.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Controller
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/anova")
    public String showAnovaResults(
            @RequestParam(name = "alpha", defaultValue = "0.05") double alpha,
            Model model) throws IOException {

        // Проверка допустимых значений alpha
        if (alpha <= 0 || alpha >= 1) {
            alpha = 0.05; // Значение по умолчанию при некорректном вводе
        }

        AnovaResultDto result = statisticsService.performAnovaAnalysis(alpha);
        byte[] chartImage = statisticsService.generateAnovaChart(result);

        Map<String, MethodStats> methodStats = statisticsService.calculateMethodStatistics();

        // Calculate overall mean and total count
        double overallMean = methodStats.values().stream()
                .mapToDouble(ms -> ms.getMean() * ms.getCount())
                .sum() /
                methodStats.values().stream()
                        .mapToLong(MethodStats::getCount)
                        .sum();

        long totalCount = methodStats.values().stream()
                .mapToLong(MethodStats::getCount)
                .sum();

        model.addAttribute("anovaResult", result);
        model.addAttribute("chartImage", Base64.getEncoder().encodeToString(chartImage));
        model.addAttribute("methodStats", methodStats);
        model.addAttribute("overallMean", overallMean);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("selectedAlpha", alpha); // Добавляем выбранный alpha в модель

        return "/anova-results";
    }

    @GetMapping("/anova/chart")
    @ResponseBody
    public ResponseEntity<byte[]> getAnovaChart(
            @RequestParam(name = "alpha", defaultValue = "0.05") double alpha) throws IOException {

        // Проверка допустимых значений alpha
        if (alpha <= 0 || alpha >= 1) {
            alpha = 0.05; // Значение по умолчанию при некорректном вводе
        }

        AnovaResultDto result = statisticsService.performAnovaAnalysis(alpha);
        byte[] chartImage = statisticsService.generateAnovaChart(result);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(chartImage);
    }
}