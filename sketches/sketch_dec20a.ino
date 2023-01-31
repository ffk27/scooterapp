#include <ESP8266WiFi.h>
#include "WiFiUdp.h"
#include "NTPClient.h"
#include <WebSocketsServer.h>
#include <ESP8266WebServer.h>
#include <WiFiCreds.h>

#define REEKS_SIZE 20

WebSocketsServer    webSocket = WebSocketsServer(80);
unsigned long epochTime; // in seconds
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 0);
unsigned long t1 = 0;
int mn = 0;
unsigned long values[REEKS_SIZE];
bool clientConnected = false;
bool engineOn = false;

void setup() {
  Serial.begin(9600);
  wifi_connect(WIFI_SSID, WIFI_PASS);
  timeClient.begin();
  timeClient.update();
  epochTime = timeClient.getEpochTime();
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

String pad(String input) {
  int len = input.length();
  for (int i=0;i<3-len;i++) {
    input = '0' + input;
  }
  return input;
}

void loop() {
  int value = analogRead(A0);
  while(!clientConnected || !engineOn) {
    webSocket.loop();
    delay(50);
    if (value > 500) {
      engineOn = true;
    }
  }
  unsigned long tmic = micros();
  if (mn < REEKS_SIZE) {
    if (value > 500) {
      if (t1 == 0) {
        t1 = tmic;
      }
      else if (tmic - 3000 > t1 + 3000) { //  
        unsigned long td = tmic - t1;
        values[mn] = 60000000 / td;
        mn++;
        t1 = tmic;
      }  
    } else if (tmic - t1 > 1000000) { // 1 seconde sinds laatste signaal, motor is uit
      engineOn = false;
    }
  } else {
    int tm = millis();
    String epoch = String(epochTime + tm / 1000) + pad(String(tm % 1000));
    qsort(values, REEKS_SIZE, sizeof(unsigned long), cmpfunc);
    String rpm = String(values[REEKS_SIZE/2]); // mediaan
    String json = "{\"time\": " + epoch + ", \"rpm\":" + rpm + "}";
    webSocket.broadcastTXT(json);
    webSocket.loop();
    mn = 0;
    t1 = 0;
  }
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

int cmpfunc (const void * a, const void * b) {
   return ( *(int*)a - *(int*)b );
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      clientConnected = false;
      break;

    case WStype_CONNECTED: {
      clientConnected = true;
      break;
    }
  }

}