import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.ConvertOptions;
import com.api2convert.http.Config;
import java.util.Map;

/**
 * Guide: Audio Operations — transcode audio and set codec/bitrate/channels.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" AudioOperations
 * </pre>
 */
public final class AudioOperations {

    private static final String REMOTE_WAV =
            "https://example-files.online-convert.com/audio/wav/example.wav";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        ConversionResult result = client.convert(REMOTE_WAV, "aac",
                Map.of(
                        "audio_codec", "aac",
                        "audio_bitrate", 192,
                        "channels", "stereo",
                        "frequency", 44100),
                new ConvertOptions().category("audio"));

        System.out.println("Saved: " + result.save("./"));
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private AudioOperations() {
    }
}
