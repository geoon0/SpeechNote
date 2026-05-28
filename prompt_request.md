# 📅 SpeechNote STT API 연동 프롬프트 & 변경 이력 타임라인

 이 파일은 **실제 요청사항(프롬프트) 원문**과, 에이전트가 이를 듣고 **구체적으로 코드를 어떻게 수정하고 반영했는지(변경 이력)**를 한 쌍으로 묶어 시간 순서대로 기록해 두는 공간

앞으로 요구사항이 추가되거나 코드를 수정하게 되면, 이 문서의 가장 아래에 새로운 묶음(요청 원문 + 변경 내역)을 계속해서 이어나갈 예정

---

## 📌 [2026-05-29 03:33] 1차 요청 : STT API 연동 모듈 기본 설계 및 작성

### 💬 민건영이 보낸 요청 원문 (Raw Prompt)
```text
# 작업 요청: Java Swing STT 데스크톱 앱의 STT API 연동 모듈 작성

## 프로젝트 컨텍스트
- 학교 팀 프로젝트, 자바 Swing 기반 데스크톱 앱
- 음성 파일(.wav)을 OpenAI Whisper API에 보내서 텍스트로 변환
- 나는 "입력·API 연동" 담당, 동료는 "UI·DB·저장" 담당
- curl/Postman으로 API 시연은 이미 성공한 상태
- 이제 이걸 자바 코드로 옮겨서 동료가 함수 하나만 호출하면 결과를 받을 수 있게 만들어야 함

## 환경 / 제약
- Java 11 이상 (java.net.http.HttpClient 사용)
- 외부 라이브러리는 `org.json:json` 하나만 허용 (JSON 파싱용)
- 빌드 도구: Maven 또는 Gradle (둘 다 잘 모름, 의존성 1개 추가하는 법만 알려줘)
- 동기(synchronous) 처리만, CompletableFuture/비동기는 생략
- 로깅 라이브러리 없이 `System.out.println` 사용 (학교 프로젝트라 단순화)
- 코드 주석은 한국어로, 변수명/메서드명은 영어로
- 초보자도 한 줄씩 설명할 수 있을 정도로 단순하게 작성

## 만들 파일 (정확히 5개 + 설정 파일 1개)

src/main/java/
├─ common/
│   ├─ TranscriptResult.java   # 변환 결과 그릇 (5필드 일반 class, record 사용 금지)
│   ├─ ApiConfig.java          # API 키/URL 로딩
│   └─ ApiException.java       # 전용 예외 (RuntimeException 상속)
├─ api/
│   └─ SttClient.java          # 핵심: HttpClient로 OpenAI Whisper 호출
└─ service/
└─ TranscriptionService.java  # 동료에게 노출되는 단일 진입점
src/main/resources/
└─ config.properties           # api.key=... (.gitignore 필수, 예시 파일도 만들어줘)


## 각 파일 상세 스펙

### 1) common/TranscriptResult.java
- 일반 `public class` (record 절대 사용 금지 — 초보자 발표용)
- 5개 필드 (모두 `private final`):
  - `String id` — UUID 문자열
  - `String source` — "FILE" / "MIC" / "SYSTEM" / "MIX"
  - `String language` — "ko", "en" 등 ISO 639-1
  - `String text` — 변환된 전체 텍스트
  - `Instant createdAt` — 변환 시각
- 5개 필드 다 받는 public 생성자 1개
- 각 필드의 getter (setter 없음, 불변 객체)
- 각 필드 위에 한국어 주석 한 줄씩

### 2) common/ApiConfig.java
- static 클래스 패턴 (인스턴스 생성 X, 메서드만 static)
- `static` 블록에서 `/config.properties`를 클래스패스에서 읽어 `Properties`에 로드
- 로드 실패 시 RuntimeException 던지기
- public static 메서드 2개:
  - `getApiKey()` — properties에서 `api.key` 키 값 반환
  - `getSttUrl()` — `"https://api.openai.com/v1/audio/transcriptions"` 상수 반환

### 3) common/ApiException.java
- `RuntimeException` 상속
- 생성자 2개: `(String message)`, `(String message, Throwable cause)`

### 4) api/SttClient.java ⭐ 핵심
- 인스턴스 필드: `HttpClient http` (connectTimeout 10초 설정)
- public 메서드 1개:


public String transcribe(Path audioFile, String language)
  - 안의 흐름을 5단계로 명확히 주석 처리:
    1. multipart 본문 만들기 (private 헬퍼 호출)
    2. HttpRequest 조립 (URL, Authorization 헤더, Content-Type, timeout 5분)
    3. http.send() 실행
    4. response.statusCode() 분기:
       - 4xx → `ApiException("입력 오류 (코드): " + body)`
       - 5xx → `ApiException("서버 오류 (코드), 잠시 후 재시도")`
    5. `new JSONObject(response.body()).getString("text")` 로 텍스트만 반환
  - `IOException` / `InterruptedException` catch → `ApiException("네트워크 오류", e)` 던지기
- private 헬퍼 메서드:

private byte[] buildMultipartBody(Path file, String language, String boundary) throws IOException

  - `ByteArrayOutputStream`에 multipart/form-data 본문 직접 작성
  - 파트 3개: `file` (Content-Type: audio/wav, 파일 바이트), `model` ("whisper-1"), `language`
  - 각 파트 사이 `--boundary\r\n`, 끝은 `--boundary--\r\n`
  - boundary 문자열은 `transcribe()`에서 `"----Boundary" + System.currentTimeMillis()` 로 만들어 넘김

### 5) service/TranscriptionService.java
- 인스턴스 필드: `SttClient sttClient = new SttClient()`
- public 메서드:

public TranscriptResult transcribeFile(Path audioFile, String language)

  - sttClient.transcribe() 호출해서 텍스트 받기
  - `new TranscriptResult(UUID.randomUUID().toString(), "FILE", language, text, Instant.now())` 반환

### 6) src/main/resources/config.properties
- 실제 키 파일은 만들지 말고 **`config.properties.example`** 만 만들어줘
- 내용:

api.key=sk-여기에_본인_OpenAI_키_입력

- `.gitignore`에 `config.properties` 한 줄 추가하는 것도 알려줘

## 검증용 main (선택)
- 같은 프로젝트 안에 별도 `Demo.java`로 빠르게 테스트할 수 있는 main 함수 하나 만들어줘:

TranscriptionService service = new TranscriptionService();
TranscriptResult result = service.transcribeFile(Path.of("test.wav"), "ko");
System.out.println("ID: " + result.getId());
System.out.println("언어: " + result.getLanguage());
System.out.println("텍스트: " + result.getText());


## 출력 요구사항
1. 파일별로 코드 블록 분리해서 전체 코드 작성
2. 각 파일 위에 어떤 경로에 저장할지 주석으로 표시
3. Maven 의존성 추가 방법 (`pom.xml`의 `<dependencies>` 부분만)
4. `.gitignore`에 추가할 내용
5. 실행 전 확인할 체크리스트 (3~5줄)

## 하지 말 것 ❌
- `record` 키워드 사용
- `CompletableFuture`, `Future`, 비동기 코드
- Jackson, Gson 등 다른 JSON 라이브러리
- slf4j, logback 등 로깅 라이브러리
- Lombok
- API 키를 코드에 하드코딩
- 한 메서드를 20줄 이상으로 길게 작성 (길어지면 헬퍼로 분리)
```

