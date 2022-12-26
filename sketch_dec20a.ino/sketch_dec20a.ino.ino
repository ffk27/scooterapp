#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <WebSocketsServer.h>
#include <ESP8266WebServer.h>
#include <Hash.h>


// Set WiFi credentials
#define WIFI_SSID ""
#define WIFI_PASS ""

WebSocketsServer    webSocket = WebSocketsServer(80);

void setup() {
  // Setup serial port
  Serial.begin(115200);
  Serial.println();

  wifi_connect(WIFI_SSID, WIFI_PASS);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) { // reconnect
    wifi_connect(WIFI_SSID, WIFI_PASS);
  }
  webSocket.loop();
  webSocket.broadcastTXT("test123");
  delay(50);

}

void wifi_connect(char* ssid, char* password) {
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Connecting to ");
  Serial.print(WIFI_SSID);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(100);
    Serial.print(".");
  }
  Serial.print("Connected! IP address: ");
  Serial.println(WiFi.localIP());
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.printf("[%u] Disconnected!\n", num);
      break;

    case WStype_CONNECTED: {
        IPAddress ip = webSocket.remoteIP(num);
        Serial.printf("[%u] Connected from %d.%d.%d.%d url: %s\n", num, ip[0], ip[1], ip[2], ip[3], payload);
        // send message to client
        webSocket.sendTXT(num, "0");
      }
      break;
  }

}
