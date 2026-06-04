# 📅 SpeechNote 프롬프트 요청 & 결과 타임라인

이 파일은 **실제 요청사항(프롬프트) 원문**과, AI 에이전트가 이를 듣고 **구체적으로 코드를 어떻게 수정하고 반영했는지(변경 이력)**를 한 쌍으로 묶어 시간 순서대로 기록해 두는 공간임.

> 📌 프로젝트 일정: 총 3주 (1주차: 05-25~05-31, 2주차: 06-01~06-07, 3주차: 06-08~06-14)
> 📌 담당자: 민건영 (입력·API 담당)
> ♻️ 핵심 원칙: 1주차에 만든 `SttClient` + `TranscriptionService`를 2·3주차 내내 **그대로 재사용**함. 마이크·시스템 음성도 결국 "음성 → 같은 `transcribeFile`"로 흘려보내고, LLM(요약·키워드)도 같은 HttpClient 통신 구조를 재사용함.

---

# 🗺️ 전체 로드맵 (민건영 담당 1~3주차)

| **주차** | **내 작업 (요약)** | **관련 요구사항** | **주차 끝 산출물** |
|---|---|---|---|
| **1주차** (05/25~05/31) | STT API 연동, 파일 → 텍스트 Service 계층 | F-02 | `transcribeFile(파일, 언어)` → 텍스트 반환 |
| **2주차** (06/01~06/07) | 마이크 목록·녹음·DB저장·기록관리 → STT | F-09a, F-01, F-04, F-05 | 마이크 녹음만으로 실시간 변환 + DB 저장 + 기록 조회/삭제 |
| **3주차** (06/08~06/14) | 시스템 음성 캡처, 동시 녹음·채널 라벨, LLM 요약/키워드, 다국어, API 안정화·테스트 | F-01, F-14, F-07, F-08 | 시스템 음성 변환 + 요약/자막 + 안정화 |

---

# 📝 프롬프트 요청 & 결과 이력

---

## 📌 [2026-05-29 03:33] 1차 요청 — STT API 연동 모듈 기본 설계 및 작성 (1주차)

### 💬 프롬프트 요청 원문
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
- 외부 라이브러리는 org.json:json 하나만 허용 (JSON 파싱용)
- 빌드 도구: Maven
- 동기(synchronous) 처리만
- 코드 주석은 한국어로, 변수명/메서드명은 영어로

