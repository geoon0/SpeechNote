package audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 선택된 마이크를 통해 실시간으로 소리를 캡처하고 .wav 파일로 저장하는 클래스임.
 */
public class AudioRecorder {

    private TargetDataLine targetDataLine;
    private final AudioFormat audioFormat;
    private Path recordedFilePath;
    private Thread recordingThread;
    private boolean isRecording = false;

    public AudioRecorder() {
        // 음성 인식 API가 권장하는 표준 포맷 (16kHz, 16bit, Mono, Signed, Little Endian)
        audioFormat = new AudioFormat(16000.0f, 16, 1, true, false);
    }

    /**
     * 지정된 마이크 장치를 사용하여 녹음을 시작함.
     * @param mixerInfo 사용할 마이크의 Mixer.Info
     * @throws LineUnavailableException 장치를 사용할 수 없을 때
     * @throws IOException 디렉토리 생성 실패 등 파일 입출력 예외
     */
    public void start(Mixer.Info mixerInfo) throws LineUnavailableException, IOException {
        if (isRecording) {
            return;
        }

        // 저장될 'records' 폴더 생성
        Path recordsDir = Paths.get("records");
        if (!Files.exists(recordsDir)) {
            Files.createDirectories(recordsDir);
        }

        // 고유한 파일명 생성
        recordedFilePath = recordsDir.resolve("record_" + System.currentTimeMillis() + ".wav");
        File outputFile = recordedFilePath.toFile();

        // 선택된 마이크 장치로부터 라인 확보
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);

        // 녹음 시작
        targetDataLine.open(audioFormat);
        targetDataLine.start();
        isRecording = true;

        // 백그라운드 스레드에서 파일로 오디오 데이터 스트리밍 기록
        recordingThread = new Thread(() -> {
            try (AudioInputStream audioInputStream = new AudioInputStream(targetDataLine)) {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
            } catch (IOException e) {
                // 스트림 종료 시 자연스럽게 예외가 발생할 수 있으므로 콘솔 출력만 수행
                System.err.println("오디오 저장 중 예외 발생: " + e.getMessage());
            }
        });
        recordingThread.start();
    }

    /**
     * 녹음을 중지하고 생성된 파일의 경로를 반환함.
     * @return 녹음된 .wav 파일의 Path
     */
    public Path stop() {
        if (!isRecording) {
            return recordedFilePath;
        }
        
        isRecording = false;
        targetDataLine.stop();
        targetDataLine.close();

        // 쓰기 스레드가 정상 종료될 때까지 대기
        try {
            if (recordingThread != null) {
                recordingThread.join(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return recordedFilePath;
    }

    public boolean isRecording() {
        return isRecording;
    }
}
