
let ws;

function startConnection() {
    const url = window.localStorage.getItem("socket_url");
    if (!url) return;
    const conn_status = document.querySelector('#connection-status');
    if (ws) ws.close();
    conn_status.color = 'gray';
    ws = new WebSocket(url);
    ws.onmessage = (ev) => {
        console.log(ev.data);
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
    const modal = document.querySelector('#modal');
    settingsBtn.onclick = () => {
        modal.style.display = 'block';
    };
    const modalDismiss = document.querySelector('#modal-dismiss');
    modalDismiss.onclick = () => {
        modal.style.display = 'none';
    }
    const modalOk = document.querySelector('#modal-ok');
    const inputUrl = document.querySelector('#websocket-url');
    inputUrl.value = window.localStorage.getItem("socket_url") || '';
    modalOk.onclick = () => {
        modal.style.display = 'none';
        window.localStorage.setItem("socket_url", inputUrl.value);
        startConnection();
    }
    modal.onclick = (ev) => {
        if (ev.target == modal) {
            modal.style.display = 'none';
        }
    }

    drawChart();
    window.onresize = () => {
        drawChart();
    }
};

function drawChart() {
    const canvas = document.querySelector('#canvas-chart');
    var ctx = canvas.getContext("2d");
    console.log(canvas);
    ctx.moveTo(0, 0);
    ctx.lineTo(200, 100);
    ctx.stroke();
}