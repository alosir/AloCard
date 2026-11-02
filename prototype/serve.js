const http=require('http'),fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'..');
const types={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.woff2':'font/woff2','.md':'text/plain; charset=utf-8','.png':'image/png'};
http.createServer((req,res)=>{
  let p=decodeURIComponent(req.url.split('?')[0]);
  // 必须重定向而非内部映射：根路径下相对路径（tokens.css 等）会解析到 / 根，
  // 导致 404。直接 302 到真实路径，浏览器才能正确解析相对资源。
  if(p==='/'){
    res.writeHead(302,{Location:'/prototype/index.html'});
    return res.end();
  }
  const f=path.join(root,p);
  if(!f.startsWith(root)){res.writeHead(403);return res.end();}
  fs.readFile(f,(e,d)=>{
    if(e){res.writeHead(404);return res.end('404 '+p);}
    res.writeHead(200,{'Content-Type':types[path.extname(f)]||'application/octet-stream','Cache-Control':'no-store'});
    res.end(d);
  });
}).listen(8899,()=>console.log('serving http://localhost:8899/prototype/index.html'));
