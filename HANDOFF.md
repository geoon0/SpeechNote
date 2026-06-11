# SpeechNote — 작업 핸드오프 문서

> 다른 AI/개발자가 이어서 작업할 수 있도록, 2026-06-11 세션(Claude Code)에서 한 모든 작업·결정·현재 상태·남은 일을 정리한 문서입니다.

---

## 0. TL;DR (현재 상태 한눈에)
- **코드 정리 + 상단 UI 재배치**: ✅ 커밋·푸시 **완료** (`origin/main`, 최신 커밋 `67d0f06`, 파일별 23커밋)
- **제출 문서(docs/0~4·9) + README 재작성**: ✅ 작성 **완료**, ❗**아직 커밋 안 됨 (working tree에만 있음)**
- **요구사항 F-번호 체계**: Notion "1.3 STT API" 기준으로 **확정(정본)** → 실제 구현과 1:1 일치, **구현율 100%**
- **바로 할 일**: ① docs+README 커밋·푸시 ② 구식 문서 2개 삭제

---

## 1. 프로젝트 개요
- **SpeechNote**: 마이크+시스템(스피커) 음성을 녹음하거나 오디오 파일을 업로드 → **STT API로 텍스트 변환** → **AI 요약·키워드**까지 제공하는 **Java 11 Swing 데스크톱 앱**.
- **기술 스택**: Java 11, Maven(maven-shade-plugin, Uber-JAR), Swing/FlatLaf, SQLite(sqlite-jdbc), org.json, Apache POI(DOCX), java.net.http.HttpClient
- **저장소**: https://github.com/jeyman2003-debug/SpeechNote (`origin/main`)
- **로컬 git 사용자**: `geoon0` (= 민건영). 실제로는 민건영이 거의 전 패키지를 작성(git authorship 확인).
- **패키지 구조**: `audio`(녹음·장치) / `api`(STT·LLM 클라이언트) / `service`(변환·내보내기·인증) / `db`(SQLite·DAO) / `ui`(Swing) / `common`(모델·설정·예외·로깅). 진입점 `SpeechNoteApp.java`.

---

## 2. 반드시 지켜야 할 제약 (과제 지침)
- **진행 지침**: https://andy-yun.github.io/Introduction2ProjectDocument/ + 제출 양식 PDF(`project-documentation-guide`)
- 핵심 규칙:
  - 커밋 메시지 `<type>: <subject>` (feat/fix/docs/style/refactor/test/chore)
  - `.gitignore`로 빌드물·비밀정보(config.properties)·IDE 설정 제외
  - `docs/`에 **0~4 필수 + 9 필수**, 5~8 선택. (3=feature-implementation, 4=summary 양식 PDF에 명시)
  - README 필수 섹션(프로젝트명·소개·팀원표·기술스택·주요기능·실행방법·구조 트리)
  - JUnit 단위 테스트, GitHub Issues/Milestone로 F-01~20 추적, Releases(SemVer) + 교수 `andy-yun`을 **Read collaborator** 등록

---

## 3. 이번 세션에서 한 일

### 3.1 코드 정리 — ✅ 커밋·푸시 완료 (`origin/main` 67d0f06)
- **`.gitignore` 재작성**: 깨진 UTF-16(→`c o n f i g...`로 보였음) → UTF-8 정상화. `config.properties`(API키)·`*.class`·`*.jar`·`logs/`·`.idea/` 등 제외.
- **소스 트리 `.class` 6개 삭제** (빌드 산출물이 `src/`에 섞여 있던 것).
- **클래스 상단 주석 보완**: `AuthService`, `LoginFrame`, `LoggerUtil`.
- **콘솔 디버그 출력 → `common.LoggerUtil` 정리**: `DatabaseManager`·`AudioRecorder`·`ApiConfig`·`TranscriptionService`·`MainFrame`. (단, `Demo.java`는 콘솔 데모라, `LoggerUtil` 부트스트랩 1곳은 로거 자기 자신이라 **의도적으로 유지**.)
- **`MainFrame` 상단 컨트롤 UI 역할별 재배치**: 계정 메뉴(👤)를 우상단으로, `[🎙 녹음]`·`[📁 파일 변환]` 제목 그룹 박스로 분리, 잡다한 `[ ] |` 라벨 제거, 버튼 9개 툴팁 추가.
- **컴파일 검증**: `javac --release 11` + `~/.m2` 클래스패스로 전체 빌드 → 에러 0.
- **파일별 23커밋 + push** (`.idea` 추적 해제 포함).

