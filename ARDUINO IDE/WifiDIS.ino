#include <WiFi.h>
#include <WiFiManager.h>
#include <FirebaseESP32.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <PZEM004Tv30.h>

// ================= FIREBASE =================
#define API_KEY "AIzaSyBegzMkA5w-5VowpsRT5VFvFoK7q_-tEFk"
#define DATABASE_URL "https://smartx-c590e-default-rtdb.firebaseio.com/"

// ================= RELAYS =================
#define RELAY_S1 26
#define RELAY_S2 27
#define RELAY_ON  LOW
#define RELAY_OFF HIGH

// ================= PZEM =================
#define PZEM_RX 16
#define PZEM_TX 17
HardwareSerial PZEMSerial(2);
PZEM004Tv30 pzem(PZEMSerial, PZEM_RX, PZEM_TX);

// ================= FIREBASE =================
FirebaseData fbdo;
FirebaseData stream;
FirebaseAuth auth;
FirebaseConfig config;

// ================= TIMING =================
unsigned long lastPoll = 0;
unsigned long lastPZEMRead = 0;
#define POLL_MS 500
#define PZEM_INTERVAL 1000

// ================= STATE =================
bool dirty = true;
bool phoneConnected = false;
bool automaticCutoffEnabled = false;
bool priorityMasterEnabled = false;

String s1Status="OFF", s2Status="OFF";
String s1Appliance="", s2Appliance="";

bool prioFan1=false, prioFan2=false, prioFan3=false;
bool prioTV=false, prioFridge=false;

int lastRelayS1 = RELAY_OFF;
int lastRelayS2 = RELAY_OFF;

bool lastWifiState = true;

// ================= SAFE READ =================
bool readBool(const char* path){
  if(Firebase.getBool(fbdo, path)) return fbdo.boolData();
  return false;
}

String readString(const char* path){
  if(Firebase.getString(fbdo, path)) return fbdo.stringData();
  return "";
}

// ================= PRIORITY =================
bool isPriority(const String& a){
  if(!priorityMasterEnabled) return false;
  if(a=="Electric Fan 1") return prioFan1;
  if(a=="Electric Fan 2") return prioFan2;
  if(a=="Electric Fan 3") return prioFan3;
  if(a=="Television") return prioTV;
  if(a=="Refrigerator") return prioFridge;
  return false;
}

// ================= AUTO CUTOFF =================
bool shouldCutoff(){

  if(WiFi.status() != WL_CONNECTED){
    Serial.println("🚨 WIFI LOST → RELAYS OFF");
    return true;
  }

  if(!automaticCutoffEnabled) return false;
  if(!phoneConnected) return true;

  return false;
}

// ================= APPLY RELAY =================
void applyRelay(){

  bool cutoff = shouldCutoff();
  int newS1, newS2;

  if(isPriority(s1Appliance)) newS1 = RELAY_ON;
  else if(cutoff) newS1 = RELAY_OFF;
  else newS1 = (s1Status=="ON") ? RELAY_ON : RELAY_OFF;

  if(isPriority(s2Appliance)) newS2 = RELAY_ON;
  else if(cutoff) newS2 = RELAY_OFF;
  else newS2 = (s2Status=="ON") ? RELAY_ON : RELAY_OFF;

  if(newS1 != lastRelayS1){
    digitalWrite(RELAY_S1,newS1);
    lastRelayS1=newS1;
  }

  if(newS2 != lastRelayS2){
    digitalWrite(RELAY_S2,newS2);
    lastRelayS2=newS2;
  }

  Serial.print("WiFi=");
  Serial.print(WiFi.status()==WL_CONNECTED);
  Serial.print(" Phone=");
  Serial.print(phoneConnected);
  Serial.print(" S1=");
  Serial.print(s1Status);
  Serial.print(" S2=");
  Serial.println(s2Status);
}

