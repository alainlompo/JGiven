package com.tngtech.jgiven.report.html5;

import com.tngtech.jgiven.relocated.guava.base.Charsets;
import com.tngtech.jgiven.relocated.guava.collect.HashMultiset;
import com.tngtech.jgiven.relocated.guava.collect.Multiset;
import com.tngtech.jgiven.relocated.guava.collect.Sets;
import com.tngtech.jgiven.relocated.guava.io.Files;
import com.tngtech.jgiven.relocated.guava.net.MediaType;
import com.tngtech.jgiven.exception.JGivenInstallationException;
import com.tngtech.jgiven.report.model.AttachmentModel;
import com.tngtech.jgiven.report.model.ReportModel;
import com.tngtech.jgiven.report.model.ReportModelVisitor;
import com.tngtech.jgiven.report.model.StepModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

class Html5AttachmentGenerator extends ReportModelVisitor {
    private static final Logger log = LoggerFactory.getLogger( Html5AttachmentGenerator.class );
    private static final String ATTACHMENT_DIRNAME = "attachments";

    private File attachmentsDir;
    private String subDir;
    private Multiset<String> fileCounter = HashMultiset.create();
    private Set<String> usedFileNames = Sets.newHashSet();
    private String htmlSubDir;

    public void generateAttachments( File targetDir, ReportModel model ) {
        subDir = ATTACHMENT_DIRNAME + File.separatorChar + model.getClassName().replace( '.', File.separatorChar );
        htmlSubDir = subDir.replace( File.separatorChar, '/' );
        attachmentsDir = new File( targetDir, subDir );

        if( !attachmentsDir.exists() && !attachmentsDir.mkdirs() ) {
            throw new JGivenInstallationException( "Could not create directory " + attachmentsDir );
        }
        model.accept( this );
    }

    @Override
    public void visit( StepModel stepModel ) {
        List<AttachmentModel> attachments = stepModel.getAttachments();
        for( AttachmentModel attachment : attachments ) {
            writeAttachment( attachment );
        }
    }

    private void writeAttachment( AttachmentModel attachment ) {
        String mimeType = attachment.getMediaType();
        MediaType mediaType = MediaType.parse( mimeType );
        File targetFile = writeFile( attachment, mediaType );
        attachment.setValue( htmlSubDir + "/" + targetFile.getName() );
        log.info( "Attachment written to " + targetFile );
    }

    private String getExtension( MediaType mediaType ) {
        if( mediaType.is( MediaType.SVG_UTF_8 ) ) {
            return "svg";
        }

        if( mediaType.is( MediaType.ICO ) ) {
            return "ico";
        }

        if( mediaType.is( MediaType.BMP ) ) {
            return "bmp";
        }

        return mediaType.subtype();
    }

    File getTargetFile( String fileName, String extension ) {
        if( fileName == null ) {
            fileName = "attachment";
        }

        int count = fileCounter.count( fileName );
        fileCounter.add( fileName );

        String suffix = "";
        if( count > 0 ) {
            count += 1;
            suffix = String.valueOf( count );
        }

        String fileNameWithExtension = fileName + suffix + "." + extension;

        while( usedFileNames.contains( fileNameWithExtension ) ) {
            fileCounter.add( fileName );
            count++;
            suffix = String.valueOf( count );
            fileNameWithExtension = fileName + suffix + "." + extension;
        }
        usedFileNames.add( fileNameWithExtension );
        return new File( attachmentsDir, fileNameWithExtension );
    }

    private File writeFile( AttachmentModel attachment, MediaType mediaType ) {
        String extension = getExtension( mediaType );
        File targetFile = getTargetFile( attachment.getFileName(), extension );
        try {
            if( attachment.isBinary() ) {
                Files.write( parseBase64Binary( attachment.getValue() ), targetFile );
            } else {
                Files.write( attachment.getValue(), targetFile, Charsets.UTF_8 );
            }
        } catch( IOException e ) {
            log.error( "Error while trying to write attachment to file " + targetFile, e );
        }
        return targetFile;
    }

}
