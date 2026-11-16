import fs from 'node:fs';
import vm from 'node:vm';
import { randomUUID } from 'node:crypto';
const results=[];
for (const filename of ['postman_collection.json','postman_etapa2.json']) {
 const collection=JSON.parse(fs.readFileSync('docs/'+filename,'utf8'));
 const vars=new Map(collection.variable.map(v=>[v.key,v.value]));
 const replace=text=>text.replace(/\{\{([^}]+)\}\}/g,(_,key)=>key==='$guid'?randomUUID():vars.get(key)??'');
 const items=filename==='postman_etapa2.json'?collection.item[0].item:collection.item;
 for(const item of items) {
  const pm={collectionVariables:{set:(k,v)=>vars.set(k,v)},variables:{replaceIn:replace},test:(name,fn)=>fn(),expect:value=>({to:{eql:expected=>{if(value!==expected)throw Error(`Esperado ${expected}, recebido ${value}`)}}})};
  for(const e of item.event??[])if(e.listen==='prerequest')vm.runInNewContext(e.script.exec.join('\n'),{pm});
  const response=await fetch(replace(item.request.url),{method:item.request.method,headers:Object.fromEntries(item.request.header.map(h=>[h.key,h.value])),body:item.request.body?replace(item.request.body.raw):undefined});
  const content=await response.text();
  pm.response={code:response.status,json:()=>JSON.parse(content),to:{have:{status:n=>{if(response.status!==n)throw Error(`${item.name}: ${response.status}, ${content}`)}}}};
  for(const e of item.event??[])if(e.listen==='test')vm.runInNewContext(e.script.exec.join('\n'),{pm});
  results.push({colecao:filename,cenario:item.name,http:response.status,resultado:'PASSOU'});
 }
}
fs.writeFileSync('docs/resultado-postman-etapa2.json',JSON.stringify({executadoEm:new Date().toISOString(),observacao:'Requisições e scripts Postman executados por validador local. Pasta manual de falhas não incluída; falhas reais verificadas por test-etapa2.ps1.',total:results.length,cenarios:results},null,2));
console.log(`${results.length} requisições e scripts das coleções aprovados.`);
