# 프로젝트 진행 상황 요약 (Progress Summary)

## ✅ 지금까지 한 작업 (완료된 기능)

1. **STT API 클라이언트 통신 기반 구축**
   - Java 11 `HttpClient`를 활용한 OpenAI Whisper API 연동 모듈 (`SttClient`) 작성 완료
   - 설정 파일(`config.properties`)로부터 API Key 및 URL을 안전하게 로드하는 유틸리티 (`ApiConfig`) 구현
   - API 통신 중 발생하는 에러를 다루기 위한 커스텀 예외 (`ApiException`) 추가

2. **Multipart 통신 모듈 고도화 및 분리**
   - Multipart 요청 바디를 구성하는 책임을 전담하는 `MultipartBodyPublisher` 헬퍼 클래스 신규 작성
   - Whisper API 호출 시 `verbose_json` 포맷을 요청하도록 속성 추가

3. **응답 데이터 구조 개선 및 세그먼트(Segment) 파싱**
   - API 응답 데이터에서 시간대별(시작/종료 시간)로 텍스트를 분리해 보관할 수 있도록 `Segment` 및 `SttResponse` DTO 클래스 추가
   - 기존의 단일 문자열 응답 구조에서, 전체 텍스트(`rawText`)와 구간별 텍스트 목록(`List<Segment>`)을 모두 포함하도록 `TranscriptResult` 구조 개선

4. **서비스 계층(Service Layer) 구축 및 비동기 지원**
   - UI 등 클라이언트 측에서 단일 진입점으로 사용할 `TranscriptionService` 작성 완료
   - 동기 메서드(`transcribeFile`) 뿐만 아니라, UI 블로킹 방지를 위해 `CompletableFuture`를 반환하는 비동기 메서드(`transcribeFileAsync`) 추가
   - 로깅 기능 보강 및 `Demo.java`를 통한 콘솔 기반 단독 테스트 환경 구축 완료

---

## 🚀 앞으로 남은 작업 (해야 할 작업)

1. **오디오 녹음 기능 구현 (핵심)**
   - 마이크 음성을 캡처하여 로컬에 파일(.wav 등)로 저장하는 기능
   - 스피커로 출력되는 시스템 음성을 캡처하는 기능 (마이크와 시스템 음성 믹싱 기능 포함)

2. **GUI 메인 화면 개발 (Swing UI)**
   - 사용자 인터페이스를 Swing으로 제작 (입력 소스 선택, 녹음 시작/중지 버튼)
   - 파일 변환 진행 상태(Progress Bar) 표시 및 결과 텍스트가 렌더링되는 결과창 화면 구성
   - 결과 텍스트 복사 및 텍스트 파일(txt 등) 다운로드 기능 추가

3. **데이터베이스(DB) 설계 및 연동**
   - 변환된 텍스트 및 메타데이터(타임스탬프 등)를 보관할 로컬 DB (예: SQLite, H2) 스키마 설계
   - JDBC 연동 및 히스토리 저장, 최근 변환 목록 조회 기능 구현

4. **부가 AI 기능 파이프라인 연결**
   - 텍스트 요약, 맞춤법 교정, 키워드 추출을 위한 추가 외부 API 연결 모듈 작성

5. **(선택) 보안 및 로컬 계정 시스템**
   - 사용자별 로컬 로그인 및 데이터 분리, 비밀번호 해싱 적용

6. **통합 연동 테스트 및 배포 준비**
   - 모듈 간(UI - Service - API - DB) 통합 연동 테스트 진행
   - 최종 사용자가 JRE 환경에서 바로 실행 가능한 단일 JAR 파일(Runnable JAR)로 빌드 및 배포 준비
