package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model.DatagenDimensionList;
import com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model.DatagenDimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager of data produced by the Minecraft data generator which loads the generated data and keeps the copies of the
 *  data in-memory for lookup.
 *
 * Includes default world height settings for dimension types that are needed to properly parse and interpret chunk data.
 *
 * NOTE: the preload() method call is optional, but recommended in order to fail-fast in case the embedded data files
 *  are incorrect for some reason.  This also reduces the possible delay of reading and parsing the data files live when
 *  their contents are needed, and instead incurs that performance penalty at startup time.
 */
public class MinecraftDatagenManager {

    public static final String DIMENSION_TYPE_SUBDIR = "data/minecraft/dimension_type";
    public static final String DIMENSION_TYPE_LIST_RESOURCE = "dimension_type_list.json";

    public static final String BLOCK_ID_SUBDIR = "reports";
    public static final String BLOCK_ID_FILENAME = "blocks.json";
    public static final String REGISTRIES_SUBDIR = "reports";
    public static final String REGISTRIES_FILENAME = "registries.json";

    private static final Logger LOG = LoggerFactory.getLogger(MinecraftDatagenManager.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    private String datagenFolderPath;
    private DatagenDimensionList datagenDimensionList;

    private BlockIdMapper blockIdMapper;
    private Object blockIdMapperLock = new Object();
    private Map<String, DatagenDimensionType> dimensionTypeMap = new ConcurrentHashMap<>();

//========================================
// Accessors
//----------------------------------------

    public String getDatagenFolderPath() {
        return datagenFolderPath;
    }

    public void setDatagenFolderPath(String datagenFolderPath) {
        this.datagenFolderPath = datagenFolderPath;
    }

//========================================
// Lifecycle
//----------------------------------------

    /**
     * Load at initialization time to get better fail-fast if there are problems.  There is still some attempt to lazy
     *  load later.
     *
     * @throws IOException
     */
    public void preload() throws IOException {
        // Load the dimension list
        if (datagenDimensionList == null) {
            this.loadDimensionTypeList();
        }

        LOG.info("Loading dimension types: dimension-type-list={}", this.datagenDimensionList);
        for (String dimensionTypeName : this.datagenDimensionList) {
            this.loadDimensionType(dimensionTypeName);
        }

        if (this.blockIdMapper == null) {
            this.loadBlockIdMapper();
        }
    }

//========================================
// API
//----------------------------------------

    public DatagenDimensionType readDimensionType(String dimensionName) throws IOException {
        DatagenDimensionType result = this.dimensionTypeMap.get(dimensionName);
        if (result == null) {
            // Try loading on-demand
            this.loadDimensionType(dimensionName);
        }

        return this.dimensionTypeMap.get(dimensionName);
    }

    /**
     * Obtain the blockIdMapper, initialing one now if not already done.
     * RECOMMENDATION: use the preload() method to fail fast and load this data early.
     *
     * @return the blockIdMapper, if one was successfully initialized; null otherwise.
     * @throws IOException if the blockIdMapper was not already loaded and there is an IOException on attemping to load it now.
     */
    public BlockIdMapper obtainBlockIdMapper() throws IOException {
        if (this.blockIdMapper == null) {
            this.loadBlockIdMapper();
        }

        return this.blockIdMapper;
    }

//========================================
// Internal Data Loaders
//----------------------------------------

    private void loadDimensionTypeList() throws IOException {
        LOG.info("Loading dimension type list from resource: {}/{}", DIMENSION_TYPE_SUBDIR, DIMENSION_TYPE_LIST_RESOURCE);
        try (InputStream dataGenInputStream = this.openDataFileWithResourceFallback(DIMENSION_TYPE_SUBDIR, DIMENSION_TYPE_LIST_RESOURCE)) {
            this.datagenDimensionList = this.objectMapper.readValue(dataGenInputStream, DatagenDimensionList.class);
        }
    }

    private void loadDimensionType(String dimensionName) throws IOException {
        String dimensionFilename = dimensionName + ".json";

        LOG.info("Loading dimension type: dimension-name={}; filename={}", dimensionName, dimensionFilename);
        try (InputStream dataGenInputStream = this.openDataFileWithResourceFallback(DIMENSION_TYPE_SUBDIR, dimensionFilename)) {
            DatagenDimensionType dimensionType = this.objectMapper.readValue(dataGenInputStream, DatagenDimensionType.class);
            this.dimensionTypeMap.put(dimensionName, dimensionType);

            LOG.info("Loaded dimension type: dimension-name={}; dimension-type={}", dimensionName, dimensionType);
        }
    }

    private void loadBlockIdMapper() throws IOException {
        BlockIdMapper candidate = new BlockIdMapper();

        synchronized (this.blockIdMapperLock) {
            if (this.blockIdMapper != null) {
                // This thread lost the race.  No need to load again.
                return;
            }

            LOG.info("Loading block id file: filename={}/{}", BLOCK_ID_SUBDIR, BLOCK_ID_FILENAME);
            try (InputStream blocksReportInputStream = this.openDataFileWithResourceFallback(BLOCK_ID_SUBDIR, BLOCK_ID_FILENAME)) {
                candidate.loadBlocksReportFile(blocksReportInputStream);
            }

            LOG.info("Loading registries file: filename={}/{}", REGISTRIES_SUBDIR, REGISTRIES_FILENAME);
            try (InputStream registriesReportInputStream = this.openDataFileWithResourceFallback(REGISTRIES_SUBDIR, REGISTRIES_FILENAME)) {
                candidate.loadRegistriesFile(registriesReportInputStream);
            }

            this.blockIdMapper = candidate;
        }
    }

//========================================
// Internal File/Resource Loading
//----------------------------------------

    /**
     * Open the data file with the given name, first checking for an override file, if an override data folder is known,
     *  then falling back to the built-in resource.  If the override file exists but cannot be loaded, an exception is
     *  thrown in order to "fail fast".
     *
     * @param filename
     * @return
     * @throws IOException
     */
    private InputStream openDataFileWithResourceFallback(String subdir, String filename) throws IOException {
        InputStream result;
        String relpath;

        if (subdir != null) {
            relpath = subdir + File.separator + filename;
        } else {
            relpath = filename;
        }

        result = this.openOptDataFile(relpath);

        if (result == null) {
            LOG.info("Did not load override file; loading from resource: filename={}", relpath);

            result = MinecraftDatagenManager.class.getResourceAsStream(relpath);

            if (result == null) {
                throw new IOException("Failed to locate resource: filename=" + relpath);
            }
        }

        return result;
    }

    /**
     *
     * @param filename
     * @return
     * @throws IOException
     */
    private InputStream openOptDataFile(String filename) throws IOException {
        InputStream result = null;

        if (this.datagenFolderPath != null) {
            File optFile = new File(this.datagenFolderPath, filename);

            try {
                result = new FileInputStream(optFile);

                LOG.info("Loading override data file: filename={}; file-path={}", filename, optFile);
            } catch (FileNotFoundException fnfExc) {
                // This is, unfortunately, a catch-all exception (boo).  Check if the file exists; if so, we rethrow the
                //  exception.  If not, we ignore.  Usually I never "check-then-open", or this one "open-then-check"
                //  because that creates race conditions.  But it's fine here.

                if (optFile.exists()) {
                    throw new IOException("Failed to open override data file even though it exists: filename=" + filename + "; file-path={}" + optFile, fnfExc);
                }

                // File does not exist; we can safely ignore - the override file is optional.
                LOG.info("Did not find override file; falling back to built-in resource: filename={}; file-path={}", filename, optFile, fnfExc);
            }
        }

        return result;
    }
}
