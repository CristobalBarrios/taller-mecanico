'use strict';

const $ = s => document.querySelector(s);
const escapar = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const dinero = v => new Intl.NumberFormat('es-GT', {style:'currency',currency:'GTQ'}).format(Number(v || 0));
const fecha = v => v ? new Date(v).toLocaleString('es-GT') : '—';
const roles = ['ADMINISTRADOR','RECEPCION','MECANICO','BODEGUERO'];
const etiqueta = v => ({ADMINISTRADOR:'Administrador',RECEPCION:'Recepción',MECANICO:'Mecánico',BODEGUERO:'Bodeguero',RECIBIDO:'Recibido',DIAGNOSTICADO:'Diagnosticado',EN_REPARACION:'En reparación',TERMINADO:'Terminado',ENTREGADO:'Entregado',PENDIENTE:'Pendiente',DESPACHADO:'Despachado',RECHAZADO:'Rechazado'}[v] || v);
const estado = v => `<span class="estado ${escapar(v)}">${escapar(etiqueta(v))}</span>`;
let usuario, csrf, seccion, configuracion;
const puede = (...permitidos) => usuario?.rol === 'ADMINISTRADOR' || permitidos.includes(usuario?.rol);

async function token() { csrf = await api('/csrf'); }
async function api(ruta, metodo='GET', datos) {
  const opciones = {method:metodo,headers:{},credentials:'same-origin'};
  if (metodo !== 'GET') {
    opciones.headers[csrf.header] = csrf.token;
    if (datos instanceof URLSearchParams) opciones.body = datos;
    else if (datos !== undefined) { opciones.headers['Content-Type']='application/json'; opciones.body=JSON.stringify(datos); }
  }
  const r = await fetch('/api'+ruta, opciones);
  const texto = await r.text();
  let cuerpo;
  try { cuerpo = texto ? JSON.parse(texto) : null; } catch { cuerpo = null; }
  if (!r.ok) {
    if (r.status===401 && ruta!=='/login') {
      $('#aplicacion').hidden=true; $('#acceso').hidden=false; usuario=null;
    }
    throw new Error(ruta==='/login' && r.status===401 ? 'Usuario o contraseña incorrectos.' : cuerpo?.error && !['Unauthorized','Forbidden'].includes(cuerpo.error) ? cuerpo.error : ({401:'La sesión terminó. Inicia sesión nuevamente.',403:'No tienes permiso para esta operación o la sesión caducó. Recarga la página.',404:'Registro no encontrado.'}[r.status] || 'No se pudo completar la operación.'));
  }
  return cuerpo;
}
function aviso(texto, error=false) {
  const e=$('#mensaje'); e.textContent=texto; e.className=error?'error':''; e.hidden=false;
  e.scrollIntoView({behavior:'smooth',block:'nearest'});
}
function abrir(titulo, html) {
  $('#titulo-modal').textContent=titulo; $('#cuerpo-modal').innerHTML=html;
  $('#modal').showModal();
}
function campo(nombre,titulo,tipo='text',valor='',extra='') {
  return `<label>${titulo}<input name="${nombre}" type="${tipo}" value="${escapar(valor)}" ${extra} required></label>`;
}
function opciones(filas, valor, texto) {
  return filas.map(f=>`<option value="${escapar(f[valor])}">${escapar(typeof texto==='function'?texto(f):f[texto])}</option>`).join('');
}
function selector(nombre,titulo,items) {
  return `<label>${titulo}<select name="${nombre}" required><option value="">Selecciona una opción</option>${items}</select></label>`;
}
function area(nombre,titulo,max=2000) {
  return `<label class="ancho">${titulo}<textarea name="${nombre}" maxlength="${max}" required></textarea></label>`;
}
function formulario(titulo,campos,guardar) {
  abrir(titulo,`<form id="formulario" class="rejilla">${campos}<p id="error-formulario" class="error ancho" role="alert"></p><div class="acciones ancho"><button type="button" id="cancelar" class="secundario">Cancelar</button><button type="submit" class="principal">Guardar</button></div></form>`);
  $('#cancelar').onclick=()=>$('#modal').close();
  $('#formulario').onsubmit=async e=>{
    e.preventDefault(); const boton=e.submitter; boton.disabled=true;
    const datos=Object.fromEntries(new FormData(e.target));
    e.target.querySelectorAll('input[type=number]').forEach(i=>{datos[i.name]=Number(i.value);});
    e.target.querySelectorAll('select[data-numero]').forEach(i=>{datos[i.name]=Number(i.value);});
    try { await guardar(datos); $('#modal').close(); await cargar(seccion); aviso('Operación guardada correctamente.'); }
    catch(err) { $('#error-formulario').textContent=err.message; }
    finally { boton.disabled=false; }
  };
  $('#formulario').querySelectorAll('select').forEach(s=>{if(s.name.endsWith('Id'))s.dataset.numero='true';});
}
function tabla(columnas,filas,vacio='No hay registros todavía.') {
  if (!filas.length) return `<div class="tabla vacio">${vacio}</div>`;
  return `<div class="tabla"><table><thead><tr>${columnas.map(c=>`<th scope="col">${c[0]}</th>`).join('')}</tr></thead><tbody>${filas.map(f=>`<tr>${columnas.map(c=>`<td>${typeof c[1]==='function'?c[1](f):escapar(f[c[1]])}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
}
function boton(accion,id,texto) {return `<button data-accion="${accion}" data-id="${id}">${texto}</button>`;}
function cabecera(texto,crear) { return `<div class="barra"><p class="muted">${texto}</p>${crear?`<button id="nuevo" class="principal">+ ${crear}</button>`:''}</div>`; }
function filtro() {
  const contenedor=$('#contenido .tabla');
  if (!contenedor?.querySelector('table')) return;
  const entrada=document.createElement('input'); entrada.type='search'; entrada.placeholder='Buscar en esta lista…'; entrada.setAttribute('aria-label','Buscar en esta lista');
  $('#contenido .barra')?.prepend(entrada);
  entrada.oninput=()=>contenedor.querySelectorAll('tbody tr').forEach(f=>{f.hidden=!f.textContent.toLocaleLowerCase().includes(entrada.value.toLocaleLowerCase());});
}
async function iniciar() {
  usuario=await api('/yo'); configuracion=await api('/configuracion');
  $('#acceso').hidden=true; $('#aplicacion').hidden=false;
  $('#nombre-usuario').textContent=usuario.nombre; $('#rol-usuario').textContent=etiqueta(usuario.rol);
  $('#marca-taller').textContent=configuracion.nombre;
  $('#fecha-hoy').textContent=new Date().toLocaleDateString('es-GT',{day:'numeric',month:'long',year:'numeric'});
  const enlaces=[];
  if(puede('RECEPCION','MECANICO')) enlaces.push(['ordenes','Órdenes de trabajo']);
  if(puede('RECEPCION')) enlaces.push(['clientes','Clientes'],['vehiculos','Vehículos']);
  if(puede('MECANICO','BODEGUERO')) enlaces.push(['repuestos','Repuestos']);
  if(puede('BODEGUERO')) enlaces.push(['solicitudes','Despachos'],['movimientos','Movimientos de bodega']);
  if(puede('RECEPCION')) enlaces.push(['facturas','Facturación']);
  if(puede()) enlaces.push(['usuarios','Usuarios y roles'],['configuracion','Configuración'],['historial','Historial']);
  $('#navegacion').innerHTML=enlaces.map(([id,texto])=>`<button data-seccion="${id}">${texto}</button>`).join('');
  $('#navegacion').onclick=e=>{const b=e.target.closest('[data-seccion]');if(b)cargar(b.dataset.seccion);};
  await cargar(enlaces[0][0]);
}
async function cargar(nombre) {
  seccion=nombre; $('#mensaje').hidden=true;
  document.querySelectorAll('[data-seccion]').forEach(b=>b.classList.toggle('activo',b.dataset.seccion===nombre));
  $('#titulo').textContent=$(`[data-seccion="${nombre}"]`)?.textContent || '';
  $('#contenido').innerHTML='<p class="cargando">Cargando información…</p>';
  try {
    await ({ordenes:verOrdenes,clientes:verClientes,vehiculos:verVehiculos,repuestos:verRepuestos,solicitudes:verSolicitudes,movimientos:verMovimientos,facturas:verFacturas,usuarios:verUsuarios,configuracion:verConfiguracion,historial:verHistorial}[nombre])();
    filtro();
  } catch(e) { $('#contenido').innerHTML='<p>No se pudo cargar esta sección. Selecciónala nuevamente para reintentar.</p>'; aviso(e.message,true); }
}
async function verClientes() {
  const filas=await api('/clientes');
  $('#contenido').innerHTML=cabecera('Datos de contacto y facturación.','Registrar cliente')+tabla([['Nombre','nombre'],['NIT','nit'],['Teléfono','telefono']],filas);
  $('#nuevo').onclick=()=>formulario('Registrar cliente',campo('nombre','Nombre completo','text','','maxlength="100"')+campo('nit','NIT / CF','text','CF','maxlength="30"')+campo('telefono','Teléfono','tel','','maxlength="30"'),d=>api('/clientes','POST',d));
}
async function verVehiculos() {
  const filas=await api('/vehiculos');
  $('#contenido').innerHTML=cabecera('Vehículos registrados y sus propietarios.','Registrar vehículo')+tabla([['Placa','placa'],['Marca','marca'],['Modelo','modelo'],['Año','anio'],['Cliente','cliente']],filas);
  $('#nuevo').onclick=async()=>{
    try {
      const clientes=await api('/clientes');
      if(!clientes.length)return aviso('Registra un cliente antes de agregar su vehículo.',true);
      formulario('Registrar vehículo',selector('clienteId','Cliente',opciones(clientes,'id','nombre'))+campo('placa','Placa','text','','maxlength="20"')+campo('marca','Marca','text','','maxlength="60"')+campo('modelo','Modelo','text','','maxlength="60"')+campo('anio','Año','number',new Date().getFullYear(),'min="1900" max="2100"'),d=>api('/vehiculos','POST',d));
    }catch(e){aviso(e.message,true);}
  };
}
async function verOrdenes() {
  const filas=await api('/ordenes');
  const abiertas=filas.filter(o=>!['TERMINADO','ENTREGADO'].includes(o.estado)).length;
  const listas=filas.filter(o=>o.estado==='TERMINADO').length;
  $('#contenido').innerHTML=`<div class="resumen"><div class="dato"><span>EN EL TALLER</span><strong>${abiertas}</strong><span>órdenes en proceso</span></div><div class="dato acento"><span>LISTOS PARA ENTREGA</span><strong>${listas}</strong><span>vehículos terminados</span></div><div class="dato"><span>HISTORIAL</span><strong>${filas.length}</strong><span>órdenes registradas</span></div></div>`+cabecera(usuario.rol==='MECANICO'?'Tus vehículos asignados y su progreso.':'Recepción y seguimiento de los vehículos.',puede('RECEPCION')?'Registrar ingreso':null)+tabla([['Orden',o=>`<strong>#${o.id}</strong>`],['Vehículo',o=>`<strong>${escapar(o.placa)}</strong><br><span class="muted">${escapar(o.marca)} ${escapar(o.modelo)}</span>`],['Cliente','cliente'],['Mecánico','mecanico'],['Estado',o=>estado(o.estado)],['Ingreso',o=>fecha(o.ingreso)],['',o=>boton('detalle',o.id,'Abrir orden →')]],filas,'No hay órdenes. Los ingresos de vehículos aparecerán aquí.');
  if($('#nuevo'))$('#nuevo').onclick=async()=>{
    try {
      const [vehiculos,mecanicos]=await Promise.all([api('/vehiculos'),api('/mecanicos')]);
      if(!vehiculos.length || !mecanicos.length)return aviso('Necesitas un vehículo registrado y un usuario con rol Mecánico.',true);
      const disponibles=vehiculos.filter(v=>!filas.some(o=>o.vehiculo_id===v.id && o.estado!=='ENTREGADO'));
      if(!disponibles.length)return aviso('Todos los vehículos registrados tienen una orden sin entregar.',true);
      formulario('Registrar ingreso',selector('vehiculoId','Vehículo',opciones(disponibles,'id',v=>`${v.placa} · ${v.cliente}`))+selector('mecanicoId','Mecánico responsable',opciones(mecanicos,'id','nombre'))+area('motivo','Motivo de ingreso',1000),d=>api('/ordenes','POST',d));
    }catch(e){aviso(e.message,true);}
  };
}
async function verDetalle(id) {
  const o=await api('/ordenes/'+id);
  const editable=!['TERMINADO','ENTREGADO'].includes(o.estado);
  let acciones='';
  if(puede('MECANICO') && editable) {
    acciones+=boton('diagnosticar',id,'Agregar diagnóstico');
    if(o.estado!=='RECIBIDO')acciones+=boton('solicitar',id,'Solicitar repuesto');
    if(['DIAGNOSTICADO','EN_REPARACION'].includes(o.estado))acciones+=boton('estado',id,o.estado==='DIAGNOSTICADO'?'Iniciar reparación':'Terminar reparación');
  }
  if(puede('RECEPCION') && editable)acciones+=boton('asignar',id,'Reasignar mecánico');
  if(puede('RECEPCION') && o.estado==='TERMINADO')acciones+=o.facturas.length?boton('entregar',id,'Registrar salida'):boton('facturar',id,'Emitir factura');
  abrir('Orden #'+id,`<div class="barra">${estado(o.estado)}<span class="muted">Ingreso: ${fecha(o.ingreso)}</span></div><p class="texto">${escapar(o.motivo)}</p>${o.salida?`<p class="muted">Salida: ${fecha(o.salida)}</p>`:''}<div class="acciones" style="flex-wrap:wrap">${acciones}</div><hr class="separador"><h3>Diagnósticos y reparaciones</h3>${o.diagnosticos.length?o.diagnosticos.map(d=>`<div class="tarjeta"><p class="muted">${escapar(d.mecanico)} · ${fecha(d.fecha)}</p><strong>Fallas detectadas</strong><p class="texto">${escapar(d.descripcion)}</p><strong>Reparación requerida</strong><p class="texto">${escapar(d.reparacion)}</p></div>`).join(''):'<p class="muted">El mecánico aún no registra un diagnóstico.</p>'}<h3>Repuestos solicitados</h3>${tabla([['Repuesto','repuesto'],['Cantidad','cantidad'],['Estado',s=>estado(s.estado)],['Precio unitario',s=>s.precio_unitario==null?'—':dinero(s.precio_unitario)]],o.solicitudes,'Sin solicitudes de repuestos.')}<p class="muted" style="margin-top:20px">Mano de obra: ${dinero(o.mano_obra)}</p>${o.facturas.map(f=>`<p>Factura #${f.id} · Total: <strong>${dinero(f.total)}</strong></p>`).join('')}`);
  $('#cuerpo-modal').dataset.orden=JSON.stringify(o);
}
async function verRepuestos() {
  const filas=await api('/repuestos');
  $('#contenido').innerHTML=cabecera('Existencias actuales. El stock se descuenta al despachar.',puede('BODEGUERO')?'Registrar repuesto':null)+tabla([['Código','codigo'],['Repuesto','nombre'],['Precio',r=>dinero(r.precio)],['Existencia',r=>`<strong>${r.stock}</strong>${r.stock<=r.minimo?' <span class="estado PENDIENTE">Stock bajo</span>':''}`],['Mínimo','minimo'],['',r=>puede('BODEGUERO')?boton('entrada',r.id,'Registrar entrada'):'Consulta en tiempo real']],filas);
  if($('#nuevo'))$('#nuevo').onclick=()=>formulario('Registrar repuesto',campo('codigo','Código','text','','maxlength="40"')+campo('nombre','Nombre','text','','maxlength="100"')+campo('precio','Precio unitario (Q)','number','0','min="0" max="99999999.99" step="0.01"')+campo('stock','Existencia inicial','number','0','min="0" max="2147483647"')+campo('minimo','Existencia mínima','number','2','min="0" max="2147483647"'),d=>api('/repuestos','POST',d));
}
async function verSolicitudes() {
  const filas=await api('/solicitudes');
  $('#contenido').innerHTML=cabecera('Comprueba y despacha los repuestos solicitados por mecánica.')+tabla([['Solicitud',s=>'#'+s.id],['Orden / placa',s=>`#${s.orden_id} · ${escapar(s.placa)}`],['Mecánico','mecanico'],['Repuesto','repuesto'],['Cantidad','cantidad'],['Estado',s=>estado(s.estado)],['',s=>s.estado==='PENDIENTE'?boton('despachar',s.id,'Resolver solicitud'):'—']],filas,'No hay solicitudes de repuestos.');
}
async function verMovimientos() {
  $('#contenido').innerHTML=cabecera('Entradas y salidas de inventario, con usuario y motivo.')+tabla([['Fecha',m=>fecha(m.fecha)],['Repuesto','repuesto'],['Cantidad',m=>`${m.cantidad>0?'+':''}${m.cantidad}`],['Motivo','motivo'],['Usuario','usuario']],await api('/movimientos'));
}
async function verFacturas() {
  const filas=await api('/facturas');
  $('#contenido').innerHTML=cabecera('Comprobantes internos de reparaciones terminadas. Importes en quetzales.')+tabla([['Factura',f=>'#'+f.id],['Orden / placa',f=>`#${f.orden_id} · ${escapar(f.placa)}`],['Cliente','cliente_nombre'],['Fecha',f=>fecha(f.fecha)],['Total',f=>dinero(f.total)],['',f=>boton('ver-factura',f.id,'Ver comprobante')]],filas,'Emite una factura desde una orden terminada.');
  $('#contenido').dataset.facturas=JSON.stringify(filas);
}
async function verUsuarios() {
  const filas=await api('/usuarios');
  $('#contenido').innerHTML=cabecera('Asigna un rol para definir los permisos de cada usuario.','Crear usuario')+tabla([['Nombre','nombre'],['Usuario','usuario'],['Rol',u=>escapar(etiqueta(u.rol))],['',u=>u.id===usuario.id?'<span class="muted">Tu usuario</span>':boton('rol',u.id,'Cambiar rol')]],filas)+`<div class="tarjeta"><h3>Permisos por rol</h3><p class="muted">Administrador: acceso global y configuración.<br>Recepción: clientes, vehículos, ingresos, facturas y entregas.<br>Mecánico: diagnósticos, repuestos y estados de sus órdenes asignadas.<br>Bodeguero: inventario, entradas y despacho de solicitudes.</p></div>`;
  $('#nuevo').onclick=()=>formulario('Crear usuario',campo('nombre','Nombre completo','text','','maxlength="100"')+campo('usuario','Usuario','text','','pattern="[a-zA-Z0-9._-]{3,60}" title="De 3 a 60 letras, números, punto, guion o guion bajo" autocomplete="off"')+campo('clave','Contraseña','password','','minlength="8" maxlength="72" autocomplete="new-password"')+selector('rol','Rol',roles.map(r=>`<option value="${r}">${etiqueta(r)}</option>`).join('')),d=>api('/usuarios','POST',d));
}
async function verConfiguracion() {
  const c=await api('/configuracion');
  $('#contenido').innerHTML=`<div class="tarjeta"><h3>Datos del taller</h3><p class="muted">Se muestran en la aplicación y al imprimir comprobantes.</p><form id="configuracion" class="rejilla">${campo('nombre','Nombre del taller','text',c.nombre,'maxlength="100"')}<label>Teléfono<input name="telefono" value="${escapar(c.telefono)}" maxlength="30"></label><label class="ancho">Dirección<input name="direccion" value="${escapar(c.direccion)}" maxlength="200"></label><div class="acciones ancho"><button class="principal">Guardar cambios</button></div></form></div>`;
  $('#configuracion').onsubmit=async e=>{
    e.preventDefault();e.submitter.disabled=true;
    try{const d=Object.fromEntries(new FormData(e.target));await api('/configuracion','PUT',d);configuracion=d;$('#marca-taller').textContent=d.nombre;aviso('Datos del taller actualizados.');}catch(err){aviso(err.message,true);}finally{e.submitter.disabled=false;}
  };
}
async function verHistorial() {
  $('#contenido').innerHTML=cabecera('Registro de las operaciones realizadas en el sistema.')+tabla([['Fecha',h=>fecha(h.fecha)],['Usuario','usuario'],['Acción',h=>escapar(h.accion.replaceAll('_',' '))],['Detalle','detalle']],await api('/historial'));
}
async function accion(nombre,id) {
  if(nombre==='detalle')return verDetalle(id);
  if(nombre==='diagnosticar')return formulario('Agregar diagnóstico · #'+id,area('descripcion','Fallas detectadas')+area('reparacion','Reparación requerida'),d=>api(`/ordenes/${id}/diagnosticos`,'POST',d));
  if(nombre==='estado') {
    const o=JSON.parse($('#cuerpo-modal').dataset.orden);
    const siguiente=o.estado==='DIAGNOSTICADO'?'EN_REPARACION':'TERMINADO';
    return formulario(etiqueta(siguiente)+' · #'+id,`<p class="nota ancho">${siguiente==='TERMINADO'?'Confirma que el trabajo está completo y registra el importe final de mano de obra. No puede haber solicitudes pendientes.':'La orden pasará a reparación.'}</p>`+campo('manoObra','Mano de obra (Q)','number',o.mano_obra,'min="0" max="99999999.99" step="0.01"'),d=>api(`/ordenes/${id}/estado`,'PUT',{...d,estado:siguiente}));
  }
  if(nombre==='solicitar') {
    const repuestos=await api('/repuestos');
    if(!repuestos.length)throw new Error('Bodega debe registrar repuestos antes de solicitarlos.');
    formulario('Solicitar repuesto · #'+id,selector('repuestoId','Repuesto',opciones(repuestos,'id',r=>`${r.codigo} · ${r.nombre} (${r.stock} disponibles)`))+campo('cantidad','Cantidad','number',1,'min="1" max="2147483647"')+'<div class="ancho"><button id="consultar" type="button">Consultar existencia</button><p id="resultado-stock" class="muted" aria-live="polite"></p></div>',async d=>{
      const consulta=await api(`/repuestos/${d.repuestoId}/existencia?cantidad=${d.cantidad}`);
      if(!consulta.disponible)throw new Error('No hay existencias suficientes para esta cantidad.');
      await api(`/ordenes/${id}/solicitudes`,'POST',d);
    });
    $('#consultar').onclick=async()=>{
      try{const f=$('#formulario');const rid=f.elements.repuestoId.value;const cantidad=Number(f.elements.cantidad.value);if(!rid || cantidad<1)throw new Error('Selecciona un repuesto y una cantidad positiva.');const c=await api(`/repuestos/${rid}/existencia?cantidad=${cantidad}`);$('#resultado-stock').textContent=`Existencia actual: ${c.repuesto.stock}. ${c.disponible?'Cantidad disponible.':'Cantidad insuficiente.'}`;}catch(e){$('#resultado-stock').textContent=e.message;}
    };
    return;
  }
  if(nombre==='asignar') {
    const mecanicos=await api('/mecanicos');
    return formulario('Reasignar mecánico · #'+id,selector('mecanicoId','Nuevo responsable',opciones(mecanicos,'id','nombre')),d=>api(`/ordenes/${id}/mecanico`,'PUT',d));
  }
  if(nombre==='entrada')return formulario('Entrada de inventario',campo('cantidad','Cantidad recibida','number',1,'min="1" max="2147483647"')+campo('motivo','Motivo / referencia','text','','maxlength="300"'),d=>api(`/repuestos/${id}/entradas`,'POST',d));
  if(nombre==='despachar')return formulario('Resolver solicitud #'+id,'<p class="nota ancho">El despacho comprueba las existencias actuales y descuenta la cantidad solicitada. El rechazo no modifica el inventario.</p>'+selector('estado','Acción','<option value="DESPACHADO">Despachar repuestos</option><option value="RECHAZADO">Rechazar solicitud</option>'),d=>api('/solicitudes/'+id,'PUT',d));
  if(nombre==='rol')return formulario('Asignar rol',selector('rol','Rol',roles.map(r=>`<option value="${r}">${etiqueta(r)}</option>`).join('')),d=>api(`/usuarios/${id}/rol`,'PUT',d));
  if(nombre==='facturar' || nombre==='entregar')return formulario(nombre==='facturar'?'Emitir factura · #'+id:'Registrar salida · #'+id,`<p class="nota ancho">${nombre==='facturar'?'Se emitirá un comprobante interno con la mano de obra y los repuestos despachados. El importe queda guardado y no se puede editar.':'Confirma la entrega del vehículo al cliente. Se registrará la fecha de salida.'}</p>`,()=>api(`/ordenes/${id}/${nombre==='facturar'?'factura':'entrega'}`,'POST'));
  if(nombre==='ver-factura') {
    const f=JSON.parse($('#contenido').dataset.facturas).find(f=>f.id===id);
    abrir('Comprobante interno #'+id,`<h3>${escapar(configuracion.nombre)}</h3><p class="muted">${escapar(configuracion.direccion)}<br>${escapar(configuracion.telefono)}</p><p class="muted">${fecha(f.fecha)} · Orden #${f.orden_id} · ${escapar(f.placa)}</p><div class="tarjeta"><strong>${escapar(f.cliente_nombre)}</strong><p>NIT: ${escapar(f.cliente_nit)}</p></div>${tabla([['Concepto','concepto'],['Importe',r=>dinero(r.importe)]],[{concepto:'Mano de obra',importe:f.mano_obra},{concepto:'Repuestos despachados',importe:f.repuestos}])}<p class="factura-total">Total ${dinero(f.total)}</p><p class="muted">Comprobante interno del taller. No es un documento tributario FEL.</p><div class="acciones"><button id="imprimir" class="principal">Imprimir</button></div>`);
    $('#imprimir').onclick=()=>window.print();
  }
}
document.addEventListener('click',async e=>{
  const b=e.target.closest('[data-accion]');if(!b)return;
  b.disabled=true;
  try{
    // Se reutiliza una sola ventana para los formularios y el detalle.
    if(b.closest('dialog') && b.dataset.accion!=='detalle')$('#modal').close();
    await accion(b.dataset.accion,Number(b.dataset.id));
  }catch(err){aviso(err.message,true);}finally{b.disabled=false;}
});
$('#cerrar-modal').onclick=()=>$('#modal').close();
$('#login').onsubmit=async e=>{
  e.preventDefault();const b=e.submitter;b.disabled=true;$('#error-login').textContent='';
  try{await token();await api('/login','POST',new URLSearchParams(new FormData(e.target)));e.target.reset();await token();await iniciar();}
  catch(err){$('#error-login').textContent=err.message;}
  finally{b.disabled=false;}
};
$('#salir').onclick=async()=>{
  try{await api('/logout','POST');usuario=null;$('#aplicacion').hidden=true;$('#acceso').hidden=false;await token();}catch(e){aviso(e.message,true);}
};
(async()=>{
  try{await token();await iniciar();}
  catch(e){if(!e.message.includes('sesión'))$('#error-login').textContent=e.message;}
})();
