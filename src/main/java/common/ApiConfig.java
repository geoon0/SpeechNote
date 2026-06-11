package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 프로젝트 루트 폴더의 config.properties 파일에서 API 설정을 읽고 쓰는 클래스.
  *
 * @author 개발자
 */
public class ApiConfig {

    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    // 기본 커스텀 STT API 서버 주소
    private static final String DEFAULT_STT_URL = "https://b93b-123-212-224-99.ngrok-free.app/transcribe";

    static {
        loadConfig();
    }

    private ApiConfig() {}

    /**
     * 물리적인 config.properties 파일에서 설정을 로드합니다.
     */
    public static void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                LoggerUtil.logError("설정 파일 로드 중 오류", e);
            }
        } else {
            // 파일이 없으면 기본값 세팅 후 생성
            properties.setProperty("stt.api.url", DEFAULT_STT_URL);
            properties.setProperty("api.key", "");
            saveConfig(DEFAULT_STT_URL, "");
        }
    }

    /**
     * 설정을 파일에 저장합니다.
     */
    public static void saveConfig(String url, String key) {
        properties.setProperty("stt.api.url", url);
        properties.setProperty("api.key", key);
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "SpeechNote Configuration");
        } catch (IOException e) {
            LoggerUtil.logError("설정 파일 저장 중 오류", e);
        }
    }

    public static String getApiKey() {
        return properties.getProperty("api.key", "");
    }

    public static String getSttUrl() {
        return properties.getProperty("stt.api.url", DEFAULT_STT_URL);
    }
}
