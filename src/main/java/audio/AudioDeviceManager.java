package audio;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

/**
 * 시스템에 연결된 오디오 장치를 조회하고 관리하는 유틸리티 클래스임.
 */
public class AudioDeviceManager {

    /**
     * 녹음이 가능한 입력 장치(마이크 및 시스템 오디오) 목록을 조회하여 반환함.
     * @return 입력 역할을 수행할 수 있는 Mixer.Info 리스트
     */
    public static List<Mixer.Info> getAudioInputs() {
        List<Mixer.Info> inputs = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                // TargetDataLine(마이크 입력 등)을 지원하는지 검사
                if (mixer.isLineSupported(new javax.sound.sampled.Line.Info(TargetDataLine.class))) {
                    inputs.add(mixerInfo);
                }
            } catch (Exception e) {
                // 특정 장치 접근 불가 시 무시하고 진행
            }
        }
        return inputs;
    }
}
