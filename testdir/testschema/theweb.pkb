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

htp.p('<a href="./!theweb.flex?key1=val1&key2=val2">Flextest</a>');

htp.p('<ul>');
htp.p('<li>');
htp.p('<a href="./theweb.gen_excel">Excel </a><br>');
htp.p('</li>');htp.p('<li>');
htp.p('<a href="./theweb.show_info">Header-Info</a>');
htp.p('</li>');htp.p('<li>');
htp.p('<a href="./theweb.gen_excel2">Gen Excel</a>');
htp.p('</li>');
htp.p('<li>');
htp.p('<a href="./theweb.tables_demo">Tables Demo</a>');
htp.p('</li>');
htp.p('<li>');
htp.p('<a href="./theweb.error">Exception</a>');
htp.p('</li>');
htp.p('</ul>');
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

procedure nix is
begin
  htp.p('nix');
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

procedure gen_excel2 is
begin
HTP.HTMLOPEN;
htp.headopen;
htp.p('<script type="text/javascript" src="https://cdn.rawgit.com/jsegarra1971/MyExcel/28fc1471/FileSaver.js"></script>');
htp.p('<script type="text/javascript" src="https://cdn.rawgit.com/jsegarra1971/MyExcel/28fc1471/jszip.js"></script>');
htp.p('<script type="text/javascript" src="https://cdn.rawgit.com/jsegarra1971/MyExcel/28fc1471/myexcel.js"></script>');
htp.headclose;
htp.bodyopen;
htp.p('<button onclick="genexcel()">Los</button>');
  htp.p(q'[<script>
  "use strict";
  function genexcel() {
      var excel = $JExcel.new("Calibri light 10 #333333");
      excel.set( {sheet:0,value:"This is Sheet 1" } );
      excel.set(0,3,2,"Row 3, column2");
      excel.generate("SampleData.xlsx");
     
  }
  
  </script>]');

htp.bodyclose;
htp.htmlclose;
  
end;

procedure query_tables(pat varchar2) is
  c sys_refcursor;
begin
 open c for select * from all_tab_columns t 
 where t.table_name like upper(pat)||'%' order by t.OWNER, table_name,t.COLUMN_ID;
 TO_JSON.CURSOR_TO_JSON(c);
end;

procedure tables_demo is
begin
HTP.HTMLOPEN;
htp.headopen;
htp.p('<script type="text/javascript" src="https://cdn.rawgit.com/jsegarra1971/MyExcel/28fc1471/FileSaver.js"></script>');
htp.p('<script type="text/javascript" src="https://cdn.rawgit.com/jsegarra1971/MyExcel/28fc1471/jszip.js"></script>');
htp.p('<script type="text/javascript" src="https://cdn.rawgit.com/jsegarra1971/MyExcel/28fc1471/myexcel.js"></script>');
htp.headclose;
htp.bodyopen;
htp.p('<h1>Enter a table name!</h1>');
htp.p('<input id="tab" type="text"></input>');
htp.p('<button onclick="genexcel()">Los!</button>');
htp.p(q'[<script>
"use strict";
  
function to_query(obj) {
  var str = [];
  for(var p in obj)
    if (obj.hasOwnProperty(p)) {
      str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
    }
  return str.join("&");
}
  
function exec_query(url, obj) {
   var req = new XMLHttpRequest();
   var q = to_query(obj);
   req.open("GET", url +"?" + q , false);
   req.send();
   if (req.status !== 200) {
        alert("JSON ERROR");
        console.log(req.responseText);
        throw "JSON ERROR";
    }
    var txt = req.responseText;
    var res = JSON.parse(txt);
    return res;
}

  function genexcel() {
    var input = document.getElementById("tab");
    var v = input.value;
    var res = exec_query('./theweb.query_tables', {"pat": v});
    console.log(res);
    var excel = $JExcel.new("Calibri light 10 #333333");
    excel.set( {sheet:0,value:"These are the tables" } );
    var headers = ["Owner", "Table Name", "Column Name", "Data Type", "Data Length"];
    var formatHeader=excel.addStyle ( { 															// Format for headers
					border: "none,none,none,thin #333333", 													// 		Border for header
					font: "Calibri 12 #0000AA B"}); 														// 		Font for headers
			for (var i=0;i<headers.length;i++){																// Loop all the haders
				excel.set(0,i,0,headers[i],formatHeader);													// Set CELL with header text, using header format
				excel.set(0,i,undefined,"auto");															// Set COLUMN width to auto (according to the standard this is only valid for numeric columns)
			}
    var numStyle = excel.addStyle({align: "R"});
    for(var i = 0;i<res.length;i++) {
      excel.set(0,0,i+1, res[i].owner);
      excel.set(0,1,i+1, res[i].table_name);
      excel.set(0,2,i+1, res[i].column_name);
      excel.set(0,3,i+1, res[i].data_type);
      excel.set(0,4,i+1, "'" +res[i].data_length);//, numStyle);  
    }
    excel.generate("tables.xlsx");
  }
  
  </script>]');

htp.bodyclose;
htp.htmlclose;
 
end;

procedure error is
begin
  raise_application_error(-20000,'exception in procedure');
end;

procedure flex(name_array owa.vc_arr, value_array owa.vc_arr) is 
begin
  owa_util.mime_header('application/json', true, 'utf-8');
  htp.p('{');
  for i in name_array.first .. name_array.last loop
    if i>1 then
      htp.p(','||chr(10));
    end if;
    htp.p('"'||name_array(i)||'": "'||replace(value_array(i),'"','\"')||'"');
  end loop;
  htp.p('}');
end;

end;
/