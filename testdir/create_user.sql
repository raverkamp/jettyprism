define user=&user
define password=&user
define tablespace=users

create user &user
  identified by &password
  default tablespace USERS
  temporary tablespace TEMP
  profile DEFAULT;
-- Grant/Revoke role privileges 
grant connect to &user;
grant resource to &user;
-- Grant/Revoke system privileges 
grant create view to &user;
grant debug connect session to &user;
grant unlimited tablespace to &user;
grant create database link to &user;
grant create synonym to &user;



