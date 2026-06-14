# 프로젝트 진행 상황 요약 (Progress Summary)

## ✅ 지금까지 한 작업 (완료된 기능)

### [1주차]
1. **STT API 클라이언트 통신 기반 구축**
   - Java 11 `HttpClient`를 활용한 OpenAI Whisper API 연동 모듈 (`SttClient`) 작성 완료
   - 설정 파일(`config.properties`)로부터 API Key 및 URL을 안전하게 로드하는 유틸리티 (`ApiConfig`) 구현
   - API 통신 중 발생하는 에러를 다루기 위한 커스텀 예외 (`ApiException`) 추가

2. **Multipart 통신 모듈 고도화 및 분리**
   - Multipart 요청 바디를 구성하는 책임을 전담하는 `MultipartBodyPublisher` 헬퍼 클래스 신규 작성 (커스텀 서버 교체 후 현재는 사용하지 않음)
   - Whisper API 호출 시 `verbose_json` 포맷을 요청하도록 속성 추가

3. **응답 데이터 구조 개선 및 세그먼트(Segment) 파싱**
   - API 응답 데이터에서 시간대별(시작/종료 시간)로 텍스트를 분리해 보관할 수 있도록 `Segment` 및 `SttResponse` DTO 클래스 추가
   - 기존의 단일 문자열 응답 구조에서, 전체 텍스트(`rawText`)와 구간별 텍스트 목록(`List<Segment>`)을 모두 포함하도록 `TranscriptResult` 구조 개선

4. **서비스 계층(Service Layer) 구축 및 비동기 지원**
   - UI 등 클라이언트 측에서 단일 진입점으로 사용할 `TranscriptionService` 작성 완료
   - 동기 메서드(`transcribeFile`) 뿐만 아니라, UI 블로킹 방지를 위해 `CompletableFuture`를 반환하는 비동기 메서드(`transcribeFileAsync`) 추가
   - 로깅 기능 보강 및 `Demo.java`를 통한 콘솔 기반 단독 테스트 환경 구축 완료

### [2주차]
5. **커스텀 STT API 서버 규격 연동 및 예외 처리 (Fallback)**
   - API 호출 목적지를 로컬 WSL 환경에서 ngrok 터널링으로 구동하는 커스텀 STT 서버(`https://...ngrok-free.app/transcribe`)로 전면 교체
   - 호출 시 `file` 파트 하나만 전송하도록 전송 바디 단순화 및 응답 대기를 위해 타임아웃을 60초 이상으로 확장
   - API 서버가 `text` 필드 안에 JSON 배열 형태의 문자열을 그대로 내려주는 특수한 케이스 대응을 위한 Fallback 파싱 로직 구현 (`SttResponse.java`)

6. **실시간 마이크 녹음 기능 구현**
   - PC에 연결된 마이크 장치 목록을 스캔하는 `AudioDeviceManager` 구현
   - 실시간 오디오 입력을 캡처하여 STT 최적 포맷(16kHz, 16bit, Mono)의 `.wav` 파일로 자동으로 저장해주는 `AudioRecorder` 구현
   - 오디오 장치를 선택할 수 있는 `SettingsDialog` 팝업창 UI 제작 및 메인 화면 연동

7. **GUI 메인 화면 개발 및 사용자 경험 개선**
   - Swing 기반의 메인 윈도우인 `MainFrame` 개발 완료
   - 파일 선택, 녹음 시작/중지, 변환 작업 등의 제어 버튼 및 상태 진행률 표시를 위한 로딩바(`JProgressBar`) 적용
   - 텍스트 창에 변환 결과를 깔끔하게 포맷팅하여 렌더링하고, 편리하게 활용하도록 클립보드 복사 기능 구현

8. **로컬 SQLite 데이터베이스(DB) 및 기록 관리 기능 연동**
   - 애플리케이션 시작 시 자동으로 데이터베이스 파일을 생성하고 `transcripts` 및 `segments` 테이블을 생성하는 `DatabaseManager` 구현
   - 변환 결과를 DB에 쓰고, 전체 기록을 불러오며, 특정 기록을 삭제하는 CRUD 기능을 지원하는 `TranscriptDao` 패턴 도입
   - 좌측에 📁 아이콘, 날짜, 텍스트 미리보기가 제공되는 과거 기록 목록 패널을 구성하여 클릭 시 이전에 변환한 텍스트를 즉시 다시 조회할 수 있게 함
   - 변환 완료 시 즉시 목록 최상단에 결과를 갱신하고, 필요 없는 기록은 DB 및 UI 목록에서 완전히 제거하는 기록 삭제 기능 구현

---

## 🚀 앞으로 남은 작업 (해야 할 작업)

1. **시스템 음성 캡처 및 동시 녹음 구현**
   - 스피커로 출력되는 시스템 음성을 캡처하는 기능 (WASAPI loopback 연동 등)
   - 마이크 음성과 시스템 음성을 함께 녹음하고, 채널 라벨을 구분하여 저장하는 기능

2. **다운로드 및 내보내기 기능 추가**
   - 변환된 텍스트 및 시간대별 세그먼트를 텍스트 파일(.txt)로 로컬 드라이브에 저장하는 기능
   - 워드 문서(.docx) 및 자막 파일(.srt) 등의 다양한 포맷 내보내기 지원

3. **녹음 컨트롤 고도화 및 상태 표시**
   - 녹음 일시정지(Pause) / 재개(Resume) 기능 구현
   - 실시간 녹음 경과 시간 표시 타이머 및 입력 음성 레벨을 실시간으로 보여주는 데시벨 레벨 미터 UI 적용

4. **환경설정 창 기능 확장**
   - 사용자가 직접 API 주소를 입력하고 저장할 수 있는 UI 환경 구성 및 저장 로직 구현

5. **부가 AI 기능 연동 (3주차)**
   - HttpClient 통신을 활용하여 변환 완료된 텍스트의 요약문 작성, 맞춤법 교정, 키워드 추출 파이프라인 추가

6. **다국어 음성 인식 지원 및 안정화**
   - 영어 및 기타 다국어 음성 인식 옵션 적용
   - API 재시도 로직, 타임아웃 안정화, 유닛 테스트 수행 및 JAR Runnable Build 구성
