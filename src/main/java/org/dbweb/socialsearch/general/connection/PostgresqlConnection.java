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


public class PostgresqlConnection implements DBConnection{

    private static Logger log = LoggerFactory.getLogger(PostgresqlConnection.class);

    private static final String urlPrefix = "jdbc:postgresql://";
    private String url = "localhost/socsearch";
    private String login = "postgres";
    private String password = "silviu";
    
    private String urlDB = urlPrefix + url;

    private static Connection connection = null;

    public PostgresqlConnection(String url, String login, String password){
    	this.url = url;
    	this.login = login;
    	this.password = password;
    	urlDB = urlPrefix + url;
    }
    
    public PostgresqlConnection() {
	}

    public PostgresqlConnection(String dataset) {
    	this.url="localhost"+dataset;
	}
    
    
	public Connection DBConnect() {
        if(connection==null){
            try {
                Driver myDriver = new org.postgresql.Driver();
    		DriverManager.registerDriver(myDriver);
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
    		//log.info("database connection closed");
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

		this.url = url;
    	this.login = login;
    	this.password = password;
    	urlDB = urlPrefix + url;
    	return DBConnect();
    	}

}
