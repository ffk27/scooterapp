let ws;
const values = [];
let activeCircle;
let activeText;
let conn_status;
let svg;
let svgWidth;
let svgHeight;
//let t = 0;

const init = () => {
    const doc = document.documentElement;
    doc.style.setProperty('--doc-height', `${window.innerHeight}px`);
    const section_graph = document.querySelector('section#graph');
    if (section_graph) {
        svgWidth = section_graph.offsetWidth;
        svgHeight = section_graph.offsetHeight;
        //alert(svgWidth + " "  + svgHeight);
        conn_status = document.querySelector('#connection-status');
        section_graph.innerHTML = '';
        svg = section_graph.appendChild(document.createElementNS('http://www.w3.org/2000/svg', 'svg'));
        svg.setAttribute("width", svgWidth);
        svg.setAttribute("height", svgHeight);
        svg.setAttribute("viewBox", "0 0 " + svgWidth + " " + svgHeight);
    }
}
window.addEventListener("resize", init);
init();

const svgFn = (time, value) => {
      if (!svg) return;
      const viewBox = svg.getAttribute("viewBox").split(' ').map(v => parseInt(v));
      const minX = viewBox[0];
      //t++;
      let xdiff = 0;
      if (values.length > 0) {
          const valuePrev = values[values.length-1];
          const y1 = Math.floor(svgHeight - (svgHeight / 10000 * valuePrev[1]));
          const y2 = Math.floor(svgHeight - (svgHeight / 10000 * value));
          xdiff = svgWidth / 60000 * (time - valuePrev[0])
          const x2 = Math.floor(svgWidth + minX + xdiff);
          //alert(svgWidth / 60000)
          const x1 = x2 - xdiff;
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
              activeText.textContent = '' + valuePrev[1];
              activeCircle = circle;
          }
      }
      values.push([time, value]);

      svg.setAttribute('viewBox', `${minX+xdiff} 0 ${svgWidth} ${svgHeight}`);
}

function onReceive(json) {
    const data = JSON.parse(json);
    const rpm = parseInt(data['rpm']);
    const time = parseInt(data['time']);
    svgFn && svgFn(time, rpm);
    document.querySelector('#metric2 .value').textContent = rpm;
}

function connected() {
    if (conn_status) {
        conn_status.style.color = 'green';
        conn_status.title = 'verbonden';
    }
}

function disconnected() {
    if (conn_status) {
        conn_status.style.color = 'red';
        conn_status.title = 'geen verbinding';
    }
}

window.onload = () => {
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
};