# SpeechNote — AI 음성-텍스트 변환 데스크톱 앱

SpeechNote는 마이크와 시스템(스피커) 음성을 녹음하거나 오디오 파일을 업로드해 **STT API로 텍스트 변환**하고, **AI 요약·키워드 추출**까지 제공하는 Java Swing 기반 데스크톱 애플리케이션입니다.

## 1. 프로젝트 소개
- **목적**: 화상회의·온라인 강의처럼 컴퓨터로 진행되는 자리에서 마이크 음성과 스피커 출력 음성을 함께 기록·변환한다.
- **대상 사용자**: 강의·회의 내용을 텍스트로 빠르게 정리하려는 학생·직장인, 타이핑보다 음성이 편한 사용자.
- **개선점**: 특정 회의 플랫폼에 종속되지 않고, 마이크+시스템 음성을 동시에 기록하며, 결과를 사용자 PC에 저장해 자유롭게 가공할 수 있다.
- **기대 효과**: 외부 AI API 연동·오디오 캡처·DB 연동을 통합 경험하며 실사용 가능한 형태로 구현.

## 2. 기술 스택
| 구분 | 기술 |
| --- | --- |
| 언어 / 빌드 | Java 11, Maven (maven-shade-plugin) |
| UI | Swing, FlatLaf |
| 데이터베이스 | SQLite (sqlite-jdbc) |
| 외부 연동 | java.net.http.HttpClient, STT API, LLM API |
| 라이브러리 | org.json, Apache POI(DOCX) |
| 형상 관리 | Git / GitHub |

## 3. 주요 기능
- 🎙 **녹음**: 마이크 + 시스템 음성 동시 녹음, 입력 장치 선택, 실시간 레벨 미터, 일시정지/취소
- 📁 **파일 변환**: 오디오 파일 업로드 → STT 변환 (비동기 처리 + 진행 표시)
- ✨ **AI 어시스턴트**: 변환 텍스트 핵심 요약 및 키워드 추출
- 🗂 **기록 관리**: SQLite 저장, 좌측 히스토리 패널, 실시간 통합 검색·정렬, 우클릭 메뉴
- ✏️ **편집·내보내기**: STT 원문 편집, TXT/DOCX/SRT 내보내기, 녹음 오디오 재생
- 👤 **계정**: 회원가입·로그인(사용자별 기록 분리), 비밀번호 변경·탈퇴
- 🛡 **안정성**: 전역 에러 로깅(`logs/error.log`), 사용자 친화적 오류 알림

## 4. 실행 방법
```bash
# 1) 저장소 클론
git clone https://github.com/jeyman2003-debug/SpeechNote.git
cd SpeechNote

# 2) 빌드 (Uber-JAR 생성)
mvn clean package

# 3) 실행 (JRE 11 이상)
java -jar target/SpeechNote-1.0-SNAPSHOT.jar
```
- 최초 실행 시 생성되는 `config.properties` 또는 앱 내 **환경설정**에서 STT API URL·키를 입력합니다.
- 시스템(스피커) 음성을 캡처하려면 **VB-CABLE** 등 가상 오디오 장치를 설치한 뒤 환경설정에서 선택하세요(아래 PoC 참고).

## 5. 프로젝트 구조
```
SpeechNote/
├── src/main/java/
│   ├── SpeechNoteApp.java     # 진입점
│   ├── audio/                 # 녹음·입력 장치
│   ├── api/                   # STT/LLM API 클라이언트
│   ├── service/               # 변환·요약·내보내기·인증
│   ├── db/                    # SQLite 연동·DAO
│   ├── ui/                    # Swing 화면
│   └── common/                # 모델·설정·예외·로깅
├── docs/                      # 프로젝트 문서 (0~4, 9)
├── pom.xml
└── README.md
```

## 6. 개발자
| 이름 | 학번 | 역할 |
| --- | --- | --- |
| 민건영 | 20210598 | **전체개발** — 녹음·장치 선택, 파일 업로드, 커스텀 STT/LLM API 연동, 비동기 처리, SQLite DB, 히스토리/검색/정렬, 내보내기(TXT/DOCX/SRT), 원문 편집, 미니 플레이어, 에러 로깅, Two-Split UI, AI 요약·키워드 |

> 상세 문서는 [`docs/`](docs/) 폴더 참고 (0-개요 · 1-요구사항 · 2-구조 · 3-기능구현 · 4-요약 · 9-AI활용).

---

## 부록: 시스템 음성 캡처 PoC 결과 보고
**목적**: PC 스피커에서 출력되는 소리(유튜브·화상회의 등)를 캡처해 STT로 변환.

### 1차 시도 — Windows WASAPI Loopback (JNA)
- **결과: 보류** — `IAudioClient`·`IAudioCaptureClient` 등 COM 인터페이스를 JNA로 매핑·메모리 관리해야 해 단기 PoC에 부적합.

### 2차 시도 — 가상 오디오 케이블 (채택)
- **결과: 성공** — **VB-CABLE**로 시스템 소리를 가상 마이크로 라우팅 → `AudioRecorder`가 마이크처럼 인식해 캡처·변환.
- **제약**: 사용자가 가상 오디오 드라이버를 1회 수동 설치·설정해야 함.