// ================= POLL FIREBASE =================
void pollFirebase(){

  if(WiFi.status() != WL_CONNECTED) {
    phoneConnected = false;
    dirty = true;
    return;
  }

  automaticCutoffEnabled = readBool("settings/appliances/automatic_cutoff");
  priorityMasterEnabled  = readBool("settings/appliances/prioritized_appliances");

  prioFan1 = readBool("prioritized_appliances/priority_fan1");
  prioFan2 = readBool("prioritized_appliances/priority_fan2");
  prioFan3 = readBool("prioritized_appliances/priority_fan3");
  prioTV   = readBool("prioritized_appliances/priority_tv");
  prioFridge = readBool("prioritized_appliances/priority_fridge");

  s1Status = readString("sockets/s1/status");
  s2Status = readString("sockets/s2/status");
  s1Appliance = readString("sockets/s1/appliance_name");
  s2Appliance = readString("sockets/s2/appliance_name");

  phoneConnected = readBool("device/phone_connected");

  dirty=true;
}

// ================= STREAM =================
void streamCallback(StreamData d){ dirty=true; }
void streamTimeoutCallback(bool t){}

// ================= SETUP =================
void setup(){

  Serial.begin(115200);

  pinMode(RELAY_S1,OUTPUT);
  pinMode(RELAY_S2,OUTPUT);
  digitalWrite(RELAY_S1,RELAY_OFF);
  digitalWrite(RELAY_S2,RELAY_OFF);

  PZEMSerial.begin(9600,SERIAL_8N1,PZEM_RX,PZEM_TX);

  WiFi.mode(WIFI_STA);
  WiFiManager wm;

  if(!wm.autoConnect("SMARTX-SETUP","WattaPips")){
    ESP.restart();
  }

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.token_status_callback = tokenStatusCallback;

  Firebase.signUp(&config,&auth,"","");
  Firebase.begin(&config,&auth);
  Firebase.reconnectWiFi(true);

  fbdo.setBSSLBufferSize(8192,2048);
  stream.setBSSLBufferSize(8192,2048);
  stream.setResponseSize(4096);

  Firebase.setString(fbdo,"device/esp32/status","ONLINE");

  pollFirebase();
  applyRelay();

  Firebase.beginStream(stream,"/device");
  Firebase.setStreamCallback(stream,streamCallback,streamTimeoutCallback);
}

// ================= LOOP =================
void loop(){

  unsigned long now=millis();

  // ===== WIFI WATCHDOG =====
  bool wifiNow = (WiFi.status()==WL_CONNECTED);

  if(wifiNow!=lastWifiState){
    lastWifiState=wifiNow;

    if(!wifiNow){
      Serial.println("🚨 FORCE RELAYS OFF");
      digitalWrite(RELAY_S1,RELAY_OFF);
      digitalWrite(RELAY_S2,RELAY_OFF);
      lastRelayS1=RELAY_OFF;
      lastRelayS2=RELAY_OFF;
    }
    dirty=true;
  }

  if(wifiNow && Firebase.ready()) Firebase.readStream(stream);

  if(now-lastPoll>POLL_MS){
    lastPoll=now;
    if(wifiNow){
      pollFirebase();
    } else {
      // WiFi is gone — skip Firebase reads to avoid blocking hangs.
      // Force cutoff logic to re-run so relays turn off immediately.
      dirty = true;
    }
  }

  if(dirty){
    dirty=false;
    applyRelay();
  }

  if(now-lastPZEMRead>PZEM_INTERVAL){
    lastPZEMRead=now;

    float v=pzem.voltage();
    float c=pzem.current();
    float p=pzem.power();
    float e=pzem.energy();

    if(!isnan(v)&&WiFi.status()==WL_CONNECTED){
      Firebase.setFloat(fbdo,"pzem/voltage",v);
      Firebase.setFloat(fbdo,"pzem/current",c);
      Firebase.setFloat(fbdo,"pzem/power",p);
      Firebase.setFloat(fbdo,"pzem/energy",e);
    }
  }
}
