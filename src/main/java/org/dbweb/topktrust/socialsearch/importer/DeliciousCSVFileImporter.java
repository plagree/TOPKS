package org.dbweb.topktrust.socialsearch.importer;

import au.com.bytecode.opencsv.CSVReader;

import org.dbweb.socialsearch.general.connection.DBConnection;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliciousCSVFileImporter {

    private static Logger log = LoggerFactory.getLogger(DeliciousCSVFileImporter.class);

    private static final String sqlInsertData = "insert into soc_tag values(DEFAULT,?,?,?)";

    private DBConnection dbConnection;

    //private JProgressBar progressBar;

    public DeliciousCSVFileImporter(DBConnection dbConnection){
        this.dbConnection = dbConnection;
    }

    public int insertDataFromFile(String fileName){
        Connection connection = dbConnection.DBConnect();
        PreparedStatement ps=null;
        CSVReader csvReader=null;
        try {
            csvReader = new CSVReader(new FileReader(fileName), '\t');
        } catch (FileNotFoundException e) {
            log.error("error opening file {} - trace {}",fileName,e);
            return 1;
        }
        String[] line;
        try {
            while ((line = csvReader.readNext()) != null) {
                ps = connection.prepareStatement(sqlInsertData);
                ps.setString(1, line[1]);
                ps.setString(2, line[2]);
                ps.setString(3, line[3]);
                ps.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            log.error("error in statement {} - trace {}",ps,e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
               log.error("error in sql rollback - trace {}",ps,ex);
            }
            return 2;
        } catch (IOException e) {
            log.error("error reading file {} - trace {}",fileName,e);
            return 3;
        }
        return 0;
    }

}
