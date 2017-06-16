package os.moudel.db.provider;

import java.io.PrintWriter;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * 数据库连接池工具类
 */
public class DBPool implements DataSource {
	
    private static LinkedList<Connection> list=null;
    
    static{
    	String url=DBConfig.url();
		String driver=DBConfig.driver();
		String username=DBConfig.username();
		String password=DBConfig.password();
		
    	if(list==null||list.size()==0){
    		list = new LinkedList<Connection>();
    		Integer max=Integer.parseInt(DBConfig.maxconnects());
    		try{
    			Class.forName(driver);
	    		for(int i=0 ;i<max;i++){
	             Connection conn =  DriverManager.getConnection(url,username,password);
	             list.add(conn);
	    		}
	    	} catch (Exception e) {
	            e.printStackTrace();
	            throw new RuntimeException("连接数据库失败");
	        }
    	}
    }
    @Override
    public Connection getConnection() throws SQLException {
        if(list.size()<=0){
            throw new RuntimeException("数据库忙，请待会再试试吧！");
        }
        Connection conn=null;
        synchronized (list) {
        	 conn = list.removeFirst();
		}
       
        LocalConn connect = new LocalConn(conn);
        return connect;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {return null;}
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {}
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {}
    @Override
    public int getLoginTimeout() throws SQLException {return 0;}
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {return null;}
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {return null;}
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {return false;}
    @Override
    public Connection getConnection(String username, String password) throws SQLException {return null;}
    
    class LocalConn implements Connection {
        private Connection conn ;
        public void close(){list.add(conn);}
        public LocalConn(Connection conn ){ this.conn = conn;}
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return this.conn.unwrap(iface);
        }
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return this.conn.isWrapperFor(iface);
        }
        @Override
        public Statement createStatement() throws SQLException {
            return this.conn.createStatement();
        }
        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return this.conn.prepareStatement(sql);
        }
        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return this.conn.prepareCall(sql);
        }
        @Override
        public String nativeSQL(String sql) throws SQLException {
            return this.conn.nativeSQL(sql);
        }
        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            this.conn.setAutoCommit(autoCommit);
        }
        @Override
        public boolean getAutoCommit() throws SQLException {
            return this.conn.getAutoCommit();
        }
        @Override
        public void commit() throws SQLException {
            this.conn.commit();
        }
        @Override
        public void rollback() throws SQLException {
            this.conn.rollback();
        }
        @Override
        public boolean isClosed() throws SQLException {
            return this.conn.isClosed();
        }
        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return this.conn.getMetaData();
        }
        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            this.conn.setReadOnly(readOnly);
        }
        @Override
        public boolean isReadOnly() throws SQLException {
            return this.conn.isReadOnly();
        }
        @Override
        public void setCatalog(String catalog) throws SQLException {
            this.conn.setCatalog(catalog);
        }
        @Override
        public String getCatalog() throws SQLException {
            return this.conn.getCatalog();
        }
        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            this.conn.setTransactionIsolation(level);
        }
        @Override
        public int getTransactionIsolation() throws SQLException {
            return this.conn.getTransactionIsolation();
        }
        @Override
        public SQLWarning getWarnings() throws SQLException {
            return this.conn.getWarnings();
        }
        @Override
        public void clearWarnings() throws SQLException {
            this.conn.clearWarnings();
        }
        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return this.conn.createStatement(resultSetType, resultSetConcurrency);
        }
        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency);
        }
        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return this.conn.getTypeMap();
        }
        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            this.conn.setTypeMap(map);
        }
        @Override
        public void setHoldability(int holdability) throws SQLException {
            this.conn.setHoldability(holdability);
        }
        @Override
        public int getHoldability() throws SQLException {
            return this.conn.getHoldability();
        }
        @Override
        public Savepoint setSavepoint() throws SQLException {
            return this.conn.setSavepoint();
        }
        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return this.conn.setSavepoint(name);
        }
        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            this.conn.rollback(savepoint);
        }
        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            this.conn.releaseSavepoint(savepoint);
        }
        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
                throws SQLException {
            return this.conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return this.conn.prepareStatement(sql, autoGeneratedKeys);
        }
        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return this.conn.prepareStatement(sql, columnIndexes);
        }
        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return this.conn.prepareStatement(sql, columnNames);
        }
        @Override
        public Clob createClob() throws SQLException {
            return this.conn.createClob();
        }
        @Override
        public Blob createBlob() throws SQLException {
            return this.conn.createBlob();
        }
        @Override
        public NClob createNClob() throws SQLException {
            return this.conn.createNClob();
        }
        @Override
        public SQLXML createSQLXML() throws SQLException {
            return this.conn.createSQLXML();
        }
        @Override
        public boolean isValid(int timeout) throws SQLException {
            return this.conn.isValid(timeout);
        }
        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            this.conn.setClientInfo(name, value);
        }
        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            this.conn.setClientInfo(properties);
        }
        @Override
        public String getClientInfo(String name) throws SQLException {
            return this.conn.getClientInfo(name);
        }
        @Override
        public Properties getClientInfo() throws SQLException {
            return this.conn.getClientInfo();
        }
        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return this.conn.createArrayOf(typeName, elements);
        }
        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return this.conn.createStruct(typeName, attributes);
        }
        @Override
        public void setSchema(String schema) throws SQLException {
            this.conn.setSchema(schema);
        }
        @Override
        public String getSchema() throws SQLException {
            return this.conn.getSchema();
        }
        @Override
        public void abort(Executor executor) throws SQLException {
            this.conn.abort(executor);
        }
        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            this.conn.setNetworkTimeout(executor, milliseconds);
        }
        @Override
        public int getNetworkTimeout() throws SQLException {
            return this.conn.getNetworkTimeout();
        }
    }
}