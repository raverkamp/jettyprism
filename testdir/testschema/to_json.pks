create or replace package to_json as
 procedure cursor_to_json(cur in out sys_refcursor) ;
end;
/