import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import com.api2convert.model.Preset;
import java.util.List;

/**
 * Guide: Presets — list saved conversion presets (reusable target + options).
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" Presets
 * </pre>
 */
public final class Presets {

    public static void main(String[] args) {
        Api2Convert client = newClient();

        // All presets for the video->mp4 conversion (may be empty if you have saved none).
        List<Preset> presets = client.presets().list("video", "mp4", null);

        System.out.println("Presets: " + presets.size());
        for (Preset preset : presets) {
            System.out.println("- " + preset.name() + " -> " + preset.target());
        }
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private Presets() {
    }
}
