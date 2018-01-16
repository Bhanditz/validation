package org.eol.handlers;

import org.eol.parser.utils.CommonTerms;
import org.eol.validator.ArchiveFileState;
import org.eol.validator.functions.AgentValidationFunctions;
import org.eol.validator.functions.MediaValidationFunctions;
import org.eol.validator.functions.ReferenceValidationFunctions;
import org.eol.validator.functions.TaxonValidationFunctions;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.Record;

import java.util.ArrayList;
import java.util.Set;

/**
 *         Handler for the Dwca common operations
 */
public class DwcaHandler {
    private DwcaHandler() {

    }

//    private static Logger logger = LogHandler.getLogger(DwcaHandler.class.getName());

    public static ArrayList<ArchiveFile> getArchiveFile(Archive dwcArchive, String rowTypeURI) throws Exception {
        ArrayList<ArchiveFile> archiveFiles = new ArrayList<ArchiveFile>();
        Set<ArchiveFile> extensions = dwcArchive.getExtensions();
//        ArchiveFile archiveFile = null;
        for (ArchiveFile af : extensions) {
            if (af.getRowType().qualifiedName().equalsIgnoreCase(rowTypeURI)) {
                archiveFiles.add(af);
//                archiveFile = af;
            }
        }
        if (archiveFiles.isEmpty()) {
            if (dwcArchive.getCore().getRowType().qualifiedName().equalsIgnoreCase(rowTypeURI))
                archiveFiles.add(dwcArchive.getCore());
            else
                throw new Exception("Archive file with row type " + rowTypeURI + " not found");
        }
        return archiveFiles;
    }

    /**
     * Return Term of the specified fieldURI if the archive file have this field.
     *
     * @param archiveFile
     * @param fieldURI
     * @return Term of the specified fieldURI
     * @throws Exception in case of on archive file has no field with that URI
     */
    public static Term getTermFromArchiveFile(ArchiveFile archiveFile, String fieldURI) throws Exception {
        Set<Term> afTerms = archiveFile.getTerms();
        Term term = null;
        for (Term t : afTerms) {
            if (t.qualifiedName().equalsIgnoreCase(fieldURI)) {
                term = t;
                break;
            }
        }
        if (term == null) {
            throw new Exception("Archive file with row type " + archiveFile.getRowType().qualifiedName() + " , has no field with the URI " + fieldURI);
        }
        return term;
    }

    public static Term getTermFromArchiveFile2(ArchiveFile archiveFile, String fieldURI) throws Exception {
        if (!archiveFile.hasTerm(fieldURI)) {
            throw new Exception("Archive file with row type " + archiveFile.getRowType().qualifiedName() + " , has no field with the URI " + fieldURI);
        }
        Term fieldTerm = TermFactory.instance().findTerm(fieldURI);
//        archiveFile.get
        Set<Term> afTerms = archiveFile.getTerms();
        Term term = null;
        for (Term t : afTerms) {
            if (t.qualifiedName().equalsIgnoreCase(fieldURI)) {
                term = t;
                break;
            }
        }
        if (term == null) {
            throw new Exception("Archive file with row type " + archiveFile.getRowType().qualifiedName() + " , has no field with the URI " + fieldURI);
        }
        return term;
    }

    public static boolean recordHasTerm(Term term, Record record) {
        if (record.value(term) == null || record.value(term).length() <= 0)
            return false;
        else
            return true;
    }

