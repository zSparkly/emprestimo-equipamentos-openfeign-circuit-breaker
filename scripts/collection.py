import json
from pathlib import Path

items=[]
def add(name,method,path,status,body=None,save=None,pre=None):
    request={'method':method,'header':[{'key':'Content-Type','value':'application/json'}],'url':'{{baseUrl}}/api/v1'+path}
    if body is not None: request['body']={'mode':'raw','raw':json.dumps(body,ensure_ascii=False),'options':{'raw':{'language':'json'}}}
    test=[f'pm.test("HTTP {status}", () => pm.response.to.have.status({status}));']
    if save: test.append(f'if (pm.response.code === {status}) pm.collectionVariables.set("{save}", pm.response.json().id);')
    item={'name':name,'request':request,'event':[{'listen':'test','script':{'type':'text/javascript','exec':test}}]}
    if pre: item['event'].append({'listen':'prerequest','script':{'type':'text/javascript','exec':pre}})
    items.append(item)

add('01 Criar usuario','POST','/usuarios',201,{'nome':'Aluno','email':'aluno-{{sufixo}}@exemplo.com','matricula':'{{sufixo}}'},'usuarioId',['pm.collectionVariables.set("sufixo", pm.variables.replaceIn("{{$guid}}"));'])
add('02 Consultar usuario','GET','/usuarios/{{usuarioId}}',200)
add('03 Criar equipamento','POST','/equipamentos',201,{'nome':'Notebook','tipo':'NOTEBOOK','patrimonio':'{{sufixo}}'},'equipamentoId')
add('04 Consultar equipamento','GET','/equipamentos/{{equipamentoId}}',200)
add('05 Listar disponiveis','GET','/equipamentos/disponiveis',200)
add('06 Email duplicado','POST','/usuarios',409,{'nome':'Outro','email':'aluno-{{sufixo}}@exemplo.com','matricula':'outro-{{sufixo}}'})
add('07 Entrada invalida','POST','/usuarios',400,{'nome':'','email':'invalido','matricula':''})
add('08 Usuario inexistente','POST','/emprestimos',404,{'usuarioId':'00000000-0000-0000-0000-000000000000','equipamentoId':'{{equipamentoId}}'})
add('09 Criar emprestimo','POST','/emprestimos',201,{'usuarioId':'{{usuarioId}}','equipamentoId':'{{equipamentoId}}'},'emprestimoId')
add('10 Consultar emprestimo','GET','/emprestimos/{{emprestimoId}}',200)
add('11 Consultar por usuario','GET','/emprestimos/usuarios/{{usuarioId}}',200)
add('12 Equipamento ocupado','POST','/emprestimos',409,{'usuarioId':'{{usuarioId}}','equipamentoId':'{{equipamentoId}}'})
add('13 Repetir reserva','PUT','/equipamentos/{{equipamentoId}}/reservas/{{emprestimoId}}',200)
add('14 Reserva de outro dono','DELETE','/equipamentos/{{equipamentoId}}/reservas/00000000-0000-0000-0000-000000000000',409)
add('15 Devolver','PUT','/emprestimos/{{emprestimoId}}/devolucao',200,{'condicao':'BOM'})
add('16 Repetir devolucao','PUT','/emprestimos/{{emprestimoId}}/devolucao',200,{'condicao':'BOM'})
add('17 Repetir liberacao','DELETE','/equipamentos/{{equipamentoId}}/reservas/{{emprestimoId}}',204)
collection={'info':{'name':'Emprestimos - microsservicos sincronos','description':'Execute em ordem com Collection Runner. O script test-api.ps1 inclui tambem concorrencia real.','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},'variable':[{'key':'baseUrl','value':'http://localhost:8080'}],'item':items}
Path('docs/postman_collection.json').write_text(json.dumps(collection,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
