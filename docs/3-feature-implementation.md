# 3. 기능 구현

SpeechNote의 요구사항(F-01 ~ F-20)별 구현 현황과 내용을 정리한다.

## 3.1 기능 구현 현황

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
| --- | --- | --- | --- |
| F-01 | 실시간 녹음·입력 장치 선택 | `src/main/java/audio/AudioRecorder.java`, `src/main/java/audio/AudioDeviceManager.java` | ✅ 완성 |
| F-02 | 로컬 오디오 파일 업로드 | `src/main/java/ui/MainFrame.java`, `src/main/java/service/TranscriptionService.java` | ✅ 완성 |
| F-03 | 커스텀 STT API 통신 모듈 | `src/main/java/api/SttClient.java`, `src/main/java/api/SttResponse.java` | ✅ 완성 |
| F-04 | 설정·구성 관리 | `src/main/java/common/ApiConfig.java`, `src/main/java/ui/SettingsDialog.java` | ✅ 완성 |
| F-05 | 비동기 처리·프로그레스 바 | `src/main/java/service/TranscriptionService.java`, `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-06 | 회원가입·로그인 | `src/main/java/service/AuthService.java`, `src/main/java/ui/LoginFrame.java` | ✅ 완성 |
| F-07 | 로컬 SQLite DB 연동 | `src/main/java/db/DatabaseManager.java`, `src/main/java/db/TranscriptDao.java` | ✅ 완성 |
| F-08 | 좌측 히스토리 패널 | `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-09 | 우클릭 컨텍스트 메뉴 | `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-10 | TXT 내보내기 | `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-11 | DOCX·SRT 내보내기 | `src/main/java/service/ExportService.java` | ✅ 완성 |
| F-12 | STT 원문 편집 | `src/main/java/ui/MainFrame.java`, `src/main/java/db/TranscriptDao.java` | ✅ 완성 |
| F-13 | 핵심 요약 | `src/main/java/api/LlmClient.java`, `src/main/java/service/TranscriptionService.java` | ✅ 완성 |
| F-14 | 주요 키워드 추출 | `src/main/java/api/LlmClient.java` | ✅ 완성 |
| F-15 | 실시간 통합 검색 | `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-16 | 목록 정렬 | `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-17 | 보안·계정 관리 | `src/main/java/service/AuthService.java`, `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-18 | 오디오 재생 미니 플레이어 | `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-19 | 전역 에러 로깅 | `src/main/java/common/LoggerUtil.java`, `src/main/java/ui/MainFrame.java` | ✅ 완성 |
| F-20 | Two-Split 레이아웃 UI | `src/main/java/ui/MainFrame.java` | ✅ 완성 |

### 구현 여부 기호 안내
- ✅ **완성**: 요구사항을 완전히 충족하는 기능이 구현되어 정상 동작함
- 🔶 **부분완성**: 일부 기능은 동작하나 요구사항을 완전히 충족하지 못함
- ❌ **미완성**: 구현되지 않았거나 전혀 동작하지 않음

## 3.2 구현 내용 설명

**F-01 · 실시간 녹음·입력 장치 선택** (✅, `audio/AudioRecorder.java`)
마이크·시스템 두 라인을 동시에 읽어 2채널 인터리브로 저장하고 16kHz WAV로 변환한다. 설정 창에서 장치를 선택하고 RMS 기반 입력 레벨을 실시간 표시한다.

**F-02 · 로컬 오디오 파일 업로드** (✅, `ui/MainFrame.java`)
`JFileChooser`로 오디오 파일을 선택해 변환 대상으로 지정한다.

**F-03 · 커스텀 STT API 통신 모듈** (✅, `api/SttClient.java`)
multipart로 음성 파일을 STT 서버에 전송하고, 재시도·타임아웃을 적용해 응답을 `SttResponse`로 파싱한다(깨진 JSON fallback 포함).

**F-04 · 설정·구성 관리** (✅, `common/ApiConfig.java`)
`config.properties`에서 API URL·키를 읽고 환경설정 창에서 저장한다.

**F-05 · 비동기 처리·프로그레스 바** (✅, `service/TranscriptionService.java`)
`CompletableFuture`/`SwingWorker`로 변환을 백그라운드 처리하고 진행바·상태 메시지로 표시한다.

**F-06 · 회원가입·로그인** (✅, `service/AuthService.java`)
SHA-256 해시 비밀번호로 가입·로그인하고 사용자별로 기록을 분리한다.

**F-07 · 로컬 SQLite DB 연동** (✅, `db/TranscriptDao.java`)
`transcripts`·`segments` 테이블에 트랜잭션으로 저장·조회·삭제(CASCADE)한다.

**F-08 · 좌측 히스토리 패널** (✅, `ui/MainFrame.java`)
사용자의 변환 기록을 최신순 목록(아이콘·시간·미리보기)으로 표시한다.

**F-09 · 우클릭 컨텍스트 메뉴** (✅, `ui/MainFrame.java`)
목록 항목 우클릭으로 복사·TXT/SRT/DOCX 내보내기·삭제를 제공한다.

**F-10 · TXT 내보내기** (✅, `ui/MainFrame.java`)
STT 원문·메모·요약을 포함한 텍스트 파일로 저장한다.

**F-11 · DOCX·SRT 내보내기** (✅, `service/ExportService.java`)
SRT(타임코드·화자)와 DOCX(요약·키워드·본문) 포맷으로 내보낸다.

**F-12 · STT 원문 편집** (✅, `ui/MainFrame.java`)
편집 모드 토글로 원문을 수정해 `updateRawText`로 재저장한다.

**F-13 · 핵심 요약** (✅, `api/LlmClient.java`)
LLM으로 변환 텍스트를 3줄 이내로 요약한다.

**F-14 · 주요 키워드 추출** (✅, `api/LlmClient.java`)
동일 LLM 호출에서 핵심 키워드 5개를 추출한다.

**F-15 · 실시간 통합 검색** (✅, `ui/MainFrame.java`)
입력 즉시 메모·원문·요약·키워드를 통합 필터링한다.

**F-16 · 목록 정렬** (✅, `ui/MainFrame.java`)
최신순·제목(메모)순으로 기록을 정렬한다.

**F-17 · 보안·계정 관리** (✅, `service/AuthService.java`)
현재 비밀번호 확인 후 변경하고, 회원 탈퇴 시 사용자·기록을 CASCADE로 삭제한다.

**F-18 · 오디오 재생 미니 플레이어** (✅, `ui/MainFrame.java`)
저장된 녹음 오디오를 재생·정지한다(`javax.sound.sampled.Clip`).

**F-19 · 전역 에러 로깅** (✅, `common/LoggerUtil.java`)
오류를 `JOptionPane`으로 알리고 `logs/error.log`에 기록한다.

**F-20 · Two-Split 레이아웃 UI** (✅, `ui/MainFrame.java`)
좌(히스토리)·중(음성 기록 ↔ 개인 메모 Split)·우(AI 요약/키워드) 3분할과 역할별 상단 컨트롤을 구성한다.
