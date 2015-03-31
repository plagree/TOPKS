package org.dbweb.socialsearch.general.connection;

import java.sql.Connection;

public interface DBConnection {
  
	public Connection DBConnect();
	public Connection DBConnect(String url, String login, String password);
}