### 🛠️ 에이전트의 작업 및 변경 내역 (Changes & Actions)
* **`pom.xml` 생성**: Java 11 빌드 명세 구성 및 `org.json:json` 단일 라이브러리 추가.
* **`.gitignore` 생성**: API 키 유출 방지를 위해 `src/main/resources/config.properties` 배제 규칙 적용.
* **`src/main/resources/config.properties.example` 생성**: API Key 주입용 설정 템플릿 파일 구현.
* **`src/main/java/common/ApiException.java` 생성**: Custom RuntimeException 구현.
* **`src/main/java/common/TranscriptResult.java` 생성**: `record` 키워드를 배제하고 5개의 `private final` 필드와 Getter로만 설계된 불변 DTO 구현 (각 필드에 상세 한글 주석 포함).
* **`src/main/java/common/ApiConfig.java` 생성**: `static` 블록에서 resources의 프로퍼티 파일을 읽고 API key 및 URL 상수를 반환하는 설정 유틸리티 클래스 구현.
* **`src/main/java/api/SttClient.java` 생성**: `HttpClient`를 활용해 동기식으로 Whisper STT API를 호출하고, `ByteArrayOutputStream`을 활용해 바이트 기반 `multipart/form-data` 본문을 수동으로 직접 구현. 4xx/5xx 상세한 오류 분기 처리 및 5단계 주요 주석 기재 완료.
* **`src/main/java/service/TranscriptionService.java` 생성**: 동료 파트너가 사용할 유일한 진입점으로, `SttClient` 호출 결과를 UUID 및 타임스탬프와 매핑하여 최종 `TranscriptResult` 인스턴스로 전달하는 파사드 레이어 설계.
* **`src/main/java/Demo.java` 생성**: 로컬에서 연동 상태를 단독 수동 검증해볼 수 있도록 간편한 `main` 코드를 담은 테스터 클래스 작성.

---

## 📌 [2026-05-29 03:50] 2차 요청 : 주석 서식 교정 및 비전문 어조 배제 지시

### 💬 민건영이 보낸 요청 원문 (Raw Prompt)
```text
모든 주석은 음슴체로 끝내고, 초보자가 보기좋게~ 같이 초보티 내는 문장은 쓰지마, 교수님도 이거 본단 말이야. 설명만 써
```

### 🛠️ 에이전트의 작업 및 변경 내역 (Changes & Actions)
* `TranscriptResult.java`, `ApiConfig.java`, `ApiException.java`, `SttClient.java`, `TranscriptionService.java`, `Demo.java` 자바 파일 내의 모든 주석 종결 어미를 간결한 학술식 **'음슴체'**로 전면 정정함.
* 클래스 기술서나 설명 주석에 포함되어 있던 "초보자가 보기 좋게", "발표용" 등과 같이 비전문적이고 사설에 준하는 표현들을 완전히 박멸하고, **객관적인 기동 조건 및 파라미터 설명만 기술되도록 고도화**함.

---

## 📌 [2026-05-29 03:51] 3차 요청 : 주석 변경 내역 기록 지시

### 💬 민건영이 보낸 요청 원문 (Raw Prompt)
```text
모든 주석은 음슴체로 변경해
```

### 🛠️ 에이전트의 작업 및 변경 내역 (Changes & Actions)
* `prompt_request.md` 타임라인 기록장에 민건영 님이 명령한 주석 스타일 개편 지시 원문과 에이전트의 구체적인 소스코드 주석 변환 작업 내역을 쌍으로 결합하여 신규 추가 완료함.


