# SpeechNote - 데스크톱 STT 변환기

SpeechNote는 마이크 및 시스템 오디오를 실시간으로 녹음하고, Whisper 호환 STT API를 통해 텍스트로 변환하여 관리할 수 있는 Java Swing 기반 데스크톱 애플리케이션입니다.

---

## 🎧 시스템 음성 캡처 PoC (Proof of Concept) 결과 보고

**목적**: PC 스피커에서 출력되는 소리(유튜브, 화상회의 등)를 내부적으로 캡처하여 STT로 변환하기 위함.

### 1. 1차 시도 (Windows WASAPI Loopback via JNA)
- **접근**: 순수 Java 환경에서 `JNA`를 이용해 Windows 핵심 오디오 API인 WASAPI의 루프백(Loopback) 캡처를 시도.
- **결과**: **보류 (Too Complex)**
- **사유**: `IAudioClient`, `IAudioCaptureClient` 등 복잡한 COM(Component Object Model) 인터페이스를 JNA로 전부 매핑하고 메모리를 직접 관리해야 하므로 단기 PoC 목적에 맞지 않으며, 써드파티 라이브러리(CoreAudio4J 등) 없이는 안정성 확보가 매우 어렵습니다.

### 2. 2차 시도 (폴백 방안: 가상 오디오 케이블 활용)
- **접근**: **VB-CABLE** 등 무료 가상 오디오 장치 드라이버를 설치 후, 시스템 소리 출력을 가상 케이블 입력으로 라우팅하여 기존 `AudioRecorder`가 이를 마이크처럼 인식하여 녹음하도록 우회.
- **결과**: **성공 (우회 경로 채택)**
- **요약**: 
  1. 운영체제의 시스템 소리를 가상 마이크(Virtual Cable)로 리다이렉트 성공.
  2. SpeechNote의 설정 창에서 해당 가상 마이크를 선택하면 시스템 소리가 완벽하게 캡처됨.
  3. 캡처된 시스템 소리는 기존 `AudioRecorder`의 16kHz 정규화를 거쳐 STT 변환이 정상 작동함을 확인.
  4. 복잡한 JNA 개발 없이 즉시 안정적인 서비스 제공이 가능함.
  5. **제약 사항**: 사용자가 PC에 가상 오디오 드라이버(VB-CABLE 등)를 1회 수동으로 설치하고 설정해야 하는 진입 장벽 존재.