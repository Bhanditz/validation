package org.eol.validator;

import org.apache.commons.io.FilenameUtils;
import org.eol.handlers.XMLHandler;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;

import java.io.File;

public class testValidator {
    public static void main (String []args){
//        try {
//            DwcaValidator validator = new DwcaValidator("configs.properties");
//            String path = "/home/ba/eol_resources/4";
//            Archive dwcArchive = ArchiveFactory.openArchive(new File(path));
//
//            validator.copyMetaFile(dwcArchive, "meta.xml" );
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try {
            DwcaValidator validator = new DwcaValidator("configs.properties");
            String path = "/home/ba/eol_resources/8";
//            File myArchiveFile = new File(path);
//            File extractToFolder = new File(FilenameUtils.removeExtension(path) + ".out");
//            Archive dwcArchive = ArchiveFactory.openArchive(myArchiveFile, extractToFolder);
            Archive dwcArchive = ArchiveFactory.openArchive(new File(path));

            validator.validateArchive(dwcArchive.getLocation().getPath(), dwcArchive);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
