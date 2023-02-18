let ws;
const values = [];
let svgFn;
let activeCircle;
let activeText;
let conn_status;

const documentHeight = () => {
    const doc = document.documentElement;
    doc.style.setProperty('--doc-height', `${window.innerHeight}px`);
}
window.addEventListener("resize", documentHeight);
documentHeight();

function onReceive(json) {
    const data = JSON.parse(json);
    const rpm = parseInt(data['rpm']);
    svgFn && svgFn(rpm);
    document.querySelector('#metric2 .value').textContent = rpm;
}

function connected() {
    conn_status.style.color = 'green';
    conn_status.title = 'verbonden';
}

function disconnected() {
    conn_status.style.color = 'red';
    conn_status.title = 'geen verbinding';
}

//function startConnection(ip) {
//    if (ip) {
//        window.localStorage.setItem("socket_ip", ip);
//        const inputUrl = document.querySelector('#websocket-ip');
//        inputUrl.value = ip;
//    } else {
//        ip = window.localStorage.getItem("socket_ip");
//    }
//    if (!ip) return;
//    if (ws) ws.close();
//    conn_status.color = 'gray';
//
//    ws = new WebSocket(`ws://${ip}:8123`);
//    ws.onmessage = (ev) => {
//        onReceive(ev.data);
//    };
//    ws.onopen = () => {
//        connected();
//    }
//    ws.onclose = (ev) => {
//        disconnected();
//        ws = null;
//        setTimeout(() => {
//            if (!ws) { // retry opening connection after 1 sec
//                startConnection();
//            }
//        }, 1000);
//    }
//
//}

window.onload = () => {
    conn_status = document.querySelector('#connection-status');
    //startConnection();
    const settingsBtn = document.querySelector('#btn-settings');
    settingsBtn.onclick = () => {
        modal.style.display = 'block';
    };
    const modalDismiss = document.querySelector('#modal-dismiss');
    modalDismiss.onclick = () => {
        modal.style.display = 'none';
    }
    const modalOk = document.querySelector('#modal-ok');
    const inputUrl = document.querySelector('#websocket-ip');
    inputUrl.value = window.localStorage.getItem("socket_ip") || '';
    modalOk.onclick = () => {
        modal.style.display = 'none';
        window.localStorage.setItem("socket_ip", inputUrl.value);
        startConnection();
    }
    modal.onclick = (ev) => {
        if (ev.target == modal) {
            modal.style.display = 'none';
        }
    }

    const section_graph = document.querySelector('section#graph');
    const svg = section_graph.appendChild(document.createElementNS('http://www.w3.org/2000/svg', 'svg'));
    const svgWidth = section_graph.offsetWidth;
    const svgHeight = section_graph.offsetHeight;
    svg.setAttribute("width", svgWidth);
    svg.setAttribute("height", svgHeight);
    svg.setAttribute("viewBox", "0 0 " + svgWidth + " " + svgHeight);

    let t = 0;

    svgFn = (value) => {
          const viewBox = svg.getAttribute("viewBox").split(' ').map(v => parseInt(v));
          const minX = viewBox[0];
          t++;
          if (values.length > 0) {
              const valuePrev = values[values.length-1];
              const y1 = Math.floor(svgHeight - (svgHeight / 10000 * valuePrev[1]));
              const y2 = Math.floor(svgHeight - (svgHeight / 10000 * value));
              const x2 = Math.floor(svgWidth + minX + 3);
              const x1 = x2 - 3;
              const line = svg.appendChild(document.createElementNS('http://www.w3.org/2000/svg','line'));
              line.setAttribute('x1', `${x1}`);
              line.setAttribute('y1', `${y1}`);
              line.setAttribute('x2', `${x2}`);
              line.setAttribute('y2', `${y2}`);
              line.style = 'stroke:brown;stroke-width:2';
              const circle = svg.appendChild(document.createElementNS('http://www.w3.org/2000/svg','circle'));
              circle.setAttribute('cx', x1);
              circle.setAttribute('cy', y1);
              circle.setAttribute('r', 10);
              circle.setAttribute('fill-opacity', '0');
              circle.onclick = () => {
                  if (activeCircle) { // hide the last clicked circle
                      activeCircle.setAttribute('fill-opacity', '0');
                  }
                  if (activeText) {
                      activeText.remove();
                  }
                  circle.setAttribute('r', 3);
                  circle.setAttribute('fill-opacity', '1');
                  activeText = svg.appendChild(document.createElementNS('http://www.w3.org/2000/svg','text'));
                  activeText.setAttribute('x', x1 - 15);
                  activeText.setAttribute('y', y1 - 10);
                  activeText.textContent = '' + value;
              }
          }
          values.push([t, value]);

          svg.setAttribute('viewBox', `${minX+3} 0 ${svgWidth} ${svgHeight}`);
    }

//      setInterval(()=>{
//
//        console.log();
//        svgFn(Math.floor(Math.random() * 6000) + 1800);
//      }, 333);
};

function drawChart() {
    const canvas = document.querySelector('#canvas-chart');
    var ctx = canvas.getContext("2d");
    console.log(canvas);
    ctx.moveTo(0, 0);
    ctx.lineTo(200, 100);
    ctx.stroke();
}