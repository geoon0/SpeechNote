package audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 선택된 마이크와 시스템 오디오를 통해 실시간으로 소리를 캡처하고 .wav 파일로 저장하는 클래스임.
 */
public class AudioRecorder {

    public enum State {
        IDLE, RECORDING, PAUSED, STOPPED
    }

    private TargetDataLine micLine;
    private TargetDataLine sysLine;
    private final AudioFormat monoFormat;
    private AudioFormat outputFormat;
    private Path recordedFilePath;
    private Thread recordingThread;
    
    private volatile State state = State.IDLE;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private volatile int currentLevel = 0; // 0 ~ 100 범위의 볼륨 레벨

    public AudioRecorder() {
        // 하드웨어에서 읽을 때는 항상 Mono 포맷을 사용함
        monoFormat = new AudioFormat(16000.0f, 16, 1, true, false);
    }

    /**
     * 지정된 마이크 및 시스템 오디오 장치를 사용하여 녹음을 시작함.
     * @param micInfo 사용할 마이크의 Mixer.Info (null 가능)
     * @param sysInfo 사용할 시스템 오디오의 Mixer.Info (null 가능)
     */
    public void startRecording(Mixer.Info micInfo, Mixer.Info sysInfo) throws LineUnavailableException, IOException {
        if (state == State.RECORDING || state == State.PAUSED) {
            return;
        }

        cancelRequested.set(false);
        currentLevel = 0;
        micLine = null;
        sysLine = null;

        // 저장될 'records' 폴더 생성
        Path recordsDir = Paths.get("records");
        if (!Files.exists(recordsDir)) {
            Files.createDirectories(recordsDir);
        }

        // 고유한 파일명 생성
        String timestamp = String.valueOf(System.currentTimeMillis());
        recordedFilePath = recordsDir.resolve("record_" + timestamp + ".wav");
        Path tempPcmPath = recordsDir.resolve("temp_" + timestamp + ".pcm");

        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, monoFormat);

        if (micInfo != null) {
            Mixer mixer = AudioSystem.getMixer(micInfo);
            micLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            micLine.open(monoFormat);
            micLine.start();
        }

        if (sysInfo != null) {
            Mixer mixer = AudioSystem.getMixer(sysInfo);
            sysLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            sysLine.open(monoFormat);
            sysLine.start();
        }

        int channels = (micLine != null && sysLine != null) ? 2 : 1;
        outputFormat = new AudioFormat(16000.0f, 16, channels, true, false);
        state = State.RECORDING;

        // 백그라운드 스레드에서 PCM 데이터 직접 읽기
        recordingThread = new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(tempPcmPath.toFile())) {
                byte[] micBuf = new byte[2048];
                byte[] sysBuf = new byte[2048];
                byte[] stereoBuf = new byte[4096];

                while (!cancelRequested.get() && state != State.STOPPED) {
                    int micCount = 0;
                    int sysCount = 0;

                    if (micLine != null) micCount = micLine.read(micBuf, 0, micBuf.length);
                    if (sysLine != null) sysCount = sysLine.read(sysBuf, 0, sysBuf.length);

                    int maxCount = Math.max(micCount, sysCount);
                    if (maxCount > 0) {
                        if (state == State.RECORDING) {
                            if (channels == 1) {
                                // 1채널일 경우 하나만 저장
                                if (micCount > 0) fos.write(micBuf, 0, micCount);
                                else if (sysCount > 0) fos.write(sysBuf, 0, sysCount);
                            } else {
                                // 2채널일 경우 마이크(Left), 시스템(Right)으로 교차 배치 (Interleave)
                                int outIdx = 0;
                                for (int i = 0; i < maxCount; i += 2) {
                                    // Left (Mic)
                                    if (i < micCount) {
                                        stereoBuf[outIdx++] = micBuf[i];
                                        stereoBuf[outIdx++] = micBuf[i+1];
                                    } else {
                                        stereoBuf[outIdx++] = 0;
                                        stereoBuf[outIdx++] = 0;
                                    }
                                    // Right (Sys)
                                    if (i < sysCount) {
                                        stereoBuf[outIdx++] = sysBuf[i];
                                        stereoBuf[outIdx++] = sysBuf[i+1];
                                    } else {
                                        stereoBuf[outIdx++] = 0;
                                        stereoBuf[outIdx++] = 0;
                                    }
                                }
                                fos.write(stereoBuf, 0, outIdx);
                            }
                        }
                        
                        // 실시간 볼륨 레벨(RMS) 계산 (Mic 기준으로 우선, 없으면 Sys 기준)
                        byte[] levelBuf = micCount > 0 ? micBuf : sysBuf;
                        int levelCount = micCount > 0 ? micCount : sysCount;
                        
                        long sumSquare = 0;
                        int sampleCount = levelCount / 2;
                        for (int i = 0; i < levelCount - 1; i += 2) {
                            short sample = (short) ((levelBuf[i + 1] << 8) | (levelBuf[i] & 0xFF));
                            sumSquare += sample * sample;
                        }
                        double rms = sampleCount > 0 ? Math.sqrt((double) sumSquare / sampleCount) : 0;
                        double db = rms > 0 ? 20.0 * Math.log10(rms / 32768.0) : -100.0;
                        
                        int level = (int) ((db + 60) * (100.0 / 60.0));
                        currentLevel = Math.max(0, Math.min(100, level));
                    } else {
                        currentLevel = 0;
                    }
                }
            } catch (IOException e) {
                System.err.println("오디오 캡처 중 예외 발생: " + e.getMessage());
            } finally {
                currentLevel = 0;
                if (micLine != null) { micLine.stop(); micLine.close(); }
                if (sysLine != null) { sysLine.stop(); sysLine.close(); }

                if (cancelRequested.get()) {
                    try { Files.deleteIfExists(tempPcmPath); } catch (IOException ignored) {}
                    state = State.IDLE;
                } else {
                    // PCM -> WAV 변환 (올바른 헤더 작성, outputFormat 사용)
                    try (FileInputStream fis = new FileInputStream(tempPcmPath.toFile());
                         AudioInputStream ais = new AudioInputStream(fis, outputFormat, tempPcmPath.toFile().length() / outputFormat.getFrameSize())) {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, recordedFilePath.toFile());
                    } catch (IOException e) {
                        System.err.println("WAV 변환 중 예외 발생: " + e.getMessage());
                    } finally {
                        try { Files.deleteIfExists(tempPcmPath); } catch (IOException ignored) {}
                        state = State.IDLE;
                    }
                }
            }
        });
        recordingThread.start();
    }

    public void pauseRecording() {
        if (state == State.RECORDING) {
            state = State.PAUSED;
        }
    }

    public void resumeRecording() {
        if (state == State.PAUSED) {
            state = State.RECORDING;
        }
    }

    public void cancelRecording() {
        if (state == State.RECORDING || state == State.PAUSED) {
            cancelRequested.set(true);
            waitForThread();
        }
    }

    public Path stopRecording() {
        if (state == State.RECORDING || state == State.PAUSED) {
            state = State.STOPPED;
            waitForThread();
        }
        return recordedFilePath;
    }

    private void waitForThread() {
        try {
            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.join(3000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public State getState() {
        return state;
    }
    
    public int getCurrentLevel() {
        return currentLevel;
    }
}
