'use strict';
// Stage 1: server-rendered JSP pages and navigation; API workflows arrive in stage 2.
const page=document.body.dataset.page;
document.querySelectorAll('nav [data-page]').forEach(a=>{if(a.dataset.page===page)a.setAttribute('aria-current','page')});
const connection=document.querySelector('#connection');connection.textContent='界面预览 · 第一阶段';
let timer;
function previewNotice(){const toast=document.querySelector('#toast');toast.textContent='当前为第一阶段页面设计，登录及数据操作将在第二阶段接入。';toast.style.display='block';clearTimeout(timer);timer=setTimeout(()=>toast.style.display='none',4500)}
document.querySelectorAll('button').forEach(button=>button.addEventListener('click',previewNotice));
