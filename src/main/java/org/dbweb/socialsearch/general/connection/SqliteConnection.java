/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.general.connection;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Silver
 */
public class SqliteConnection implements DBConnection{

    private static Logger log = LoggerFactory.getLogger(SqliteConnection.class);

    private static String urlPrefix = "jdbc:sqlite:";
    private String url = "socsearch.db";
    
    private String urlDB = urlPrefix + url;
    
    public SqliteConnection(String url){
    	this.url = url;
    	urlDB = urlPrefix + url;
    }
    
    public SqliteConnection(){
    }

    private static Connection connection = null;

    public Connection DBConnect(){
    	if(connection==null){
    		try {
    			//Driver myDriver = new org.sqlite.JDBC();
    			//DriverManager.registerDriver(myDriver);
    			connection = DriverManager.getConnection(urlDB);
    			connection.setAutoCommit(false);
    		} catch (SQLException e) {
    			log.error("connecting to {} - error trace {}",urlDB,e);
    			//System.exit(1);
    		}
    		return connection;
    	}
    	return connection;
    }

    @Override
    protected void finalize() throws Throwable
    {
    	if(connection!=null){
    		try {
    			connection.close();
    			log.info("database connection closed");
    		}
    		catch (SQLException e) {
    			log.error("closing the DB connection - error trace {}",e);
    			//System.exit(1);
    		}
    	}
        super.finalize();
    }

	@Override
	public Connection DBConnect(String url, String login, String password) {
		return null;
	}

}
