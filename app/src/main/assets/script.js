let ws;
const values = [];

const documentHeight = () => {
    const doc = document.documentElement;
    doc.style.setProperty('--doc-height', `${window.innerHeight}px`);
}
window.addEventListener("resize", documentHeight);
documentHeight();

function startConnection(ip) {
    if (ip) {
        window.localStorage.setItem("socket_ip", ip);
        const inputUrl = document.querySelector('#websocket-ip');
        inputUrl.value = ip;
    } else {
        ip = window.localStorage.getItem("socket_ip");
    }
    if (!ip) return;
    const conn_status = document.querySelector('#connection-status');
    if (ws) ws.close();
    conn_status.color = 'gray';
    ws = new WebSocket(`ws://${ip}`);
    ws.onmessage = (ev) => {
        const data = JSON.parse(ev.data);
        console.log(ev.data);
        document.querySelector('#metric2 .value').textContent = parseInt(data['rpm']);
    };
    ws.onopen = () => {
        conn_status.style.color = 'green';
        conn_status.title = 'verbonden';
    }
    ws.onclose = () => {
        conn_status.style.color = 'red';
        conn_status.title = 'geen verbinding';
    }
}

window.onload = () => {
    startConnection();
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

                let t = 0;
                setInterval(()=>{
                    const svg = document.querySelector('section#graph svg');
                    const svgWidth = parseInt(svg.getAttribute('width'));
                    const svgHeight = parseInt(svg.getAttribute('height'));
                    const viewbox = svg.getAttribute('viewBox').split(' ');
                    const minX = parseInt(viewbox[0]);
                    const value = Math.floor(Math.random() * 300) + 300;
                    t++;
                    if (values.length > 0) {
                        const valuePrev = values[values.length-1];
                        const y1 = Math.floor(svgHeight - (svgHeight / 1000 * valuePrev[1]));
                        const y2 = Math.floor(svgHeight - (svgHeight / 1000 * value));
                        const x2 = Math.floor(svgWidth + minX + 10);
                        const x1 = x2 - 10;
                        const line = svg.appendChild(document.createElementNS('http://www.w3.org/2000/svg','line'));
                        line.setAttribute('x1', `${x1}`);
                        line.setAttribute('y1', `${y1}`);
                        line.setAttribute('x2', `${x2}`);
                        line.setAttribute('y2', `${y2}`);
                        line.style = 'stroke:rgb(0,255,0);stroke-width:2';
                    }
                    values.push([t, value]);

                    svg.setAttribute('viewBox', `${minX+10} 0 ${svgWidth} ${svgHeight}`);
                },333);
};

function drawChart() {
    const canvas = document.querySelector('#canvas-chart');
    var ctx = canvas.getContext("2d");
    console.log(canvas);
    ctx.moveTo(0, 0);
    ctx.lineTo(200, 100);
    ctx.stroke();
}