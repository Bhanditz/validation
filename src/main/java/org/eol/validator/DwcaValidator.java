package org.eol.validator;

import com.sun.prism.impl.Disposer;
import org.eol.handlers.DwcaHandler;
import org.eol.parser.utils.Constants;
import org.eol.validator.rules.FieldValidationRule;
import org.eol.validator.rules.MetaFileValidationRule;
import org.eol.validator.rules.RowValidationRule;
import org.eol.validator.rules.ValidationRulesLoader;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.Record;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
//        logger.info("Start applying the structuralValidationRules on archive " + dwca.getLocation
//                () + " ...");
//        if (!applyStructuralValidationRules(dwca, validationResult)) {
//            logger.error("Failed in applying some Structural Validation rules");
//            return false;
//        }
//        logger.info("Start applying the RowValidationRules on archive " + dwca.getLocation
//                () + " ...");
//        if (validationResult.getStructuralErrors().size() > 0) {
//            logger.error("Dwca " + dwca.getLocation() + " does not pass some structural " +
//                    "validation rules");
//            return true;
//        }
//        logger.info("Start applying the FieldValidationRules on archive " + dwca.getLocation
//                () + " ...");
        boolean validationRunWithoutProblems = true;
//        if (!applyRowValidationRules(dwca, validationResult)) {
//            logger.error("Failed in applying some Row Validation rules");
//            validationRunWithoutProblems = false;
//        }

        if (!applyFieldValidationRules(dwca, validationResult)) {
            logger.error("Failed in applying some Field Validation rules");
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
//    private boolean applyStructuralValidationRules(Archive dwca, ValidationResult
//            validationResult) {
//        List<MetaFileValidationRule> rules = rulesLoader.getMetaFileValidationRules();
//        if (rules.isEmpty()) {
//            // TODO copy meta file
//            copyMetaFile(dwca, "meta.xml");
//
//            logger.info("No MetaFileValidationRules to apply");
//            return true;
//        }
//
//        int success = 0;
//        int failures = 0;
//        for (MetaFileValidationRule rule : rules) {
//            if (rule.validate(dwca, validationResult))
//                success++;
//            else
//                failures++;
//        }
//        logger.info("Out of  " + rules.size() + " MetaFileValidationRules");
//        logger.info("Successfully applied " + success + " MetaFileValidationRules");
//        logger.info("Failed in applying " + failures + " MetaFileValidationRules");
//
//        return failures == 0;
//    }

    private List<String> filterNotExistingRowTypes(Archive archive, List<String> rowTypeList) {
        logger.info("Prepare HashSet for the rowtypes of the archive");
        HashSet<String> archiveList = new HashSet<String>();
        archiveList.add(archive.getCore().getRowType().qualifiedName().toLowerCase());
        for (ArchiveFile archiveFile : archive.getExtensions()) {
            archiveList.add(archiveFile.getRowType().qualifiedName().toLowerCase());
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

    /**
     * Apply the Row Validation Rules on the darwin core archive and put the results in the
     * ValidationResult object
     *
     * @param dwcArchive the input Darwin Core Archive
     * @return false in case of failure in applying any of the validation rules
     */
//    private boolean applyRowValidationRules(Archive dwcArchive, ValidationResult validationResult) {
//        List<String> rowTypeList = rulesLoader.getRowTypeList();
//        if (rowTypeList.isEmpty()) {
//            logger.warn("Empty rowType list. No rowTypes have validation rules");
//            return true;
//        }
//        int success = 0;
//        int failures = 0;
//        rowTypeList = filterNotExistingRowTypes(dwcArchive, rowTypeList);
//        for (String rowType : rowTypeList) {
//            List<RowValidationRule> rules = rulesLoader.getValidationRulesOfRowType(rowType);
//            if (rules.isEmpty()) {
//                logger.info("Row type " + rowType + " has no row validation rules");
//                continue;
//            }
//            logger.info("start applying "+rules.size()+" row Validations on archive file " + rowType );
//            int localSuccess = 0;
//            int localFailures = 0;
//            for (RowValidationRule rule : rules) {
//                if (!rule.validate(dwcArchive, validationResult)) {
//                    localFailures++;
//                    logger.error("RowType : " + rowType + " , Failed in applying the following " +
//                            "rule : " + rule.toString());
//                } else
//                    localSuccess++;
//            }
//            if (localFailures == 0)
//                logger.info("Row validation rules on the rowType " + rowType + " all run " +
//                        "successfully");
//            else
//                logger.info("Row validation rules on the rowType " + rowType + " had problems. " +
//                        "Failed in applying " + localFailures + " out of " + (localFailures +
//                        localSuccess));
//            success += localSuccess;
//            failures += localFailures;
//        }
//        if (failures > 0) {
//            logger.info("Row validation had  " + failures + " failed to be applied rules out of "
//                    + (failures + success));
//            return false;
//        } else
//            return true;
//    }

    /**
     * Apply the Field Validation Rules on the darwin core archive and put the results in the
     * ValidationResult object
     *
     * @param dwcArchive the input Darwin Core Archive
     * @return false in case of failure in applying any of the validation rules
     */
    private boolean applyFieldValidationRules(Archive dwcArchive, ValidationResult
            validationResult) {
        List<String> rowTypeList = rulesLoader.getRowTypeList();
        if (rowTypeList.isEmpty()) {
            logger.warn("Empty rowType list. No rowTypes have validation rules");
            return true;
        }
        int success = 0;
        int failures = 0;
        rowTypeList = filterNotExistingRowTypes(dwcArchive, rowTypeList);
        for (String rowType : rowTypeList) {
            List<FieldValidationRule> rules = rulesLoader.getValidationRulesOfFieldType(rowType);
            if (rules.isEmpty()){
                logger.info("Row type " + rowType + " has no field field validation rules");
//                continue;
                //TODO call copy method
            }
            logger.info("start applying "+rules.size()+" field Validations on archive file " + rowType );
            int localSuccess = 0;
            int localFailures = 0;
            ArrayList<ArchiveFile> archiveFiles;
            try {
                archiveFiles = DwcaHandler.getArchiveFile(dwcArchive, rowType);
            } catch (Exception e) {
//            logger.fatal("The specified rowtype : " + this.rowTypeURI + " is not found at the archive");
                return true;
            }
            for(ArchiveFile archiveFile : archiveFiles){
                int totalLines = 0;
                ArrayList<Record> recordsToValid = new ArrayList<Record>();
                for (Record record : archiveFile){
                    if (totalLines%chunkSize == 0 && totalLines !=0){
                        validateRecords (rules, archiveFile, validationResult,
                                recordsToValid, rowType);
                    }
                    totalLines ++;
                    recordsToValid.add(record);
                }

                if (!recordsToValid.isEmpty()){
                    validateRecords (rules, archiveFile, validationResult,
                            recordsToValid, rowType);

                }
            }
        }
        if (failures > 0) {
            logger.info("Field validation had  " + failures + " failed to be applied rules out of" +
                    " " + (failures + success));
            return false;
        } else
            return true;
    }

    public void copyMetaFile(Archive dwca, String metaName){
        String path = dwca.getLocation().getPath();
        File metaFile = new File(path+"/"+metaName);
        InputStream ins = null;
        try {
            ins = new FileInputStream(metaFile);
            byte fileContent[] = new byte[(int)metaFile.length()];
            ins.read(fileContent);
            File file = new File("/home/ba/eol_resources/meta.xml");

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fop = new FileOutputStream(file);
            fop.write(fileContent);
            fop.flush();
            fop.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void validateRecords (List<FieldValidationRule> rules, ArchiveFile archiveFile, ValidationResult validationResult,
                                  ArrayList<Record> recordsToValid, String rowType){
        //TODO loop on rules and flush array list by write it to files
        for (FieldValidationRule rule : rules) {
            if (!rule.validate(archiveFile, validationResult, recordsToValid)) {
//                localFailures++;
                logger.error("RowType : " + rowType + " , Failed in applying the following " +
                        "rule : " + rule.toString());
            }
//            } else
//                localSuccess++;
        }
//        if (localFailures == 0)
//            logger.info("Field validation rules on the rowType " + rowType + " all run " +
//                    "successfully");
//        else
//            logger.info("Field validation rules on the rowType " + rowType + " had problems. " +
//                    "Failed in applying " + localFailures + " out of " + (localFailures +
//                    localSuccess));
//        success += localSuccess;
//        failures += localFailures;
        if(Constants.copyContentOfArchiveFileToDisk(recordsToValid, archiveFile)){
            recordsToValid.clear();
        }
    }

}