### 3.2 제출 문서 — ✅ 작성 완료, ❗미커밋 (working tree)
- `docs/0-project-overview.md` — 소개·범위·팀(학번 포함)·GitHub
- `docs/1-requirement-analysis.md` — F-01~20 표·비기능 요구사항·아키텍처·**ERD(실제 DB 스키마로 수정)**·Issues
- `docs/2-project-structure.md` — 디렉토리/패키지·분야 매핑·역할 분담·WBS 일정
- `docs/3-feature-implementation.md` — F-01~20 구현 현황 표 + 설명 (**전부 ✅ 완성**)
- `docs/4-summary.md` — **20/20 = 구현율 100%** + 회고(실시간 변환·로컬 DB) + 팀원 기여
- `docs/9-AI-prompts.md` — AI(Claude Code) 사용 기록
- `README.md` **재작성** — 기존 좋은 버전(커밋 03208f5) 알맹이 복원 + **주요 기능·실행 방법·프로젝트 구조 트리** 추가 + 팀표(학번) + 시스템 음성 캡처 PoC를 부록으로 보존. (그 사이 커밋 3fb9920에서 README가 PoC만 남기고 잘려 있었음.)

---

## 4. 핵심 결정 사항
- **F-번호 정본 = Notion "1.3 STT API"** (사용자가 "이게 맞는 내용"이라 확인). 실제 구현 기능과 1:1 → **20/20 ✅, 100%**.
  - 주의: 기존 `docs/Project_Architecture_and_Schedule.md`는 **초기 기획본**이라 F-번호 의미가 다름(예: 그쪽 F-07=요약, Notion F-07=SQLite DB). **역할 분담(누가 무엇)은 두 기준 동일**, F-번호 라벨만 다름. → 구식 문서는 폐기 대상.
- **역할 분담**: 민건영=**입력·오디오·API**, 정의영(팀장)=**UI·DB·결과·관리**.
- **학번**: 정의영 `20220505` / 민건영 `20210598` (git 이메일 `20210598@gm.hannam.ac.kr`로 교차 확인).

---

## 5. 정본 데이터 — 요구사항 F-01~F-20 (Notion 기준)

