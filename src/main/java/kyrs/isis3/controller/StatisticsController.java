package kyrs.isis3.controller;

import kyrs.isis3.model.AnovaResultDto;
import kyrs.isis3.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Base64;

@Controller
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/anova")
    public String showAnovaResults(Model model) throws IOException {
        AnovaResultDto result = statisticsService.performAnovaAnalysis();
        byte[] chartImage = statisticsService.generateAnovaChart(result);

        model.addAttribute("anovaResult", result);
        model.addAttribute("chartImage", Base64.getEncoder().encodeToString(chartImage));
        return "/anova-results";
    }

    @GetMapping("/anova/chart")
    @ResponseBody
    public ResponseEntity<byte[]> getAnovaChart() throws IOException {
        AnovaResultDto result = statisticsService.performAnovaAnalysis();
        byte[] chartImage = statisticsService.generateAnovaChart(result);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(chartImage);
    }
}
