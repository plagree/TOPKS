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
public class OracleConnection implements DBConnection{

    private static Logger log = LoggerFactory.getLogger(OracleConnection.class);

    private static String urlPrefix = "jdbc:oracle:thin:@//";
    private String url = "infres5:1521/orcl11";
    private String login = "silviu";
    private String password = "silver";
    
    private String urlDB = urlPrefix + url;

    private static Connection connection = null;
    
    public OracleConnection(String url, String login, String password){
    	this.url = url;
    	this.login = login;
    	this.password = password;
    	urlDB = urlPrefix + url;
    }

    public OracleConnection() {
	}

	public Connection DBConnect(){
    	if(connection==null){
    		try {
    			//Driver myDriver = new oracle.jdbc.driver.OracleDriver();
    			//DriverManager.registerDriver(myDriver);
    			connection = DriverManager.getConnection(urlDB, login, password);
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
