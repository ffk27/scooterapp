

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "WiFiCreds.h"

#define SERVICE_UUID         "508c16b9-7ccc-46a8-907d-802b91e2b1f8"
#define CHARACTERISTIC_UUID  "7a6082ec-79ea-45da-8129-0709de8f5d50"

#define REEKS_SIZE 20
BLECharacteristic *pCharacteristic;

bool deviceConnected = false;

void setup() {
  Serial.begin(9600);
  initBluetooth();
}

void loop() {
//  if (deviceConnected) {
//    pCharacteristic->setValue(random(256) + "");
//    pCharacteristic->notify();
//  }
  Serial.println("loop");
  delay(1000);
}

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("connected");
      deviceConnected = true;
    };
 
    void onDisconnect(BLEServer* pServer) {
      Serial.println("disconnected");
      deviceConnected = false;
      pServer->getAdvertising()->start();
    }
};

void initBluetooth() {
  BLEDevice::init("xiao-esp32-c3");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);               
  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pCharacteristic->setValue("sensor data");
  //pCharacteristic->addDescriptor(new BLE2902());
  pServer->getAdvertising()->start();
//  BLEDevice::startAdvertising();
//  pAdvertising->addServiceUUID(SERVICE_UUID);
//  pAdvertising->setScanResponse(true);
//  pAdvertising->setMinPreferred(0x06);
//  pAdvertising->setMinPreferred(0x12);  
}
