window.localStorage.setItem("socket_url", "ws://192.168.2.20");
const ws = new WebSocket(window.localStorage.getItem("socket_url"));
ws.onmessage = (ev) => {
    console.log(ev.data);
};

window.onload = () => {
    const settingsBtn = document.querySelector('#btn-settings');
    const modal = document.querySelector('#modal');
    settingsBtn.onclick = () => {
        modal.style.display = 'block';
    };
    const modalDismiss = document.querySelector('#modal-dismiss');
    modalDismiss.onclick = () => {
        modal.style.display = 'none';
    }
    modal.onclick = (ev) => {
        if (ev.target == modal) {
            modal.style.display = 'none';
        }
    }
};