    public static ArchiveFileState checkRecordsHaveAtLeastOneOfTermsList(ArchiveFile archiveFile, String[] termsString, String rowType) {
        ArrayList<Term> termsList = new ArrayList<Term>(termsString.length);
        for (String termName : termsString) {
            try {
                termsList.add(DwcaHandler.getTermFromArchiveFile(archiveFile, termName));
            } catch (Exception e) {
//                logger.error("Error while getting " + termName + " from archive file. error message : " + e.getMessage());
            }
        }
        if (termsList.isEmpty()) {
//            logger.error("Archive file does not have any of the term list");
            return new ArchiveFileState(true);
        }
        int failures = 0;
        int totalLines = 0;
        for (Record record : archiveFile) {
            boolean hasAnyTerm = false;
            for (Term term : termsList) {
                if (term != null && DwcaHandler.recordHasTerm(term, record)) {
                    hasAnyTerm = true;
                    break;
                }
            }
            if (!hasAnyTerm) {
                failures++;
                countFailedLines(record);
            }
            totalLines++;
        }
        return new ArchiveFileState(totalLines, failures);
    }

//    private static void countFailedLines(Record record, String rowType){
//        if(rowType.equalsIgnoreCase(TermURIs.mediaURI)){
//            MediaValidationFunctions.failedMedia.add(record.value(CommonTerms.identifierTerm));
//        }else if(rowType.equalsIgnoreCase(TermURIs.referenceURI)){
//            ReferenceValidationFunctions.failedReferences.add(record.value(CommonTerms.referenceIDTerm));
//        }else if(rowType.equalsIgnoreCase(TermURIs.agentURI)){
//            AgentValidationFunctions.failedAgents.add(record.value(CommonTerms.agentIDTerm));
//        }else{
//            TaxonValidationFunctions.failedTaxa.add(record.value(DwcTerm.taxonID));
//        }
//    }

    private static void countFailedLines(Record record){
        if(record.rowType() == CommonTerms.mediaTerm){
            MediaValidationFunctions.failedMedia.add(record.value(CommonTerms.identifierTerm));
        }else if(record.rowType() == CommonTerms.referenceTerm){
            ReferenceValidationFunctions.failedReferences.add(record.value(CommonTerms.referenceIDTerm));
        }else if(record.rowType() == CommonTerms.agentTerm){
            AgentValidationFunctions.failedAgents.add(record.value(CommonTerms.agentIDTerm));
        }else{
            TaxonValidationFunctions.failedTaxa.add(record.value(DwcTerm.taxonID));
        }
    }

    /**
     * Checking if the values of specific Term/field is among the list of validValues
     *
     * @param archiveFile        the input Archive file
     * @param values             list of valid values
     * @param emptyFieldAccepted if true the function will handle record without the field as a valid record
     * @return Archive file state
     */
    public static ArchiveFileState checkFieldHasOneOfListOfValues(ArchiveFile archiveFile, String fieldURI, String[] values, boolean emptyFieldAccepted,
                                                                  String rowType)
            throws Exception {
        Term fieldTerm;
        try {
            fieldTerm = DwcaHandler.getTermFromArchiveFile(archiveFile, fieldURI);
        } catch (Exception e) {
            if (emptyFieldAccepted) {
//                logger.error(e.getMessage() + " - " + e);
//                logger.error("All lines do not have Term " + fieldURI + " so all lines is complying the rule, because empty Field is Accepted");
                ArchiveFileState archiveFileState = new ArchiveFileState();
                archiveFileState.setAllLinesComplying(true);
                return archiveFileState;
            } else {
//                logger.error(e.getMessage() + " - " + e);
//                logger.error("All lines do not have Term " + fieldURI + " so all lines is violating the rule, because empty Field is not Accepted");
                return new ArchiveFileState(true);
            }
        }
        int violatingLines = 0;
        int totalLines = 0;
        for (Record record : archiveFile) {
            totalLines++;
            String recordValue = record.value(fieldTerm);
            if (recordValue == null || recordValue.length() <= 0) {
                if (!emptyFieldAccepted) {
                    violatingLines++;
                    countFailedLines(record);
//                    logger.debug("archiveFile " + archiveFile.getRowType().qualifiedName() + " line with null type value");
                }
                continue;
            }
            boolean validRow = false;
            for (String validValue : values) {
                if (recordValue.equalsIgnoreCase(validValue)) {
                    validRow = true;
                    break;
                }
            }
            if (!validRow) {
                violatingLines++;
                countFailedLines(record);
//                logger.debug("archiveFile " + archiveFile.getRowType().qualifiedName() + " line with invalid value : " + recordValue);
            }
        }
        return new ArchiveFileState(totalLines, violatingLines);
    }
}
