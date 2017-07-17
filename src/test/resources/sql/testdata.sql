INSERT INTO database_connection (description,driver_class,jdbc_url,name,type,username,password,version,site_id,min_connections,max_connections,managed,active,validation_query)
VALUES ('appNG Root Database','org.hsqldb.jdbc.JDBCDriver','jdbc:hsqldb:hsql://localhost:9001/hsql-testdb','appNG HSQL','HSQL','sa','',now(),null,1,20,true,true,'select 1');