| F | 기능 | 담당 | 구현 | 주요 소스 |
| --- | --- | --- | --- | --- |
| F-01 | 실시간 녹음·입력 장치 선택 | 민건영 | ✅ | audio/AudioRecorder.java, AudioDeviceManager.java |
| F-02 | 로컬 오디오 파일 업로드 | 민건영 | ✅ | ui/MainFrame.java, service/TranscriptionService.java |
| F-03 | 커스텀 STT API 통신 모듈 | 민건영 | ✅ | api/SttClient.java, SttResponse.java |
| F-04 | 설정·구성 관리 | 민건영 | ✅ | common/ApiConfig.java, ui/SettingsDialog.java |
| F-05 | 비동기 처리·프로그레스 바 | 민건영 | ✅ | service/TranscriptionService.java, ui/MainFrame.java |
| F-06 | 회원가입·로그인 | 정의영 | ✅ | service/AuthService.java, ui/LoginFrame.java |
| F-07 | 로컬 SQLite DB 연동 | 정의영 | ✅ | db/DatabaseManager.java, TranscriptDao.java |
| F-08 | 좌측 히스토리 패널 | 정의영 | ✅ | ui/MainFrame.java |
| F-09 | 우클릭 컨텍스트 메뉴 | 정의영 | ✅ | ui/MainFrame.java |
| F-10 | TXT 내보내기 | 정의영 | ✅ | ui/MainFrame.java |
| F-11 | DOCX·SRT 내보내기 | 정의영 | ✅ | service/ExportService.java |
| F-12 | STT 원문 편집 | 정의영 | ✅ | ui/MainFrame.java, db/TranscriptDao.java |
| F-13 | 핵심 요약 | 민건영 | ✅ | api/LlmClient.java, service/TranscriptionService.java |
| F-14 | 주요 키워드 추출 | 민건영 | ✅ | api/LlmClient.java |
| F-15 | 실시간 통합 검색 | 정의영 | ✅ | ui/MainFrame.java |
| F-16 | 목록 정렬 | 정의영 | ✅ | ui/MainFrame.java |
| F-17 | 보안·계정 관리(비번 변경·탈퇴) | 정의영 | ✅ | service/AuthService.java, ui/MainFrame.java |
| F-18 | 오디오 재생 미니 플레이어 | 정의영 | ✅* | ui/MainFrame.java |
| F-19 | 전역 에러 로깅 | 정의영 | ✅ | common/LoggerUtil.java, ui/MainFrame.java |
| F-20 | Two-Split 레이아웃 UI | 정의영 | ✅ | ui/MainFrame.java |

\* F-18은 재생·정지까지만 구현(일시정지·탐색 없음). "미니 플레이어"로 ✅ 처리했으나, 엄격 기준이면 🔶로 내릴지 검토.

---

## 6. 남은 작업 (다음 차례)
1. ❗ **docs/0~4·9 + README.md 커밋·푸시** (현재 미커밋). 파일별 커밋 컨벤션 예: `docs: 3-feature-implementation.md - 기능 구현 현황·설명`
2. **구식 문서 삭제**: `docs/Project_Architecture_and_Schedule.md`(옛 F-번호 → 새 문서와 충돌), `docs/README.md`(0번에 흡수). 내용은 0~4번에 모두 반영됨 → 삭제 권장.
3. 지침 잔여(제출 전): **JUnit 단위 테스트** 추가, GitHub **Issues/Milestone**로 F-01~20 등록, **Releases**(v1.0.0, SemVer), 교수 `andy-yun`을 **Read collaborator** 등록.
4. (보류 지시) `ApiConfig.DEFAULT_STT_URL`의 ngrok 임시 주소 → 추후 고정 주소로 교체 예정. 지금은 건드리지 말 것.

---

## 7. 환경·도구 메모 (gotchas)
- **Maven이 PATH에 없음.** 빌드 검증은 JDK의 `javac`로: `javac --release 11 -encoding UTF-8 -cp "<~/.m2/repository 하위 모든 jar ;로 연결>" -d <out> <src/main/java 전체 .java>`. (JDK: Adoptium 25)
- **한글 커밋 메시지**는 PowerShell 5.1에서 깨질 수 있음 → **Bash 툴**로 커밋(UTF-8 그대로 전달됨).
- **파일별 커밋 시 삭제 처리**: 수정/추가는 `git commit -m "..." -- <경로>` 가능하지만, **이미 삭제된 파일은 그 방식이 안 됨**("nothing to commit"). 삭제는 `git rm <경로>` 후 커밋하거나, 인덱스에 삭제만 staged된 상태에서 경로 없이 `git commit`.
- 이 세션의 메모리는 프로젝트 밖(`~/.claude/.../memory/`)에 저장됨 — git과 무관.
- 빌드/실행: `mvn clean package` → `java -jar target/SpeechNote-1.0-SNAPSHOT.jar` (JRE 11+). 시스템 음성 캡처는 VB-CABLE 등 가상 장치 필요.