## 만들 파일 (정확히 5개 + 설정 파일 1개)
- common/TranscriptResult.java (변환 결과 불변 DTO, 5개 private final 필드)
- common/ApiConfig.java (API Key/URL 로딩, static 클래스)
- common/ApiException.java (전용 RuntimeException)
- api/SttClient.java (HttpClient로 OpenAI Whisper 호출, multipart/form-data 직접 구현)
- service/TranscriptionService.java (동료에게 노출되는 단일 진입점)
- src/main/resources/config.properties.example (API Key 템플릿)
- Demo.java (검증용 main)
```

### 🛠️ 작업 결과
| 생성/수정된 파일 | 역할 |
|---|---|
| `pom.xml` | Java 11 빌드 명세 구성, `org.json:json` 라이브러리 의존성 추가 |
| `.gitignore` | `config.properties` API 키 유출 방지 배제 규칙 적용 |
| `config.properties.example` | API Key 주입용 설정 템플릿 |
| `common/ApiException.java` | Custom RuntimeException 구현 |
| `common/TranscriptResult.java` | 5개 `private final` 필드 + Getter 불변 DTO |
| `common/ApiConfig.java` | static 블록에서 프로퍼티 파일 로드, API key/URL 반환 유틸 |
| `api/SttClient.java` | HttpClient 동기 호출, ByteArrayOutputStream 기반 multipart/form-data 직접 구현, 4xx/5xx 오류 분기 |
| `service/TranscriptionService.java` | SttClient 호출 → UUID/타임스탬프 매핑 → TranscriptResult 반환하는 파사드 레이어 |
| `Demo.java` | 로컬 수동 검증용 main 테스터 |

---

## 📌 [2026-05-29 03:50] 2차 요청 — 주석 서식 교정

### 💬 프롬프트 요청 원문
```text
모든 주석은 음슴체로 끝내고, 초보자가 보기좋게~ 같이 초보티 내는 문장은 쓰지마, 교수님도 이거 본단 말이야. 설명만 써
```

### 🛠️ 작업 결과
* 전체 자바 파일의 모든 주석 종결 어미를 간결한 학술식 **'음슴체'**(~임, ~함)로 전면 정정함.
* "초보자가 보기 좋게", "발표용" 등 비전문적 사설을 완전히 제거하고, 객관적인 기동 조건 및 파라미터 설명만 기술되도록 교정함.

---

## 📌 [2026-06-04] 3차 요청 — 커스텀 STT API 서버 규격으로 전면 교체 (2주차)

### 💬 프롬프트 요청 원문
```text
내가 wsl 환경에서 ngrok으로 STT API 서버를 띄워놨거든?
이걸로 API 주소를 바꾸고 싶어.
config.properties에 stt.api.url=https://고정도메인.ngrok-free.app/transcribe 이런 식으로 넣고 쓸 수 있게 해줘.
방식은 POST고 multipart/form-data로 file 파라미터에 오디오 파일(.wav) 넣어서 보내면 작동해.
그리고 응답은 JSON으로 오는데 성공하면 status가 success고, text(전체 변환 텍스트)랑 segments(시간대별 발화 정보 배열)가 리턴돼. segments 안에는 Start, End, Speaker, Content 같은 필드들이 있고.
만약 에러나면 status는 error고 message에 에러 이유가 실려와.
아, 그리고 AI 모델 돌리는 거라 결과 나오기까지 좀 걸리니까 타임아웃은 60초 이상으로 넉넉하게 잡아줘.
```

### 🛠️ 작업 결과
| 생성/수정된 파일 | 변경 내용 |
|---|---|
| `common/ApiConfig.java` | OpenAI URL 상수 제거 → `config.properties`에서 `stt.api.url` 키로 커스텀 서버 주소를 읽어오도록 변경 |
| `config.properties.example` | `api.key` 제거, `stt.api.url=https://고정도메인.ngrok-free.app/transcribe` 로 교체 |
| `api/SttClient.java` | Authorization 헤더 제거, multipart에서 `model`/`language` 파트 제거 → `file` 파트만 전송하도록 단순화 |
| `api/SttResponse.java` | 커스텀 서버의 `status`/`text`/`segments` JSON 구조에 맞게 파싱 로직 전면 개편. `Speaker`(화자) 필드 추가 매핑 |
| `common/TextSegment.java` | 기존 `Segment.java`에서 이름 변경. `speaker` (Integer) 필드 신규 추가 |
| `api/MultipartBodyPublisher.java` | 더 이상 필요 없는 파일 삭제 |

---

## 📌 [2026-06-04] 4차 요청 — Java Swing GUI 메인 화면 개발

### 💬 프롬프트 요청 원문
```text
일단 간단하게 GUI 만들어서 기능 돌아가는 것만 먼저 확인해보자.
```

### 🛠️ 작업 결과
| 생성/수정된 파일 | 역할 |
|---|---|
| `SpeechNoteApp.java` | 프로그램 실행 진입점. 시스템 룩앤필 테마 적용 후 메인 창을 EDT(이벤트 디스패치 스레드) 위에서 안전하게 띄움 |
| `ui/MainFrame.java` | 메인 윈도우. 음성 파일 선택(`JFileChooser`), 변환 시작, 로딩 바(`JProgressBar`), 결과 출력(`JTextArea`), 텍스트 복사 버튼 등 핵심 UI 레이아웃 및 이벤트 처리 |

---

## 📌 [2026-06-04] 5차 요청 — 실시간 마이크 녹음 기능 구현

### 💬 프롬프트 요청 원문
```text
녹음 파일을 업로드 하는게 아닌, 직접 그 자리에서 녹음하고 변환되는 기능 구현 가능할까?
(만약 추가할거면 오디오 채널 선택할수 있는 설정창도 따로 만들어야 되겠지)
```

