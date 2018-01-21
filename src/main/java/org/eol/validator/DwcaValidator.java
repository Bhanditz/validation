package org.eol.validator;

import org.apache.commons.io.FileUtils;
import org.eol.handlers.DwcaHandler;
import org.eol.parser.utils.CommonTerms;
import org.eol.parser.utils.Constants;
import org.eol.validator.rules.FieldValidationRule;
import org.eol.validator.rules.MetaFileValidationRule;
import org.eol.validator.rules.RowValidationRule;
import org.eol.validator.rules.ValidationRulesLoader;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.Record;
import org.slf4j.LoggerFactory;
import sun.management.Agent;

import java.io.*;
import java.util.*;

public class DwcaValidator {

    //    private Logger logger;
    private ValidationRulesLoader rulesLoader;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DwcaValidator.class);
    private static int chunkSize = Constants.ChunkSize;

    /**
     * Construct new DwcaValidator, and load validation rules
     *
     * @throws Exception in case of failing in loading rules
     */
    public DwcaValidator(String propertiesFile) throws Exception {
//        LogHandler.initializeHandler(propertiesFile);
//        logger = LogHandler.getLogger(DwcaValidator.class.getName());
        rulesLoader = new ValidationRulesLoader(propertiesFile);
        if (!rulesLoader.loadValidationRules()) {
            throw new Exception("Failed to load the validation rules while creating new dwca " +
                    "validator");
        }
    }

    public ValidationResult validateArchive(String path, Archive dwcArchive) throws Exception {
        ValidationResult validationResult = new ValidationResult(path);
        if (!validateArchive(dwcArchive, validationResult)) {
            throw new Exception("Problem happened while trying to apply the validation rules on " +
                    "the archive : " + path);
        }
        return validationResult;
    }

    /**
     * Validate darwin core archive and put the results in the ValidationResult object
     *
     * @param dwca input darwin core archive
     * @return false in case of failure in applying any of the validation rules
     */
    public boolean validateArchive(Archive dwca, ValidationResult validationResult) {
        logger.info("Start applying the structuralValidationRules on archive " + dwca.getLocation
                () + " ...");
        if (!applyStructuralValidationRules(dwca, validationResult)) {
            logger.error("Failed in applying some Structural Validation rules");
            return false;
        }

        if (validationResult.getStructuralErrors().size() > 0) {
            logger.error("Dwca " + dwca.getLocation() + " does not pass some structural " +
                    "validation rules");
            return true;
        }

        boolean validationRunWithoutProblems = true;
        if (!applyValidationRules(dwca, validationResult)) {
            validationRunWithoutProblems = false;
        }
        return validationRunWithoutProblems;
    }

    /**
     * Apply the Structural Validation Rules on the darwin core archive and put the results in the
     * ValidationResult object
     *
     * @return false in case of failure in applying any of the validation rules
     */
    private boolean applyStructuralValidationRules(Archive dwca, ValidationResult
            validationResult) {
        List<MetaFileValidationRule> rules = rulesLoader.getMetaFileValidationRules();
        if (rules.isEmpty()) {
            copyMetaFile(dwca, "meta.xml");

            logger.info("No MetaFileValidationRules to apply");
            return true;
        }

        int success = 0;
        int failures = 0;
        for (MetaFileValidationRule rule : rules) {
            if (rule.validate(dwca, validationResult))
                success++;
            else
                failures++;
        }
        logger.info("Out of  " + rules.size() + " MetaFileValidationRules");
        logger.info("Successfully applied " + success + " MetaFileValidationRules");
        logger.info("Failed in applying " + failures + " MetaFileValidationRules");
        if (failures == 0)
            copyMetaFile(dwca, "meta.xml");

        return failures == 0;
    }

    private List<String> filterNotExistingRowTypes(Archive archive, List<String> rowTypeList) {
        logger.info("Prepare HashSet for the rowtypes of the archive");
        HashSet<String> archiveList = new HashSet<String>();
        archiveList.add(archive.getCore().getRowType().qualifiedName().toLowerCase());
        if (!rowTypeList.contains(archive.getCore().getRowType().qualifiedName().toLowerCase()))
            copyArchiveFile(archive.getCore());
        for (ArchiveFile archiveFile : archive.getExtensions()) {
            String archiveRowTyp = archiveFile.getRowType().qualifiedName().toLowerCase();
            archiveList.add(archiveRowTyp);
            if (!rowTypeList.contains(archiveRowTyp))
                copyArchiveFile(archiveFile);
        }
        logger.info("Using the HashSet in filtering the rowTypes");
        List<String> filteredList = new ArrayList<String>();
        for (String rowType : rowTypeList) {
            if (archiveList.contains(rowType.toLowerCase()))
                filteredList.add(rowType);
            else
                logger.error("RowType : " + rowType + "  is not found at the DwcArchive");
        }
        logger.info("Returning " + filteredList.size() + " rowType out of " + rowTypeList.size() + " rowtype");
        return filteredList;
    }

    private boolean applyValidationRules(Archive dwcArchive, ValidationResult validationResult) {
        List<String> rowTypeList = rulesLoader.getRowTypeList();
        if (rowTypeList.isEmpty()) {
            System.out.println("no rules");
            copyAllFiles(dwcArchive);
            logger.warn("Empty rowType list. No rowTypes have validation rules");
            return true;
        }

        rowTypeList = filterNotExistingRowTypes(dwcArchive, rowTypeList);
        boolean rowSuccess = true, fieldSuccess = true;

        for (String rowType : rowTypeList) {

            List<RowValidationRule> rowValidationRules = rulesLoader.getValidationRulesOfRowType(rowType);
            if (rowValidationRules.isEmpty()) {
                logger.info("Row type " + rowType + " has no row validation rules");
            }

            List<FieldValidationRule> fieldValidationRules = rulesLoader.getValidationRulesOfFieldType(rowType);
            if (rowValidationRules.isEmpty()) {
                logger.info("Row type " + rowType + " has no field validation rules");
            }

            ArrayList<ArchiveFile> archiveFiles;
            try {
                archiveFiles = DwcaHandler.getArchiveFile(dwcArchive, rowType);
            } catch (Exception e) {
//            logger.fatal("The specified rowtype : " + this.rowTypeURI + " is not found at the archive");
                return true;
            }
            for (ArchiveFile archiveFile : archiveFiles) {
                int totalLines = 0, chunks = 0;
                ArrayList<Record> recordsToValid = new ArrayList<Record>();
                for (Record record : archiveFile) {
                    if (totalLines % chunkSize == 0 && totalLines != 0) {
                        chunks++;
                        logger.info("start applying " + rowValidationRules.size() + " row Validations on archive file " + rowType + "on " + chunks + "chunk");
                        rowSuccess &= applyRowValidationRules(archiveFile, validationResult, rowType, rowValidationRules, recordsToValid);
                        logger.info("start applying " + rowValidationRules.size() + " field Validations on archive file " + rowType + "on " + chunks + "chunk");
                        fieldSuccess &= applyFieldValidationRules(archiveFile, validationResult, rowType, fieldValidationRules, recordsToValid);
                        if (Constants.copyContentOfArchiveFileToDisk(recordsToValid, archiveFile)) {
                            recordsToValid.clear();
                        }
                    }
                    totalLines++;
                    recordsToValid.add(record);
                }
                if (!recordsToValid.isEmpty()) {
                    chunks++;
                    logger.info("start applying " + rowValidationRules.size() + " row Validations on archive file " + rowType + "on " + chunks + "chunk");
                    rowSuccess &= applyRowValidationRules(archiveFile, validationResult, rowType, rowValidationRules, recordsToValid);
                    logger.info("start applying " + rowValidationRules.size() + " field Validations on archive file " + rowType + "on " + chunks + "chunk");
                    fieldSuccess &= applyFieldValidationRules(archiveFile, validationResult, rowType, fieldValidationRules, recordsToValid);
                    if (Constants.copyContentOfArchiveFileToDisk(recordsToValid, archiveFile)) {
                        recordsToValid.clear();
                    }
                }

            }


        }
        if (!rowSuccess)
            logger.error("Failed in applying some Row Validation rules");
        if (!fieldSuccess)
            logger.error("Failed in applying some Field Validation rules");
        return rowSuccess && fieldSuccess;
    }

    private void copyAllFiles(Archive dwcArchive) {
        Set<ArchiveFile> extensions = dwcArchive.getExtensions();
        ArchiveFile coreFile = dwcArchive.getCore();
        copyArchiveFile(coreFile);

        for (ArchiveFile archiveFile : extensions) {
            System.out.println(archiveFile.getTitle());
            copyArchiveFile(archiveFile);

        }

//        if (dwcArchive.getExtension(CommonTerms.referenceTerm) != null && !extensions.contains(dwcArchive.getExtension(CommonTerms.referenceTerm))) {
//            System.out.println("copy references");
//            copyArchiveFile(dwcArchive.getExtension(CommonTerms.referenceTerm));
//        }
//
//        if (dwcArchive.getExtension(CommonTerms.agentTerm) != null /*&& !extensions.contains(dwcArchive.getExtension(CommonTerms.agentTerm))*/) {
//            System.out.println("copy agents");
//            copyArchiveFile(dwcArchive.getExtension(CommonTerms.agentTerm));
//        }
//
//        if (dwcArchive.getExtension(CommonTerms.associationTerm) != null && !extensions.contains(dwcArchive.getExtension(CommonTerms.associationTerm))) {
//            System.out.println("copy associations");
//            copyArchiveFile(dwcArchive.getExtension(CommonTerms.associationTerm));
//        }
//
//        if (dwcArchive.getExtension(DwcTerm.MeasurementOrFact) != null && !extensions.contains(dwcArchive.getExtension(DwcTerm.MeasurementOrFact))) {
//            System.out.println("copy measurements");
//            copyArchiveFile(dwcArchive.getExtension(DwcTerm.MeasurementOrFact));
//        }
    }

    private void copyArchiveFile(ArchiveFile archiveFile) {
        System.out.println("copy " + archiveFile.getTitle());
        int totalLines = 0;
        ArrayList<Record> records = new ArrayList<Record>();
        for (Record record : archiveFile) {
            if (totalLines % chunkSize == 0 && totalLines != 0) {
                if (Constants.copyContentOfArchiveFileToDisk(records, archiveFile)) {
                    records.clear();
                }
            }
            totalLines++;
            records.add(record);
        }
        if (!records.isEmpty()) {
            if (Constants.copyContentOfArchiveFileToDisk(records, archiveFile)) {
                records.clear();
            }
        }
    }

    /**
     * Apply the Row Validation Rules on the darwin core archive and put the results in the
     * ValidationResult object
     *
     * @param archiveFile the input archive file
     * @return false in case of failure in applying any of the validation rules
     */
    private boolean applyRowValidationRules(ArchiveFile archiveFile, ValidationResult validationResult,
                                            String rowType, List<RowValidationRule> rules, ArrayList<Record> recordsToValid) {

        if (rules.isEmpty()) {
            return true;
        }
        int localSuccess = 0;
        int localFailures = 0;
        for (RowValidationRule rule : rules) {
            if (!rule.validate(archiveFile, validationResult, recordsToValid)) {
                localFailures++;
                logger.error("RowType : " + rowType + " , Failed in applying the following " +
                        "rule : " + rule.toString());
            } else
                localSuccess++;
        }

        if (localFailures == 0)
            logger.info("Row validation rules on the rowType " + rowType + " all run " +
                    "successfully");
        else
            logger.info("Row validation rules on the rowType " + rowType + " had problems. " +
                    "Failed in applying " + localFailures + " out of " + (localFailures +
                    localSuccess));

        return localFailures == 0;
    }

    /**
     * Apply the Field Validation Rules on the darwin core archive and put the results in the
     * ValidationResult object
     *
     * @param archiveFile the input archive file
     * @return false in case of failure in applying any of the validation rules
     */
    private boolean applyFieldValidationRules(ArchiveFile archiveFile, ValidationResult validationResult,
                                              String rowType, List<FieldValidationRule> rules, ArrayList<Record> recordsToValid) {


        if (rules.isEmpty()) {
            return true;
        }
        int localSuccess = 0;
        int localFailures = 0;


        for (FieldValidationRule rule : rules) {
            if (!rule.validate(archiveFile, validationResult, recordsToValid)) {
                localFailures++;
                logger.error("RowType : " + rowType + " , Failed in applying the following " +
                        "rule : " + rule.toString());
            } else
                localSuccess++;
        }
        if (localFailures == 0)
            logger.info("Field validation rules on the rowType " + rowType + " all run " +
                    "successfully");
        else
            logger.info("Field validation rules on the rowType " + rowType + " had problems. " +
                    "Failed in applying " + localFailures + " out of " + (localFailures +
                    localSuccess));


        return localFailures == 0;
    }

    public void copyMetaFile(Archive dwca, String metaName) {
        String path = dwca.getLocation().getPath();
        File metaFile = new File(path + "/" + metaName);
        InputStream ins = null;
        try {
            ins = new FileInputStream(metaFile);
            byte fileContent[] = new byte[(int) metaFile.length()];
            ins.read(fileContent);
            File backupMetaFile = new File(path + "_valid/" + metaName);
            FileUtils.forceMkdir(backupMetaFile.getParentFile());
            // if file doesnt exists, then create it
            if (!backupMetaFile.exists()) {
                backupMetaFile.createNewFile();
            }
            FileOutputStream fop = new FileOutputStream(backupMetaFile);
            fop.write(fileContent);
            fop.flush();
            fop.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}