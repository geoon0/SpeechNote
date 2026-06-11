# 2. 프로젝트 구조

## 2.1 디렉토리 / 패키지 구조
```
SpeechNote/
├── src/main/java/
│   ├── SpeechNoteApp.java       # 애플리케이션 진입점 (테마·DB 초기화·로그인)
│   ├── Demo.java                # STT 연동 검증용 콘솔 데모
│   ├── audio/                   # 오디오 캡처·입력 장치
│   │   ├── AudioRecorder.java
│   │   └── AudioDeviceManager.java
│   ├── api/                     # 외부 API 클라이언트
│   │   ├── SttClient.java
│   │   ├── SttResponse.java
│   │   └── LlmClient.java
│   ├── service/                 # 비즈니스 로직
│   │   ├── TranscriptionService.java
│   │   ├── ExportService.java
│   │   └── AuthService.java
│   ├── db/                      # 데이터베이스 접근
│   │   ├── DatabaseManager.java
│   │   └── TranscriptDao.java
│   ├── ui/                      # Swing 화면
│   │   ├── MainFrame.java
│   │   ├── LoginFrame.java
│   │   └── SettingsDialog.java
│   └── common/                  # 공통 모델·설정·예외·로깅
│       ├── TranscriptResult.java
│       ├── TextSegment.java
│       ├── ApiConfig.java
│       ├── ApiException.java
│       └── LoggerUtil.java
├── docs/                        # 프로젝트 문서 (0~4, 9)
├── pom.xml                      # Maven 의존성·빌드 설정
├── README.md
└── .gitignore
```

## 2.2 패키지 구성 (분야 ↔ 요구사항)

| 분야(패키지) | 포함 요구사항 | 핵심 기술 |
| --- | --- | --- |
| `audio` | F-01, F-02 | Java Sound API, 입력 장치 enumeration, 녹음 스레드, 채널 분리 |
| `api` | F-03, F-13, F-14 | HttpClient, STT/LLM API, 비동기 호출, 재시도 |
| `service` | F-05, F-06, F-10, F-11, F-17 | 변환 파이프라인, 인증, Apache POI(DOCX)·SRT 내보내기 |
| `db` | F-07, F-12, F-15, F-16 | SQLite + JDBC, DAO, 트랜잭션, CASCADE |
| `ui` | F-08, F-09, F-18, F-20 | Swing, SwingWorker, 히스토리 패널·컨텍스트 메뉴·Two-Split |
| `common` | F-04, F-19 | 설정 로딩, 도메인 모델, 예외, 로깅 |

## 2.3 역할 분담

| 담당자 | 담당 요구사항 | 책임 범위 |
| --- | --- | --- |
| 민건영 (입력·오디오·API) | F-01, F-02, F-03, F-04, F-05, F-13, F-14 | 실시간 녹음·장치 선택, 파일 업로드, 커스텀 STT API 통신, 설정 관리, 비동기·진행 표시, 요약·키워드 추출 |
| 정의영 (팀장, UI·DB·관리) | F-06 ~ F-12, F-15 ~ F-20 | 로그인·DB 연동, 히스토리 패널·검색·정렬, 내보내기, 원문 편집, 미니 플레이어, 에러 로깅, Two-Split UI |

- 두 사람의 인터페이스는 공통 도메인 객체 `TranscriptResult`. 의존성이 단방향이라 충돌이 적음.
- **협업 방안**: Teams·Notion으로 의견 정리 후, 확정본을 GitHub(브랜치→PR→리뷰→merge)로 반영.

## 2.4 일정 관리 (WBS)

| 단계 | 기간 | 민건영 (입력·API) | 정의영 (UI·저장·관리) | 산출물 |
| --- | --- | --- | --- | --- |
| 1주차 — 셋업 + MVP | 2026-05-25 ~ 05-31 | STT API 검증, 파일→STT 통신 모듈 | 패키지 구조, 메인 창, 진행 표시, DB 스키마 | 파일 넣고 텍스트 보기 |
| 2주차 — 녹음 + 저장 | 2026-06-01 ~ 06-07 | 장치 선택·실시간 녹음, 비동기 처리 | DB 저장·히스토리 패널, TXT 내보내기, 설정 화면 | 녹음→변환→저장 |
| 3주차 — AI·관리·마무리 | 2026-06-08 ~ 06-14 | 시스템 음성 캡처, 요약·키워드 | 검색·정렬, 편집, DOCX/SRT, 미니 플레이어, 로그인, Two-Split UI, 릴리스 | 최종 시연 + JAR 배포 |

### 중간 점검
- **1차(~05-31)**: STT 응답 수신·파일 변환 성공.
- **2차(~06-07)**: 녹음→변환→DB 저장→TXT 내보내기.
- **최종(~06-14)**: 시스템 음성·요약·자막 생성, 통합 점검, GitHub Releases 배포.