### 🛠️ 작업 결과
| 생성/수정된 파일 | 역할 |
|---|---|
| `audio/AudioDeviceManager.java` | PC에 연결된 마이크(입력 장치) 목록 스캔 |
| `audio/AudioRecorder.java` | 선택된 마이크로 실시간 음성 캡처 → `records/` 폴더에 `.wav` 파일 자동 저장 (16kHz, 16bit, Mono) |
| `ui/SettingsDialog.java` | 오디오 채널(마이크) 선택 팝업창. 드롭다운으로 마이크 목록 표시 |
| `ui/MainFrame.java` | [설정⚙️] [🔴녹음시작] [⬛녹음중지] 버튼 추가. 녹음 완료 시 자동으로 STT 변환 트리거 |

---

## 📌 [2026-06-04] 6차 요청 — 로컬 SQLite 데이터베이스 구축

### 💬 프롬프트 요청 원문
```text
DB를 구현하자. 그런데 자바 프로그램을 실행하는 컴퓨터에서 저장하는게 나을까?
아니면 그냥 api로 연결되어있는 wsl환경(AI 모델을 돌리고 있는)에서 저장해놓는게 좋을까?
```

### 💡 설계 판단
* **결론: 로컬(내 PC)에 SQLite로 저장**
* 이유: ① 개인 음성 메모의 프라이버시 보호 ② 서버 꺼져 있어도 오프라인 열람 가능 ③ 서버는 순수 번역 기계(Stateless)로 경량 유지

### 🛠️ 작업 결과
| 생성/수정된 파일 | 역할 |
|---|---|
| `pom.xml` | `org.xerial:sqlite-jdbc` 라이브러리 의존성 추가 |
| `db/DatabaseManager.java` | 프로그램 시작 시 `speechnote.db` 파일 생성, `transcripts` 및 `segments` 테이블 자동 초기화 |
| `db/TranscriptDao.java` | 변환 결과 저장(`INSERT`), 과거 기록 전체 조회(`SELECT`) DAO 패턴 구현 |
| `service/TranscriptionService.java` | 변환 성공 시 `TranscriptDao.saveTranscript()` 자동 호출하여 DB 저장 로직 추가 |
| `SpeechNoteApp.java` | `main()` 시작 시 `DatabaseManager.initializeDatabase()` 호출 추가 |

---

## 📌 [2026-06-04] 7차 요청 — 좌측 '과거 기록 목록' 패널 UI 구현

### 💬 프롬프트 요청 원문
```text
패널 ㄱㄱ
```

### 🛠️ 작업 결과
| 수정된 파일 | 변경 내용 |
|---|---|
| `ui/MainFrame.java` | `JSplitPane`으로 화면을 좌(기록목록)/우(결과출력)로 분할. 좌측에 `JList` + 커스텀 `ListCellRenderer`(📁아이콘 + 날짜 + 텍스트 미리보기 18자) 적용. 앱 시작 시 `TranscriptDao.getAllTranscripts()`로 DB 로드. 클릭 시 우측에 상세 내용 출력. 신규 변환 완료 시 좌측 목록 최상단에 자동 추가 |

---

## 📌 [2026-06-04] 8차 요청 — API 응답 파싱 Fallback 처리

### 💬 프롬프트 요청 원문
```text
그리고, 녹음한 내용을 상세내용 박스에 표시할때 내용 전체가 그냥 노출돼. 이거 수정이 조금 필요해 보이는데?
▶ 전체 텍스트:
assistant
[{"Start":0.0,"End":1.11,"Content":"[Silence]"},{"Start":1.11,"End":19.99,"Speaker":0,"Content":"안녕하세요 민권영 고객님. 보험업 감독 업무 수행 세칙 제 5조에 따라 가입하신 보험계약 내용에 대한 공시 안내장을 보내드립니다. 자세한 내용은 안내장 다운로드 버튼을 선택하여 확인해 주세요."}]

▶ 시간대별 상세:
```

### 🛠️ 작업 결과
| 수정된 파일 | 변경 내용 |
|---|---|
| `api/SttResponse.java` | `text` 필드 안에 JSON 배열 문자열이 그대로 담겨 온 경우를 감지하는 **Fallback 파싱 로직** 추가. segments 배열로 재분해하고, `[Silence]` 메타 태그를 제외한 순수 발화 텍스트만 추출하여 깔끔하게 표시되도록 수정 |

