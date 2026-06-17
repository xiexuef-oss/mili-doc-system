package com.military.doc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {
    private boolean enabled = false;
    private String tesseractPath = "tesseract";
    private String language = "chi_sim+eng";
    private int dpi = 300;
    private int timeoutSeconds = 120;

    /** OCR引擎: tesseract | paddleocr */
    private String provider = "tesseract";

    /** PaddleOCR Python解释器路径 */
    private String paddleocrPython = "python";

    /** PaddleOCR桥接脚本路径 (相对于backend目录) */
    private String paddleocrScript = "scripts/ocr_paddle.py";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTesseractPath() { return tesseractPath; }
    public void setTesseractPath(String tesseractPath) { this.tesseractPath = tesseractPath; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public int getDpi() { return dpi; }
    public void setDpi(int dpi) { this.dpi = dpi; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaddleocrPython() { return paddleocrPython; }
    public void setPaddleocrPython(String paddleocrPython) { this.paddleocrPython = paddleocrPython; }
    public String getPaddleocrScript() { return paddleocrScript; }
    public void setPaddleocrScript(String paddleocrScript) { this.paddleocrScript = paddleocrScript; }
}
