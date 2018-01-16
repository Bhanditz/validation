package org.eol.parser.utils;

import org.eol.validator.OwnDwcaWriter;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.Record;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Constants {
    public static final int ChunkSize = 1000;

    public static boolean copyContentOfArchiveFileToDisk(ArrayList<Record> records, ArchiveFile archiveFile){
        System.out.println("debug " + archiveFile.getTitle());
        Archive archive = archiveFile.getArchive();
        File backup_file = new File("/home/ba/eol_resources/"+archive.getLocation().getName()+"_valid");
        Term rowType = archiveFile.getRowType();
        List<ArchiveField> fieldsSorted = archiveFile.getFieldsSorted();
        ArrayList<Term> termsSorted = new ArrayList<Term>();
        for (ArchiveField archiveField:fieldsSorted){
            termsSorted.add(archiveField.getTerm());
        }

        try {
            OwnDwcaWriter dwcaWriter = new OwnDwcaWriter(archive.getCore().getRowType() /*rowType*/, backup_file);
            for(Record record: records){
                Map<Term, String> termStringMap = dwcaWriter.recordToMap(record, archiveFile);
                dwcaWriter.newRecord(record.id());
                dwcaWriter.addExtensionRecord(termsSorted, rowType, termStringMap, archiveFile.getTitle(),
                        archiveFile.getFieldsTerminatedBy(), archiveFile.getLinesTerminatedBy(), archiveFile.getEncoding());
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("debug");

//        FileOutputStream fop = null;
//        try {
//            if (!backup_file.exists()) {
//                backup_file.createNewFile();
//            }
//            fop = new FileOutputStream(backup_file, true);
//            String content = "";
//            for(Record record: records){
//                Map<Term, String> termStringMap = DwcaWriter.recordToMap(record, archiveFile);
//                content += record.toString();
//                content += "\n";
//            }
//            byte [] fileContent = content.getBytes();
//            fop.write(fileContent);
//            fop.flush();
//            fop.close();
//            return true;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return false;
    }
}
