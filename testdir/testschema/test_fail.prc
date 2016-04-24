create or replace procedure test_fail is
begin
htp.p('<h1>HBug if you see this. '||user||' at '||systimestamp||'</h1>');
end;
/