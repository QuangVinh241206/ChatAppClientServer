package com.chatapp.common;

import javax.sound.sampled.*;
import java.io.*;

/**
 * Tiện ích ghi âm và phát lại âm thanh sử dụng Java Sound API tiêu chuẩn (javax.sound.sampled)
 * Không cần bất kỳ thư viện ngoài (native C/C++) nào, chạy mượt mà trên mọi hệ điều hành.
 */
public class AudioHelper {

    // Định dạng âm thanh chuẩn: 16000Hz, 16bit, Mono (1 kênh), Signed, Little Endian
    public static AudioFormat getStandardFormat() {
        return new AudioFormat(16000.0f, 16, 1, true, false);
    }

    /**
     * Lớp hỗ trợ ghi âm trực tiếp từ Microphone
     */
    public static class AudioRecorder {
        private TargetDataLine line;
        private Thread recordThread;
        private ByteArrayOutputStream recordingStream;
        private volatile boolean isRecording = false;

        public boolean startRecording() {
            try {
                AudioFormat format = getStandardFormat();
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Microphone format not supported!");
                    return false;
                }

                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                isRecording = true;
                recordingStream = new ByteArrayOutputStream();

                recordThread = new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    while (isRecording) {
                        int count = line.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            recordingStream.write(buffer, 0, count);
                        }
                    }
                }, "Audio-Recorder-Thread");
                recordThread.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Dừng ghi âm và trả về mảng byte định dạng chuẩn .WAV
         */
        public byte[] stopRecording() {
            if (!isRecording) return null;
            isRecording = false;
            try {
                if (line != null) {
                    line.stop();
                    line.close();
                }
                if (recordThread != null) {
                    recordThread.join(1000);
                }

                byte[] audioRaw = recordingStream.toByteArray();
                if (audioRaw.length == 0) return null;

                // Đóng gói mảng raw PCM thành file WAV hoàn chỉnh có WAV header
                ByteArrayInputStream bais = new ByteArrayInputStream(audioRaw);
                AudioFormat format = getStandardFormat();
                AudioInputStream audioStream = new AudioInputStream(bais, format, audioRaw.length / format.getFrameSize());

                ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavOut);
                return wavOut.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean isRecording() {
            return isRecording;
        }
    }

    /**
     * Phát đoạn âm thanh từ mảng byte (file WAV)
     */
    public static class AudioPlayer {
        private Clip clip;

        public void play(byte[] wavBytes, Runnable onFinished) {
            stop();
            new Thread(() -> {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(wavBytes);
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(bais);

                    clip = AudioSystem.getClip();
                    clip.open(audioStream);

                    if (onFinished != null) {
                        clip.addLineListener(event -> {
                            if (event.getType() == LineEvent.Type.STOP) {
                                onFinished.run();
                            }
                        });
                    }

                    clip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (onFinished != null) onFinished.run();
                }
            }, "Audio-Player-Thread").start();
        }

        public void stop() {
            try {
                if (clip != null && clip.isRunning()) {
                    clip.stop();
                    clip.close();
                }
            } catch (Exception ignored) {}
        }

        public boolean isPlaying() {
            return clip != null && clip.isRunning();
        }
    }
}
