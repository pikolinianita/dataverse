package edu.harvard.iq.dataverse.engine.command.impl;

import com.google.common.base.Strings;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.batch.jobs.importer.filesystem.FileRecordWriter;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchRuntime;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.common.NullSafeJsonBuilder.jsonObjectBuilder;

@RequiredPermissions(Permission.EditDataset)
public class ImportFromFileSystemCommand extends AbstractCommand<JsonObject> {

    private static final Logger logger = Logger.getLogger(ImportFromFileSystemCommand.class.getName());

    final Dataset dataset;
    final String uploadFolder;
    final Long totalSize;
    final String mode;
    final ImportMode importMode;

    public ImportFromFileSystemCommand(DataverseRequest aRequest, Dataset theDataset, String theUploadFolder, Long theTotalSize, ImportMode theImportMode) {
        super(aRequest, theDataset);
        dataset = theDataset;
        uploadFolder = theUploadFolder;
        totalSize = theTotalSize;
        importMode = theImportMode;
        mode = theImportMode.toString();
    }

    @Override
    public JsonObject execute(CommandContext ctxt)  {
        JsonObjectBuilder bld = jsonObjectBuilder();
        /**
         * batch import as-individual-datafiles is disabled in this iteration;
         * only the import-as-a-package is allowed. -- L.A. Feb 2 2017
         */
        String fileMode = FileRecordWriter.FILE_MODE_PACKAGE_FILE;
        try {
            /**
             * Current constraints: 1. only supports merge and replace mode 2.
             * valid dataset 3. valid dataset directory 4. valid user & user has
             * edit dataset permission 5. only one dataset version 6. dataset
             * version is draft
             */
            if (!mode.equalsIgnoreCase("MERGE") && !mode.equalsIgnoreCase("REPLACE")) {
                String error = "Import mode: " + mode + " is not currently supported.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }
            if (!fileMode.equals(FileRecordWriter.FILE_MODE_INDIVIDUAL_FILES) && !fileMode.equals(FileRecordWriter.FILE_MODE_PACKAGE_FILE)) {
                String error = "File import mode: " + fileMode + " is not supported.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }
            File directory = new File(ctxt.systemConfig().getFilesDirectory()
                                              + File.separator + dataset.getAuthority() + File.separator + dataset.getIdentifier());
            if (!isValidDirectory(directory)) {
                String error = "Dataset directory is invalid. " + directory;
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

            if (Strings.isNullOrEmpty(uploadFolder)) {
                String error = "No uploadFolder specified";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

            File uploadDirectory = new File(ctxt.systemConfig().getFilesDirectory()
                                                    + File.separator + dataset.getAuthority() + File.separator + dataset.getIdentifier()
                                                    + File.separator + uploadFolder);
            if (!isValidDirectory(uploadDirectory)) {
                String error = "Upload folder is not a valid directory.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

            if (dataset.getVersions().size() != 1) {
                String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - Dataset has more than one version.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

            if (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) {
                String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - Dataset isn't in DRAFT mode.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

            try {
                long jid;
                Properties props = new Properties();
                props.setProperty("datasetId", dataset.getId().toString());
                props.setProperty("userId", getUser().getIdentifier().replace("@", ""));
                props.setProperty("mode", mode);
                props.setProperty("fileMode", fileMode);
                props.setProperty("uploadFolder", uploadFolder);
                if (totalSize != null && totalSize > 0) {
                    props.setProperty("totalSize", totalSize.toString());
                }
                JobOperator jo = BatchRuntime.getJobOperator();
                jid = jo.start("FileSystemImportJob", props);
                if (jid > 0) {
                    bld.add("executionId", jid).add("message", "FileSystemImportJob in progress");
                    return bld.build();
                } else {
                    String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId();
                    logger.info(error);
                    throw new CommandException(error, this);
                }

            } catch (JobStartException | JobSecurityException ex) {
                String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - " + ex.getMessage();
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

        } catch (Exception e) {
            bld.add("message", "Import Exception - " + e.getMessage());
            return bld.build();
        }
    }

    /**
     * Make sure the directory path is truly a directory, exists and we can read
     * it.
     *
     * @return isValid
     */
    private boolean isValidDirectory(File directory) {
        String path = directory.getAbsolutePath();
        if (!directory.exists()) {
            logger.log(Level.SEVERE, "Directory " + path + " does not exist.");
            return false;
        }
        if (!directory.isDirectory()) {
            logger.log(Level.SEVERE, path + " is not a directory.");
            return false;
        }
        if (!directory.canRead()) {
            logger.log(Level.SEVERE, "Unable to read files from directory " + path + ". Permission denied.");
            return false;
        }
        return true;
    }

}
