let ws;

const documentHeight = () => {
    const doc = document.documentElement;
    doc.style.setProperty('--doc-height', `${window.innerHeight}px`);
}
window.addEventListener("resize", documentHeight);
documentHeight();

function startConnection() {
    const ip = window.localStorage.getItem("socket_ip");
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
    const modal = document.querySelector('#modal');
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

    alert(navigator.bluetooth);
    // try {
    //     alert(ble);
    // } catch (e) {
    //     alert(e);
    // }
    
    // ble.scan([], 5, function(device) {
    //     alert(JSON.stringify(device));
    // }, (error) => {
    //     alert(error);
    // });
};

function drawChart() {
    const canvas = document.querySelector('#canvas-chart');
    var ctx = canvas.getContext("2d");
    console.log(canvas);
    ctx.moveTo(0, 0);
    ctx.lineTo(200, 100);
    ctx.stroke();
}