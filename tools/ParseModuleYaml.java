import java.nio.file.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import com.google.gson.*;

/** Parses supplied configuration as data. Never constructs arbitrary YAML classes. */
class ParseModuleYaml {
    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        JsonArray rows = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(20);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        int errors = 0;
        for (JsonElement e : rows) {
            JsonObject row = e.getAsJsonObject();
            try { row.add("data", gson.toJsonTree(yaml.load(Files.readString(Path.of(row.get("file").getAsString()))))); }
            catch (Exception ex) { row.addProperty("error", ex.getMessage()); errors++; }
        }
        Files.writeString(Path.of(args[1]), gson.toJson(rows));
        System.out.println("YAML files: " + rows.size() + ", errors: " + errors);
        if (errors != 0) System.exit(1);
    }
}
