create or replace package body to_json as

  procedure handle_to_json(handle in integer) is
  
    lo_cursor integer;
  
    col_cnt   INTEGER;
    rec_tab   DBMS_SQL.DESC_TAB;
    rec       DBMS_SQL.DESC_REC;
    x         integer;
    r         integer;
    v         varchar2(2000);
    le        integer;
    err       integer;
    num       number;
    dat       date;
    lo_tt     varchar2(2000);
    rownumber integer;
    
    procedure q(v varchar2) is
    begin
      htp.p(v);
    end;
  
    
  BEGIN
    dbms_output.put_line('x');
    lo_cursor := handle;
    DBMS_SQL.DESCRIBE_COLUMNS(lo_cursor, col_cnt, rec_tab);
    for i in 1 .. col_cnt loop
      rec := rec_tab(i);
    
      lo_tt := 'name: ' || rec.col_name || ', type: ' || rec.col_type ||
               ', scale: ' || rec.col_scale || ', precision: ' ||
               rec.col_precision;
    
      if rec.col_type = dbms_types.TYPECODE_DATE then
        dbms_sql.define_column(lo_cursor, i, dat);
      
      elsif rec.col_type = dbms_types.TYPECODE_NUMBER then
        dbms_sql.define_column(lo_cursor, i, num);
      else
        dbms_sql.define_column(lo_cursor, i, v, 2000);
      end if;
    end loop;
    q('[');
    rownumber := 0;
    loop
      x := DBMS_SQL.FETCH_ROWS(lo_cursor);
      exit when x = 0;
      if rownumber > 0 then
        q(',');
      end if;
      rownumber := rownumber + 1;
      q('{');
      for i in 1 .. col_cnt loop
        rec := rec_tab(i);
        if i > 1 then
          q(', ');
        end if;
        q('"' || lower(rec.col_name) || '" : ');
        if rec.col_type = dbms_types.TYPECODE_DATE then
          DBMS_SQL.COLUMN_VALUE(lo_cursor, i, dat);
          if dat is null then
            q(' null ');
          else
            q('"' || to_char(dat, 'yyyy-mm-dd hh24:mi:ss') || '"');
          end if;
        elsif rec.col_type = dbms_types.TYPECODE_NUMBER then
          DBMS_SQL.COLUMN_VALUE(lo_cursor, i, num);
          if num is null then
            q(' null ');
          else
            q(to_char(num,'TME','nls_numeric_characters=''. '''));
          end if;
        else
          DBMS_SQL.COLUMN_VALUE(lo_cursor, i, v, err, le);
          q('"' || replace(to_char(v), '"', '\"') || '"');
        end if;
      end loop;
      q('}' || chr(10));
      r := r + 1;
    end loop;
    q(']');
    dbms_sql.close_cursor(lo_cursor);
  end;
  
  procedure cursor_to_json(cur in out sys_refcursor) is
    lo_cursor integer;
  begin
    lo_cursor := DBMS_SQL.TO_CURSOR_NUMBER (cur);
    handle_to_json(lo_cursor);
  end;

end;