package common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 리소스 설정 파일(/config.properties)에서 OpenAI API 키를 읽어오는 설정 클래스임.
 * static 접근을 제공하여 전역적으로 설정을 로드할 수 있게 함.
 */
public class ApiConfig {

    // 로딩된 속성 키-값을 담는 Properties 객체
    private static final Properties properties = new Properties();
    
    // 로드할 리소스 설정 파일의 경로
    private static final String CONFIG_FILE = "/config.properties";

    // 기본 커스텀 STT API 서버 주소 (properties에 없을 시 사용됨)
    private static final String DEFAULT_STT_URL = "https://24a2-123-212-224-99.ngrok-free.app/transcribe";

    // 클래스 로드 시 설정 파일을 클래스패스에서 읽어옴.
    static {
        try (InputStream input = ApiConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("설정 파일(" + CONFIG_FILE + ")이 resources 디렉토리에 존재하지 않음.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("설정 파일(" + CONFIG_FILE + ")을 불러오는 중 예외가 발생함.", e);
        }
    }

    // 인스턴스화 방지를 위해 private 생성자로 제약함.
    private ApiConfig() {}

    /**
     * properties 파일 내에 기재된 'api.key'의 값을 가져옴.
     * @return OpenAI API 키 문자열
     */
    public static String getApiKey() {
        return properties.getProperty("api.key");
    }

    /**
     * STT API 서버의 엔드포인트 URL을 제공함. config 파일에서 읽거나 기본값을 사용함.
     * @return API 엔드포인트 URL 주소
     */
    public static String getSttUrl() {
        return properties.getProperty("stt.api.url", DEFAULT_STT_URL);
    }
}
