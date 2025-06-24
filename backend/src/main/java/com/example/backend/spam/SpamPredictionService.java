package com.example.backend.spam;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class SpamPredictionService {


    private static final String PYTHON_SCRIPT_PATH = "backend\\python\\spam_classifier.py";

    public SpamResult predictSpam(String content) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", PYTHON_SCRIPT_PATH, content);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || output.toString().contains("error")) {
                System.out.println("output.toString() = " + output.toString());
                return new SpamResult(false, "Python 脚本执行失败：" + output);
            }

            // 解析 JSON 结果
            String jsonStr = output.toString();
            boolean isSpam = jsonStr.contains("\"is_spam\": true");
            double confidence = extractConfidence(jsonStr);

            return new SpamResult(isSpam, "AI 判断，置信度：" + confidence);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SpamResult(false, "调用失败：" + e.getMessage());
        }
    }

    private double extractConfidence(String jsonStr) {
        try {
            String[] parts = jsonStr.split("\"confidence\":")[1].split("}")[0].trim().split(",");
            return Double.parseDouble(parts[0]);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
