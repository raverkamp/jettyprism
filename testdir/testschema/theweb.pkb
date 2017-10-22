create or replace package body theweb as


procedure menu is
begin
HTP.HTMLOPEN;
htp.headopen;
htp.headclose;
htp.bodyopen;
htp.p('<h1>Menu</h1>');
htp.p('<a href="./theweb.test2?a=AA&b=BB">Test2</a>');
htp.p('<a href="./theweb.test2?a=AA&b=BB&c=CC">Test2 b</a>');
htp.p('<div>');
htp.p('<form method="post" action="./theweb.test2" accept-charset="UTF-8">');
htp.p('<input type=text name="a">');
htp.p('<input type=text name="b">');
htp.p('<input type="submit">');
htp.p('</form>');
htp.p('</div>');

htp.p('<div>');
htp.p('<form method="post" action="./theweb.test2">');
htp.p('<input type=text name="a">');
htp.p('<input type=text name="b">');
htp.p('<input type=text name="c">');
htp.p('<input type="submit">');
htp.p('</form>');
htp.p('</div>');

htp.p('<div>Big HTML</div>');
htp.p('<form method="post" action="./theweb.bightml">');
htp.p('<input type=text name="sizee">');
htp.p('<input type="submit">');
htp.p('</form>');
htp.p('</div>');

htp.p('<div>Clob</div>');
htp.p('<form method="post" action="./theweb.clobtest">');
htp.p('<input type=text name="a">');
htp.p('<input type=text name="b">');
htp.p('<input type=text name="c">');
htp.p('<input type=text name="d">');
htp.p('<input type="submit">');
htp.p('</form>');

htp.p('<a href="./theweb.gen_excel">Excel </a><br>');
htp.p('<a href="./theweb.show_info">Header-Info</a>');

htp.p('</div>');

htp.bodyclose;
htp.htmlclose;
end;

procedure test1 is
begin
htp.p('<h1>Hallo '||user||' at '||systimestamp||'</h1>');
for i in 1 .. 20 loop
htp.p('<p> Line '||i||'</p>');
end loop;
end;


procedure test2(a varchar2,b varchar2,c varchar2 default 'x') is
begin
  htp.p('<h2> test2</h2>');
  htp.p('<p>');
  htp.p('a='||a);
    htp.p('<p>');
  htp.p('b='||b);
    htp.p('<p>');
  htp.p('c='||c);
  
end;

procedure bightml(sizee varchar2) is
begin
HTP.HTMLOPEN;
htp.headopen;
htp.headclose;
htp.bodyopen;
for i in 1 .. to_number(sizee) loop
  htp.p(rpad('line '||to_char(i,'000000')||'  ',80,'x'));
  htp.p('<br>');
end loop;
htp.bodyclose;
htp.htmlclose;
end;

procedure clobtest(a varchar2,b clob,c varchar2,d clob) is
x varchar2(32767);
begin
htp.p('<div>');
htp.p(SYS.HTF.ESCAPE_SC(a));

htp.p('</div>');
x:= b;
htp.p('<div>');
htp.p(x);
htp.p('</div>');
htp.p('<div>');
htp.p(c);
htp.p('</div>');

x:= d;
htp.p('<div>');
htp.p(x);
htp.p('</div>');
end;


procedure gen_excel is
begin
 owa_util.mime_header('application/excel', false, 'ISO-8859-4');
 htp.p('');
 htp.p('<html>');
 htp.p('<head></head>');
 htp.p('<body>');
 htp.p('<table>');
 for i in 1 .. 20 loop
 htp.p('<tr>');
 htp.p('<td>'||i||'</td><td>hasgd'||i||'</td>');
 htp.p('</tr>');
 end loop;
 
 htp.p('</table>');
 
 htp.p('</body>');
 htp.p('</html>'); 
end;

procedure show_info is
begin
  HTP.HTMLOPEN;
htp.headopen;
htp.headclose;
htp.bodyopen;
htp.p('<table border="1">');
  for i in 1..owa.num_cgi_vars loop
    htp.p('<tr><td>'||  owa.cgi_var_name(i)||'</td><td>'||owa.cgi_var_val(i)||'</td></tr>');
  end loop;
  htp.p('</table>');
htp.bodyclose;
htp.htmlclose;

end;

end;

/