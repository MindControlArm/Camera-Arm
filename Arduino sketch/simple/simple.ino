#include <Wire.h>
#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#include <Servo.h> 

Servo servo1; 
Servo servo2; 

AndroidAccessory acc("Mind Control Project TOM 2014",
"MCP",
"MCP Mega ADK",
"0.1",
"http://www.tomisrael.org",
"0000000087654321");

#define OUT_CHANNEL0 22
#define OUT_CHANNEL1 24

#define IN_CHANNEL0 22
#define IN_CHANNEL1 24
#define IN_CHANNEL2 26

#define  RELAY1         A0
#define  RELAY2         A1

#define  BUTTON1        A6
#define  BUTTON2        A7
#define  BUTTON3        A8

// These should match the command definitions in the commands section 
// of the .json file
#define WHICHDEVICE 0
#define ARMROTATE 1
#define GRIPPERUPDOWN 2
#define GRIPPERLEFTRIGHT 3
#define GRIPPERROTATE 4
#define ARM 5
#define GRIPPER 6
#define GRIPPERLEFTRIGHTENDPOINT 7
#define GRIPPERUPDOWNENDPOINT 8
#define GRIPPERROTATEENDPOINT 9

#define STOP 10

#define ZOOM 20
#define SNAP 21
#define CAMERAPOWER 22
#define PANUPDOWN 23
#define PANLEFTRIGHT 24

#define PAN_UP_START 25
#define PAN_UP_STOP 26
#define PAN_DOWN_START 27
#define PAN_DOWN_STOP 28
#define PAN_LEFT_START 29
#define PAN_LEFT_STOP 30
#define PAN_RIGHT_START 31
#define PAN_RIGHT_STOP 32

int panLR = 0;
int panUD = 0;

int degreeLR = 90;
int degreeUD = 30;

int MAX_RANGE_UD = 90;
int MIN_RANGE_UD = 0;

int MAX_RANGE_LR = 130;
int MIN_RANGE_LR = 0;


int CONSTANT_DEGREE_JUMP = 1;
int loopCounter = 0;

void setup();
void loop();

void init_buttons()
{
  pinMode(BUTTON1, INPUT);
  pinMode(BUTTON2, INPUT);
  pinMode(BUTTON3, INPUT);

  // enable the internal pullups
  digitalWrite(BUTTON1, HIGH);
  digitalWrite(BUTTON2, HIGH);
  digitalWrite(BUTTON3, HIGH);
}

void init_relays()
{
  pinMode(RELAY1, OUTPUT);
  pinMode(RELAY2, OUTPUT);
}


byte b1, b2, b3, b4, c;

void setup()
{
  Serial.begin(115200);
  Serial.print("\r\nStart\r\n");

  init_relays();
  init_buttons();

  b1 = digitalRead(BUTTON1);
  b2 = digitalRead(BUTTON2);
  b3 = digitalRead(BUTTON3);
  c = 0;

  acc.powerOn();
  
  servo1.attach(10);
  servo1.write(90); 
  servo2.attach(9);
  servo2.write(90);
  Serial.println("initial setup completed");
}

void loop()
{
  byte err;
  byte idle;
  static byte count = 0;
  byte msg[3];
  long touchcount;

  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1);
    int i;
    byte b;
    byte b1 = 0;
    uint16_t val;
    int x, y;
    char c0;

    if (len > 0) {
      String printthis = "\r\nReceived command ";

      Serial.print(printthis + msg[0] + "," + msg[1] + "," + msg[2] + "\r\n");

      // assumes only one command per packet
      if (msg[0] == WHICHDEVICE) {
        msg[0] = WHICHDEVICE;
        msg[1] = 1; // This needs to be changed later
        msg[2] = 0; // Currently unused
        String printthis2 = "\r\nSent message: ";
        Serial.println(printthis2 + msg[0] + msg[1] + msg[2]);
        acc.write(msg, 3);
      } 
      else if (msg[0] == ARMROTATE)
        digitalWrite(RELAY1, msg[1] ? HIGH : LOW);
      else if (msg[0] == GRIPPERUPDOWN) 
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == GRIPPERLEFTRIGHT) 
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == GRIPPERROTATE) 
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == ARM) 
        digitalWrite(OUT_CHANNEL0, msg[1] ? HIGH : LOW);
      else if (msg[1] == GRIPPER)
        digitalWrite(OUT_CHANNEL0, msg[1] ? HIGH : LOW);
      else if (msg[0] == STOP) 
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == ZOOM)
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == SNAP)
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == CAMERAPOWER)
        digitalWrite(RELAY2, msg[1] ? HIGH : LOW);
      else if (msg[0] == PANUPDOWN){
        servo1.write((msg[1]+60));
      }
      else if (msg[0] == PANLEFTRIGHT){
        servo2.write((msg[1]));
      }
      
      // ON TOUCH COMMANDS


      else if (msg[0] == PAN_UP_START){
        panUD=1;        
      }
      else if (msg[0] == PAN_DOWN_START){
        panUD=-1;        
      }
       else if ((msg[0] == PAN_UP_STOP)||  (msg[0] == PAN_DOWN_STOP)){
        panUD=0;        
      }
      
      
      
      else if (msg[0] == PAN_RIGHT_START){
        panLR=1;        
      }
      else if (msg[0] == PAN_LEFT_START){
        panLR=-1;        
      }
       else if ((msg[0] == PAN_RIGHT_STOP)||  (msg[0] == PAN_LEFT_STOP)){
        panLR=0;        
      }

    }

    // Run every 5 times
    if (loopCounter==5) {
      loopCounter = 0;  
      if(panUD == 1)
      {
        degreeUD += CONSTANT_DEGREE_JUMP;

        if (degreeUD >= MAX_RANGE_UD)
           degreeUD = MAX_RANGE_UD;
                
        servo1.write(degreeUD+60);
      }
      else if (panUD == -1)
      {
        degreeUD -= CONSTANT_DEGREE_JUMP;

        if (degreeUD <= MIN_RANGE_UD)
           degreeUD = MIN_RANGE_UD;
                
        servo1.write(degreeUD+60);
      }

      if(panLR == 1)
      {
        degreeLR += CONSTANT_DEGREE_JUMP;

        if (degreeLR >= MAX_RANGE_LR)
           degreeLR = MAX_RANGE_LR;
                
        servo2.write(degreeLR);
      }
      else if (panLR == -1)
      {
        degreeLR -= CONSTANT_DEGREE_JUMP;

        if (degreeLR <= MIN_RANGE_LR)
           degreeLR = MIN_RANGE_LR;
                
        servo2.write(degreeLR);
      }
    }
    else {
      loopCounter += 1;
    }
  
      msg[0] = 0x1;

    b = digitalRead(IN_CHANNEL0);
    if (b != b1) {
      msg[1] = 0;
      msg[2] = b ? 0 : 1;
      acc.write(msg, 3);
      b1 = b;
    }

    b = digitalRead(IN_CHANNEL1);
    if (b != b2) {
      msg[1] = 1;
      msg[2] = b ? 0 : 1;
      acc.write(msg, 3);
      b2 = b;
    }

    b = digitalRead(IN_CHANNEL2);
    if (b != b3) {
      msg[1] = 2;
      msg[2] = b ? 0 : 1;
      acc.write(msg, 3);
      b3 = b;
    }
  } 

  delay(10);
  
    //  servo1.write(90);
    //servo2.write(0);
    //delay(1000);
    //servo1.write(90);
    //servo2.write(10);
    //delay(1000);
}




