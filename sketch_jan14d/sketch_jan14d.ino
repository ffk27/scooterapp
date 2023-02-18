#include <WiFi.h>
#include <WiFiMulti.h>
//#include "WiFiUdp.h"
//#include "NTPClient.h"
#include <WebSocketsServer.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>

#define MODE 1 // 0 = websocket, 1 = bluetooth
#define SERVICE_UUID         "508c16b9-7ccc-46a8-907d-802b91e2b1f8"
#define CHARACTERISTIC_UUID  "7a6082ec-79ea-45da-8129-0709de8f5d50"
#define WIFI_SSID "DIRECT-sa-android"
#define WIFI_PASS "03f3747d-250d-414f-9de8-be4d7b28c6d3"
#define BLUETOOTH_NAME "xiao-esp32-c3"

#define TMIN 6000 // minimale tijd tussen twee metingen in µs. Bij 8000 toeren neemt één omwenteling 7,5 ms seconden in beslag.
#define REEKS_SIZE 20 // aantal metingen, waaruit de mediaan zal worden genomen. Een meting is de tijd tussen twee hoge spanningen
#define DREMPEL 500 // drempelspanning 3,3v / 4096 * 500 = 400mV, pull-up-weerstand van 56 kilo-ohm gebruikt.

#if MODE==0
  WebSocketsServer    webSocket = WebSocketsServer(8123);
  //WiFiUDP ntpUDP;
  //NTPClient timeClient(ntpUDP, "pool.ntp.org", 0);
  //WiFiMulti wifiMulti;
  WiFiClient client;
#endif
#if MODE==1
  BLECharacteristic *pCharacteristic;
#endif

unsigned long epochTime = 0; // in seconds
unsigned long t1 = 0; // tpm
unsigned long ts = 0; // time values send
int mn = 0;
unsigned long values[REEKS_SIZE];
bool clientConnected = false; // Bluetooth/Websocket client
bool engineOn = false;

void setup() {
  Serial.begin(9600);  
  #if MODE==0
    wifi_connect(WIFI_SSID, WIFI_PASS);
    //timeClient.begin();
    //timeClient.update();
    //epochTime = timeClient.getEpochTime();
    //Serial.println("Time: " + String(epochTime));

    webSocket.begin();
    webSocket.onEvent(webSocketEvent);
  #endif
  pinMode(A0,INPUT);
  #if MODE==1
    initBluetooth();
  #endif
}

String pad(String input) {
  int len = input.length();
  for (int i=0;i<3-len;i++) {
    input = '0' + input;
  }
  return input;
}


void loop() {
  // uint8_t value = random(100) + 33;
  // Serial.printf("*** NOTIFY: %d ***\n", value);
  // pCharacteristic->setValue(&value, 1);
  // pCharacteristic->notify();
  // delay(333);
  #if MODE==0
    while (WiFi.status() != WL_CONNECTED) {
      Serial.println("reconnecting");
      wifi_connect(WIFI_SSID, WIFI_PASS);
    }
  #endif
  //int value = analogRead(A0);
  int value = random(502);
  if(!clientConnected || !engineOn) {// 
    #if MODE==0
      webSocket.loop();
    #endif
    //Serial.println("not connected" + String(value));
    if (value > DREMPEL) {
      engineOn = true;
    } else {
      //Serial.println(String(value));
    }
    return;
  }
  unsigned long tmic = micros();
  if (mn < REEKS_SIZE) {
    if (value > DREMPEL) {
      if (t1 == 0) {
        t1 = tmic;
      }
      else if (tmic - TMIN > t1) { //   tmic - 3000 > t1 + 3000  // 
        unsigned long td = tmic - t1;
        values[mn] = 60000000 / td;
        mn++;
        t1 = tmic;
      }  
    }
    
    else if (tmic - t1 > 1000000) { // 1 seconde sinds laatste signaal, motor is uit
      engineOn = false;
    }
  } else {
    while(millis() - ts < 333) { // maximaal 3 berichten per seconde
      #if MODE==0
        webSocket.loop();
      #endif
    }
    
    
    qsort(values, REEKS_SIZE, sizeof(unsigned long), cmpfunc);
    
    unsigned long tm = millis();
    unsigned long rpm = values[REEKS_SIZE/2]; // mediaan
    Serial.println(String(rpm) + "");
    
    #if MODE==0
      
      String rpm = String(rpm);
      String epoch = String(epoch + tm / 1000) + pad(String(tm % 1000));
      String json = "{\"time\":" + epoch + ", \"rpm\":" + rpm + "}";
      webSocket.broadcastTXT(json);
    #endif
    #if MODE==1
      
      //String sensor_data = epoch + "," + rpm;
      uint8_t sensor_data[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      sensor_data[3] = tm;
      sensor_data[2] = tm >> 8;
      sensor_data[1] = tm >> 16;
      sensor_data[0] = tm >> 24;
      sensor_data[7] = rpm;
      sensor_data[6] = rpm >> 8;
      sensor_data[5] = rpm >> 16;
      sensor_data[4] = rpm >> 24;
      //a = epochTime + tm; 
      pCharacteristic->setValue(&sensor_data[0], 16);
      pCharacteristic->notify();
    #endif
    ts = millis();
    #if MODE==0
      webSocket.loop();
    #endif
    mn = 0;
    t1 = 0;
  }
}

int cmpfunc (const void * a, const void * b) {
   return ( *(unsigned long*)a - *(unsigned long*)b );
}

#if MODE==0
  void wifi_connect(char* ssid, char* password) {
    WiFi.begin(ssid, password);
    //Serial.print("Connecting to WiFi");
    //Serial.print(WIFI_SSID);
    while (WiFi.status() != WL_CONNECTED)
    {
      delay(300);
      Serial.print(".");
    }
    Serial.println("Connected to " + WiFi.SSID());
    Serial.print("Connected! IP address: ");
    Serial.println(WiFi.localIP());
    Serial.println(WiFi.gatewayIP());

    IPAddress gateway = WiFi.gatewayIP();
    if (client.connect(gateway, 3123)) {
      Serial.println("receiving time");
//      while (client.available()) {
//        char c = client.read();
//        Serial.print(c);
//        delay(100);
//      }
    } else {
      Serial.println("failed");
    }
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
#endif

#if MODE==1
  class ServerCallbacks: public BLEServerCallbacks {
      void onConnect(BLEServer* pServer) {
        Serial.println("connected");
        clientConnected = true;
        pServer->getAdvertising()->start();
      };
   
      void onDisconnect(BLEServer* pServer) {
        Serial.println("disconnected");
        
        if (pServer->getConnectedCount() == 0) {
          clientConnected = false;
        }
        pServer->getAdvertising()->start();
      }
  };
  
  void initBluetooth() {
    BLEDevice::init(BLUETOOTH_NAME);
    BLEServer *pServer = BLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());
    BLEService *pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID, 
                      //BLECharacteristic::PROPERTY_READ   |
                      // BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY //|
                      // BLECharacteristic::PROPERTY_INDICATE    
    );               

    BLEDescriptor *descriptor = new BLE2902();
    descriptor->setValue("sensor_data");
    pCharacteristic->addDescriptor(descriptor);

    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12); 
    pServer->getAdvertising()->start();

    pService->addCharacteristic(pCharacteristic);
    pService->start();
  }
#endif