---

## 📌 [2026-06-04] 9차 요청 — 과거 기록 삭제 기능

### 💬 프롬프트 요청 원문
```text
db에서(과거 기록목록) 원하는 걸 삭제하는 기능도 추가해줘
```

### 🛠️ 작업 결과
| 생성/수정된 파일 | 변경 내용 |
|---|---|
| `db/TranscriptDao.java` | `deleteTranscript(String id)` 메서드 추가. 외래 키 CASCADE로 segments 자동 삭제 |
| `ui/MainFrame.java` | 하단에 [🗑️ 기록 삭제] 버튼 추가. 좌측 목록에서 항목 선택 후 클릭 시 "삭제하시겠습니까?" 확인 팝업 → DB + 목록에서 영구 삭제 |

---

## 📌 [2026-06-04] 10차 요청 — GitHub 파일별 역할 커밋

### 💬 프롬프트 요청 원문
```text
이걸 깃허브에 각각 어떤 역할인지 / 어떤 코딩 작업을 했는지 간단하게 커밋해줘.
파일 각각 역할 쓰라고
```

### 🛠️ 작업 결과
기존 묶음 커밋을 `git reset`으로 해체한 뒤, 폴더/파일별 역할을 커밋 메시지에 명시하여 **개별 커밋 6건**으로 분리 후 `force push` 완료:
| 커밋 메시지 | 포함 파일 |
|---|---|
| `SpeechNoteApp.java: 프로그램 실행 진입점 및 메인 창 띄우기` | SpeechNoteApp.java |
| `ui/: 녹음/변환 메인 화면 및 마이크 선택 팝업창 구성` | ui/MainFrame.java, ui/SettingsDialog.java |
| `audio/: 마이크 스캔 및 실시간 음성 캡처 기능` | audio/AudioDeviceManager.java, audio/AudioRecorder.java |
| `api/: 커스텀 STT API 서버 통신 및 응답 파싱` | api/SttClient.java, api/SttResponse.java |
| `common/: STT 응답 보관(TextSegment) 및 주소 설정` | common/TextSegment.java, common/ApiConfig.java, common/TranscriptResult.java |
| `config & Demo: API 설정 예시 및 콘솔 출력 수정` | config.properties.example, Demo.java |

---

# 🧩 이슈 코드(F-01~F-20) 완료 현황 (2026-06-04 기준)

> 📊 **요약**: 완전 완료 ✅ 5건 / 부분 완료 🔄 2건 / 미착수 ⬜ 13건.

| **ID** | **요구사항** | **담당** | **상태** |
|---|---|---|---|
| F-01 | 마이크+시스템 음성 동시 녹음 → 변환 | 민건영 | 🔄 마이크 녹음 완료 / 시스템 음성 남음 |
| F-02 | 음성 파일(mp3/wav) 업로드 → 텍스트 변환 | 민건영 | ✅ 완료 |
| F-03 | 변환 진행 상태·결과 표시(복사 포함) | 민건영 | ✅ 완료 (GUI) |
| F-04 | 변환 기록 DB 저장·목록 조회 | 민건영 | ✅ 완료 (SQLite + 좌측 패널) |
| F-05 | 저장 항목 다운로드/삭제 | 민건영 | 🔄 삭제 완료 / .txt 다운로드 남음 |
| F-06 | 회원가입/로그인 (사용자별 기록 분리) | 정의영 | ⬜ |
| F-07 | 텍스트 요약·맞춤법 교정·키워드 추출 | 민건영 | ⬜ |
| F-08 | 다국어(영어 등) 음성 인식 | 민건영 | ⬜ |
| F-09 | 입력 장치 선택(a) + 실시간 입력 레벨 표시(b) | 민건영(a)/정의영(b) | 🔄 장치 선택(F-09a) 완료 / 레벨 미터(F-09b) 남음 |
| F-10 | 녹음 일시정지/재개 + 경과 시간 표시 | 민건영·정의영 | ⬜ |
| F-11 | 진행 중인 변환 작업 취소(Cancel) | 민건영 | ⬜ |
| F-12 | 변환 결과 텍스트 편집(수정 후 재저장) | 정의영 | ⬜ |
| F-13 | 다양한 형식 다운로드 (txt/docx/srt) | 정의영 | ⬜ |
| F-14 | 화자(채널) 구분 표시 — 마이크/시스템 라벨링 | 민건영 | 🔄 STT 화자 정보 표시 완료 / 채널 라벨 남음 |
| F-15 | 변환 기록 검색·필터링 (제목·날짜·키워드) | 정의영 | ⬜ |
| F-16 | 기록 정렬(최신순·제목순) · 페이지 조회 | 정의영 | ⬜ |
| F-17 | 비밀번호 변경 · 회원 탈퇴 | 정의영 | ⬜ |
| F-18 | 변환 전 음성 파일 미리듣기(재생·탐색) | 정의영 | ⬜ |
| F-19 | 오류 알림 + 오류 로그 파일 저장 | 정의영 | ⬜ |
| F-20 | 환경설정 — API 키 입력·보관(필수), 테마·글꼴(부가) | 정의영 | ⬜ |

