package us.modal.configmapdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConfigMapDemoController {

    @Value("${app.message}")
    private String message;

    @Value("${app.version}")
    private String version;

    @Value("${demo.environment.string}") // default "NOPE" value is set in application.properties
    private String demoEnvironmentString;

    @Value("${demo.file.string:NOPE}")
    private String demoFileString;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("message", message);
        model.addAttribute("version", version);
        model.addAttribute("demoEnvironmentString", demoEnvironmentString);
        model.addAttribute("demoFileString", demoFileString);
        return "home";
    }
}