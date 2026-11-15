// Run: node tests/frontend-smoke.cjs (no dependencies or running database needed).
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const elements=new Map();
const el=s=>{if(!elements.has(s))elements.set(s,{textContent:'',innerHTML:'',style:{},dataset:{},classList:{toggle(){}},addEventListener(){},close(){},showModal(){this.open=true}});return elements.get(s)};
const storage=new Map(), requests=[];
let response={status:200,body:{code:0,data:null}};
const context=vm.createContext({Intl,Date,Number,String,Math,Object,Array,JSON,Error,TypeError,AbortController,encodeURIComponent,setTimeout,clearTimeout,console,location:{pathname:'/calorie_app_war/index.html',hash:''},localStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)},document:{querySelector:el,querySelectorAll:()=>[]},window:{addEventListener(){}},fetch:async(url,options)=>{requests.push({url,options});return {status:response.status,ok:response.status<400,json:async()=>response.body}}});
vm.runInContext(fs.readFileSync('src/main/resources/static/app.js','utf8').replace(/boot\(\);\s*$/,''),context);
const evaluate=code=>vm.runInContext(code,context);
(async()=>{
 await evaluate("api('/health')");assert.equal(requests.at(-1).url,'/calorie_app_war/api/v1/health');
 evaluate("state.token='test-token'");await evaluate("api('/meals','POST',{mealType:'LUNCH'})");assert.equal(requests.at(-1).options.headers.Authorization,'Bearer test-token');assert.equal(JSON.parse(requests.at(-1).options.body).mealType,'LUNCH');
 assert.equal(evaluate("esc('<script>\"&')"),'&lt;script&gt;&quot;&amp;');
 assert.match(evaluate("mealRows([{mealType:'LUNCH',totalCalories:120,items:[{foodName:'<img onerror=x>'}]}])"),/&lt;img onerror=x&gt;/);
 response={status:200,body:{code:0,data:{totalCalories:0,targetCalories:0,summary:{},list:[]}}};
 // Endpoint-specific empty fixtures exercise every signed-in page's empty-state contract.
 context.fetch=async(url,options)=>{requests.push({url,options});let data={};if(url.includes('/meals?')||url.includes('/advice?'))data=[];else if(url.includes('/stats/weekly'))data={daily:[],summary:{}};else if(url.includes('/stats/today'))data={targetCalories:0,totalCalories:0};else if(url.includes('/recipes?'))data={list:[],pages:0};return {status:200,ok:true,json:async()=>({code:0,data})}};
 for(const page of ['overview','meals','stats','recipes','advice','profile']){const html=await evaluate(page+'()');assert.ok(html.length>100,page);assert.ok(!html.includes('NaN'),page+' should not render NaN');}
 context.fetch=async()=>({status:401,ok:false,json:async()=>({code:40100,message:'expired'})});
 await assert.rejects(()=>evaluate("api('/auth/me')"),/登录已失效/);assert.equal(evaluate('state.token'),'');
 context.fetch=async()=>({status:200,ok:true,json:async()=>({code:42900,message:'今日调用次数已用完'})});
 await assert.rejects(()=>evaluate("api('/foods/parse','POST',{text:'米饭'})"),/今日调用次数已用完/);
 const calls=[];
 context.fetch=async(url,options)=>{calls.push(url);return {status:200,ok:true,json:async()=>({code:0,data:url.endsWith('/login')?{token:'new-token',user:{username:'test_user'}}:{id:7}})}};
 const loggedIn=await evaluate("authenticate({username:'test_user',password:'sample-password'},true)");
 assert.equal(loggedIn.token,'new-token');assert.ok(calls[0].endsWith('/register'));assert.ok(calls[1].endsWith('/login'));
 calls.length=0;
 context.fetch=async url=>{calls.push(url);return {status:503,ok:false,json:async()=>({code:50000,message:'数据库暂未连接'})}};
 await assert.rejects(()=>evaluate("authenticate({username:'test_user',password:'sample-password'},true)"),/数据库暂未连接/);
 assert.equal(calls.length,1,'failed registration must not attempt login');
 context.fetch=async url=>({status:url.endsWith('/register')?200:503,ok:url.endsWith('/register'),json:async()=>url.endsWith('/register')?{code:0,data:{id:7}}:{code:50000,message:'数据库暂未连接'}});
 await assert.rejects(()=>evaluate("authenticate({username:'test_user',password:'sample-password'},true)"),/账号已注册成功.*无需重复注册/);
 evaluate('auth(true)');assert.ok(el('#modal-body').innerHTML.includes('id="auth-error"'),'auth errors must appear inside the modal');
 context.fetch=async()=>({status:200,ok:true,json:async()=>({code:0,data:{status:'DB_DOWN'}})});
 await evaluate('refreshConnection()');assert.equal(el('#connection').textContent,'数据库未连接');
 context.fetch=async()=>({status:200,ok:true,json:async()=>({code:0,data:{status:'UP'}})});
 await evaluate('refreshConnection()');assert.equal(el('#connection').textContent,'服务已连接');
 for (const root of ['', '/calorie_app_war']) {
  for (const page of ['overview','meals','stats','recipes','advice','profile']) {
   const pageElement={innerHTML:'<section>server-rendered '+page+'</section>'};
   let requested;
   const jspContext=vm.createContext({...context,
    location:{pathname:root+'/jsp/'+page,hash:''},
    document:{...context.document,body:{dataset:{page,context:root}},addEventListener(){},querySelector:s=>s==='#page'?pageElement:el(s)},
    fetch:async url=>{requested=url;return {status:200,ok:true,json:async()=>({code:0,data:{}})}}
   });
   vm.runInContext(fs.readFileSync('src/main/resources/static/app.js','utf8').replace(/boot\(\);\s*$/,''),jspContext);
   await vm.runInContext('render()',jspContext);
   assert.equal(vm.runInContext('state.page',jspContext),page);
   assert.ok(pageElement.innerHTML.includes('server-rendered '+page),'guest retains module-specific JSP content');
   assert.ok(pageElement.innerHTML.includes('jsp-login'));
   await vm.runInContext("api('/health')",jspContext);
   assert.equal(requested,root+'/api/v1/health');
  }
 }
 console.log('PASS: API contracts, escaping, six page empty states, auth errors, and all six JSP guest pages under root and WAR contexts');
})().catch(e=>{console.error(e);process.exitCode=1});