---

# 📊 현재 프로젝트 파일 구조 (2026-06-04 기준)

```text
src/main/java/
├─ SpeechNoteApp.java          # 프로그램 실행 진입점
├─ Demo.java                   # 콘솔 테스트용 main
├─ api/
│   ├─ SttClient.java          # 커스텀 STT 서버 통신 (HttpClient + multipart)
│   └─ SttResponse.java        # API 응답 JSON 파싱 DTO (Fallback 포함)
├─ audio/
│   ├─ AudioDeviceManager.java # 마이크 장치 목록 스캔
│   └─ AudioRecorder.java      # 실시간 음성 녹음 → .wav 저장
├─ common/
│   ├─ ApiConfig.java          # config.properties에서 API 주소 로드
│   ├─ ApiException.java       # 전용 RuntimeException
│   ├─ TextSegment.java        # 시간대별 발화 내용 + 화자 정보 DTO
│   └─ TranscriptResult.java   # 최종 변환 결과 불변 객체
├─ db/
│   ├─ DatabaseManager.java    # SQLite DB 생성 및 스키마 초기화
│   └─ TranscriptDao.java      # DB CRUD (저장/조회/삭제)
├─ service/
│   └─ TranscriptionService.java # STT 호출 + DB 저장 통합 서비스
└─ ui/
    ├─ MainFrame.java          # 메인 윈도우 (녹음/변환/기록목록/결과출력/삭제)
    └─ SettingsDialog.java     # 마이크 선택 팝업창
```

---

# 🔜 앞으로 남은 일 (2주차 잔여 + 3주차)

## 2주차 잔여 (06/04~06/07)
| **작업** | **요구사항** | **상태** |
|---|---|---|
| .txt 파일 다운로드 | F-05 | ⬜ |
| 녹음 일시정지/재개 | F-10 | ⬜ |
| 변환 취소 | F-11 | ⬜ |
| 녹음 시간 표시 + 오디오 레벨 미터 | F-03, F-09b | ⬜ |
| API 주소 입력 UI (설정 창) | F-20 | ⬜ |

## 3주차 (06/08~06/14)
| **작업** | **요구사항** | **상태** |
|---|---|---|
| 시스템 음성 캡처 (WASAPI loopback) | F-01 | ⬜ |
| 마이크+시스템 동시 녹음, 채널 라벨 | F-01, F-14 | ⬜ |
| LLM 요약·맞춤법·키워드 (HttpClient 재사용) | F-07 | ⬜ |
| 다국어 옵션 | F-08 | ⬜ |
| API 재시도·타임아웃 안정화 + 단위 테스트 | - | ⬜ |
| 검색·필터, 정렬, 결과 편집 | F-15, F-16, F-12 | ⬜ |
| docx/srt 다운로드 | F-13 | ⬜ |
| 미리듣기 | F-18 | ⬜ |
| 릴리스 빌드·GitHub Releases 배포 | - | ⬜ |
