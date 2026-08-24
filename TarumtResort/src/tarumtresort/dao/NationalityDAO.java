package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.utility.JsonFileHandler;

// Author: Chai Chee Tong

public class NationalityDAO {
    private final String FILE_NAME = "data/customNationalityList.json";

    public String[] loadCustomNationalities() {
        try {
            String[] loaded = JsonFileHandler.load(Path.of(FILE_NAME), String[].class);
            return loaded != null ? loaded : new String[0];
        } catch (IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
            return new String[0];
        }
    }

    public void saveCustomNationalities(String[] nationalities) {
        try {
            JsonFileHandler.save(nationalities, Path.of(FILE_NAME));
        } catch (IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}