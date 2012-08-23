#include <SoftwareSerial.h>

//#include <NewSoftSerial.h>

// settings
#define RESET_ENABLED      1
#define RESET_TIME         1000
#define RFID_REMOVED_TIME  3000
#define RFID_TAG_LENGTH    5 // 5 Bytes
#define RFID_TAG_INPUT     12 // DATA (10 ASCII) + CHECK SUM (2 ASCII)

// digital pins
#define RESET_PIN         18
#define RFID_RX           19
#define RFID_TX           20 // not used
//#define RESET_LED_PIN     13
//Define pins for switches
#define SW_UP             A6 // can only be used as analog input
#define SW_DOWN           A7 // can only be used as analog input

// values to handle buttons
int SW_DOWN_VAL = 0;
int SW_DOWN_MAP = 0;
int SW_UP_VAL = 0;
int SW_UP_MAP = 0;

// LED's inner ring red
int LEDsInnerRed[] = {10, 11, 12, 13};
// LED's outer ring red
int LEDsOuterRed[] = {14, 15, 16, 17};
// LED's inner ring green
int LEDsInnerGreen[] = {9, 8, 7, 6};
// LED's outer ring green
int LEDsOuterGreen[] = {5, 4, 3, 2};
//Define pins for switches

int ledRingState = 0;

// software serial connection for rfid reader
SoftwareSerial rfidSerial =  SoftwareSerial(RFID_RX, RFID_TX);
// millis of last Reset 
unsigned int nowReset = 0;
// millis of last seen rfid tag
unsigned int nowLastRfid = 0;
// reader is in reset state
boolean rfidEnabled = false;
//
boolean rfidTagSeen = false;
boolean rfidPresent = false;
// last seen tag
byte rfidTagCurrent[RFID_TAG_LENGTH];
byte tagEmpty[RFID_TAG_LENGTH] = {0,0,0,0,0};
// temp tag
byte rfidTagTemp[6];
// 1st device or 2nd device
boolean firstDevice = false;
boolean secondDevice = false;
boolean deviceIDConfirmed = false;
boolean secondDeviceConfirmed = false;

// values for PC comm
boolean connectionPossible = false;
boolean isConnected = false;
//
boolean reasoning = false;

// counter values for LED sequence and LED blinking 
long previousMillis = 0;
long previousMillis1 = 0;
long interval = 1500;
long interval1 = 500;

void setup() 
{
  // connect to the serial port
  Serial.begin(115200); 
  // initialise software serial for rfid reader
  rfidSerial.begin(9600);

  // clear current tag
  clearTag(rfidTagCurrent, RFID_TAG_LENGTH);
  // initialise first device
  firstDevice = true;

  // RFID reset pins
  pinMode(RESET_PIN, OUTPUT);
  //pinMode(RESET_LED_PIN, OUTPUT);
  pinMode(SW_DOWN, INPUT);
  pinMode(SW_UP, INPUT);

  // define all LED pins as output
  for(int i = 0; i < 4; i++){pinMode(LEDsInnerRed[i], OUTPUT);}
  for(int i = 0; i < 4; i++){pinMode(LEDsOuterRed[i], OUTPUT);}
  for(int i = 0; i < 4; i++){pinMode(LEDsInnerGreen[i], OUTPUT);}
  for(int i = 0; i < 4; i++){pinMode(LEDsOuterGreen[i], OUTPUT);}
}


void loop () 
{
  int i = 0;
  // run rfid related code
  rfidSequence();
 
  // while there is a rfid tag present, do the LED sequence
  while (rfidPresent && i < 4 && !secondDeviceConfirmed)
  {      
    rfidSequence();
    // we don't want a delay so we use millis
    unsigned long currentMillis = millis();
    if(currentMillis - previousMillis > interval) 
    {
      previousMillis = currentMillis; 
      if (firstDevice)
      {
        digitalWrite(LEDsInnerGreen[i], HIGH);
        digitalWrite(LEDsInnerRed[i], HIGH);
      }
      else if (secondDevice)
      {
        digitalWrite(LEDsOuterGreen[i], HIGH);
        digitalWrite(LEDsOuterRed[i], HIGH);
      }
      i++;    
    }
  }
  
  if (rfidPresent && firstDevice && !secondDeviceConfirmed) 
  {
    deviceIDConfirmed = true;
    firstDevice = false;
    Serial.write("C");
    Serial.write("1");
  }
  else if (rfidPresent && secondDevice && !secondDeviceConfirmed)
  {
    deviceIDConfirmed = true;
    secondDeviceConfirmed = true;
    firstDevice = true;        // reset firstDevice
    secondDevice = false;      // reset secondDevice
    Serial.write("C");
    Serial.write("2");
    
    // wait while reasoning
    reasoning = true;
    SW_DOWN_MAP = analogRead(SW_DOWN);
    SW_DOWN_VAL = map(SW_DOWN_MAP, 0, 1023, 0, 1);
    SW_UP_MAP = analogRead(SW_UP);
    SW_UP_VAL = map(SW_UP_MAP, 0, 1023, 0, 1);
    
    while (reasoning)
    {
      serialComm();
      SW_DOWN_MAP = analogRead(SW_DOWN);
      SW_DOWN_VAL = map(SW_DOWN_MAP, 0, 1023, 0, 1);
      SW_UP_MAP = analogRead(SW_UP);
      SW_UP_VAL = map(SW_UP_MAP, 0, 1023, 0, 1);
      if ( SW_DOWN_VAL == 1 || SW_UP_VAL == 1)
      {
        reasoning = false;
        reset();
      }     
      delay(100);
    }
  }
  else
  {
    //turn off LEDs
    if (ledRingState == 1) {} //except when LEDs are blinking
    else 
    {
      LEDsInner(0);
      LEDsOuter(0);
    }
  }
  
  // check status and controll LEDs and connections
  
  // device is confirmed, turn LEDs orange 
  if (deviceIDConfirmed && !firstDevice) LEDsInner(3);  
  
  // device is confirmed, two devices were selected and a connection is possible
  else if (deviceIDConfirmed && !secondDevice && connectionPossible && !isConnected)
  {
    unsigned long currentMillis = millis();
    if(currentMillis - previousMillis1 > interval1) 
    {
      previousMillis1 = currentMillis;
      //blink LED rings green
      if (ledRingState == 0)
        ledRingState = 1;
      else
        ledRingState = 0;

      LEDsInner(ledRingState);
      LEDsOuter(ledRingState);
    }
  }
  // device is confirmed, two devices were selected, no connection and no connection is possible
  else if (deviceIDConfirmed && !secondDevice && !connectionPossible && !isConnected) //added !isConnected 
  {
    // turn TEDs red
    LEDsInner(2);
    LEDsOuter(2);
    
    // keep LEDs red until rfid tag is removed from field or a button is pressed
    do  // add 2nd button
    {
      rfidSequence();
      // convert analog value to digital states
      SW_DOWN_MAP = analogRead(SW_DOWN);
      SW_DOWN_VAL = map(SW_DOWN_MAP, 0, 1023, 0, 1);
      // add 2nd button 
      SW_UP_MAP = analogRead(SW_UP);
      SW_UP_VAL = map(SW_UP_MAP, 0, 1023, 0, 1);
      delay(500);
    } while (rfidPresent && SW_DOWN_VAL == 0 && SW_UP_VAL == 0);
    //reset
    reset();
  }
  // dvice is confirmed, two devices were selected and there is an existing connection
  else if (deviceIDConfirmed && !secondDevice && isConnected) 
  {
    LEDsInner(1);
    LEDsOuter(1);
  }
  else ledRingState = 0; // reset ledRingState
  
  // read button
  SW_DOWN_MAP = analogRead(SW_DOWN);
  SW_DOWN_VAL = map(SW_DOWN_MAP, 0, 1023, 0, 1);
  // add 2nd button
  SW_UP_MAP = analogRead(SW_UP);
  SW_UP_VAL = map(SW_UP_MAP, 0, 1023, 0, 1);
  if (SW_DOWN_VAL == 1 && secondDeviceConfirmed && connectionPossible && !isConnected) //connect
  {
    Serial.write("S");
    LEDsInner(1);
    LEDsOuter(1);
    do
    {
      delay(500);
      rfidSequence();
    } while (rfidPresent);
    
    // reset    
    reset();
  }
  else if (SW_UP_VAL == 1 && secondDeviceConfirmed && connectionPossible && !isConnected) //cancel
  {
    Serial.write("R");
    LEDsInner(0);
    LEDsOuter(0);
    do
    {
      delay(500);
      rfidSequence();
    } while (rfidPresent);
    
    // reset    
    reset();
  }
  else if (SW_UP_VAL == 1 && secondDeviceConfirmed && isConnected) // disconnect
  {
    Serial.write("D");
    LEDsInner(2);
    LEDsOuter(2);
    do
    {
      delay(500);
      rfidSequence();
    } while (rfidPresent);
    // reset    
    reset();
  }
  else if (SW_DOWN_VAL == 1 && secondDeviceConfirmed && isConnected) // cancel
  {
    Serial.write("R");
    LEDsInner(0);
    LEDsOuter(0);
    do
    {
      delay(500);
      rfidSequence();
    } while (rfidPresent);
    // reset    
    reset();
  }
  else if ((SW_DOWN_VAL == 1 || SW_UP_VAL == 1) && !isConnected && !connectionPossible) // reset
  {
    Serial.write("R"); // send (R)eset to serial
    reset();
  }
  
  
  /**
   * TO DO:
   * communication with PC
   * make sure both LED rings are activated in turn 
   * correctly respond to button clicks
   * make sure there is robustness when rfid tags are leaving/entering the field
   * ...
   */
  
  // serial connection with computer
  serialComm();
      
  // delay 100 milliseconds
  delay(100);
}

/**
 * reset
 */
void reset()
{
  deviceIDConfirmed = false;
  firstDevice = true;
  secondDevice = false;   
  secondDeviceConfirmed = false;
  LEDsInner(0);
  LEDsOuter(0); 
  // added 4/9 to make reset work in all cases
  isConnected = false;
  connectionPossible = false;
  
}

/**
 * serial communication with computer
 */
void serialComm()
{
  while (Serial.available() > 0) 
  {
    char serial = Serial.read();
    
    switch (serial) 
    {   
      // Connection possible
      case 'P':
        // clear serial
        connectionPossible = true;
        isConnected = false;
        //digitalWrite(RESET_LED_PIN, HIGH);
        reasoning = false;
        break;
  
      // Connection not possible
      case 'N':
        connectionPossible = false;
        isConnected = false;
        //digitalWrite(RESET_LED_PIN, LOW);
        reasoning = false;
        break;
      
      // Connected
      case 'C':
        isConnected = true;
        reasoning = false;
        break;
      
      // not connected
      case 'X':
        LEDsInner(0);   
        for(int i = 0; i < 3; i++)
        {          
          LEDsOuter(2);
          delay(100);
          LEDsOuter(0);
          delay(100);
        }
        while (rfidPresent) {rfidSequence(); delay(100);};
        reset();        
        reasoning = false;
        break;
    }
  } 
}

/**
 * RFID reader sequence to read tags and get notified when tags get removed
 * Code based on: http://blog.formatlos.de/2008/12/08/arduino-id-12/ by Martin Rädlinger
 */
void rfidSequence()
{
  byte action = 0;
  unsigned int now = millis();
  
  updateID12(false);
  clearTag(rfidTagTemp, 6);
  
  // serial connection with rfid reader
  if (rfidSerial.available()) 
  {
    // wait for the next STX byte
    while(rfidSerial.available() && action != 0x02)
      action = rfidSerial.read();
    
    // STX byte found -> RFID tag available
    if(action == 0x02)
    {
      if(readID12(rfidTagTemp))
      {
        nowLastRfid = millis();
        rfidTagSeen = true;
        updateCurrentRfidTag(rfidTagTemp); // writes to serial line
      }
    }    
  }
  else if(rfidEnabled && rfidTagSeen == true && (now - nowLastRfid) >= RFID_REMOVED_TIME)
  {    
    rfidTagSeen = false;
    updateCurrentRfidTag(rfidTagTemp); // writes to serial line
  }
}

/**
 * print actual tag number to serial
 */
void updateCurrentRfidTag(byte *tagNew)
{  
  // only print changed value     
  if(!equals(tagNew, rfidTagCurrent))
  {
    saveTag(tagNew, rfidTagCurrent);
    
    // if the new tag is not an empty tag  
    if (!equals(tagNew, tagEmpty))
    { 
      // and a first device has been selected
      if (!firstDevice)
      {
        secondDevice = true;
      }
      // an rfid tag is present
      rfidPresent = true;
    }
    else
    { 
      rfidPresent = false;
      //flash LED ring
      //LEDsInner(3);
      //delay(500);
      //LEDsInner(0);
    }
    
    byte i = 0;
    
    // STX
    Serial.write(0x02);
    
    for (i=0; i<5; i++) 
    {
      if (rfidTagCurrent[i] < 16) Serial.print("0");
      Serial.print(rfidTagCurrent[i], HEX);
    }  
    // ETX
    Serial.write(0x03);
  }
}


/**
 * read data from rfid reader
 * @return rfid tag number
 *
 * Based on code by BARRAGAN, HC Gilje, djmatic, Martijn
 * http://www.arduino.cc/playground/Code/ID12 
 */
boolean readID12(byte *code)
{
  boolean result = false;
  byte val = 0;
  byte bytesIn = 0;
  byte tempbyte = 0;
  byte checksum = 0;
  
  // read 10 digit code + 2 digit checksum
  while (bytesIn < RFID_TAG_INPUT) 
  {                        
    if( rfidSerial.available() > 0) 
    { 
      val = rfidSerial.read();

      // if CR, LF, ETX or STX before the 10 digit reading -> stop reading
      if((val == 0x0D)||(val == 0x0A)||(val == 0x03)||(val == 0x02)) break;
      
      // Do Ascii/Hex conversion:
      if ((val >= '0') && (val <= '9')) 
        val = val - '0';
      else if ((val >= 'A') && (val <= 'F'))
        val = 10 + val - 'A';


      // Every two hex-digits, add byte to code:
      if (bytesIn & 1 == 1) 
      {
        // make some space for this hex-digit by
        // shifting the previous hex-digit with 4 bits to the left:
        code[bytesIn >> 1] = (val | (tempbyte << 4));
        
        // If we're at the checksum byte, Calculate the checksum... (XOR)
        if (bytesIn >> 1 != RFID_TAG_LENGTH) checksum ^= code[bytesIn >> 1]; 
      } 
      else 
      {
        // Store the first hex digit first...
        tempbyte = val;                           
      }

      // ready to read next digit
      bytesIn++;                                
    } 
  }

  // read complete
  if (bytesIn == RFID_TAG_INPUT) 
  { 
    // valid tag
    if(code[5] == checksum) result = true; 
  }

  // reset id-12
  updateID12(true);

  return result;
}


/**
 * update reset state of the rfid reader
 */
void updateID12(boolean reset_)
{
  // reset is disabled
  if(RESET_ENABLED == 0 && rfidEnabled == true) return;

  // don't reset, just check if the id-12 should be enabled again 
  if(reset_ == false)
  {
    // current time
    unsigned int now = millis();

    // id-12 is disabled and ( reset period is over or initial id-12 startup )
    if (rfidEnabled == false && ((now - nowReset) >= RESET_TIME || nowReset == 0)) 
    { 
      //digitalWrite(RESET_LED_PIN, LOW);
      digitalWrite(RESET_PIN, HIGH);
      rfidEnabled = true;
    }
  }
  // reset rfid reader
  else
  {
    //digitalWrite(RESET_LED_PIN, HIGH);
    digitalWrite(RESET_PIN, LOW);

    nowReset = millis();
    rfidEnabled = false;  
  }
}


/**
 * clear rfid tags
 */
void clearTag(byte *arr, byte len)
{
  byte i;
  for (i=0; i < len ;i++) arr[i] = 0;
}


/**
 * save rfid tag
 */
void saveTag(byte *tagIn, byte *tagOut)
{
  byte i;
  for (i=0; i < RFID_TAG_LENGTH ;i++) tagOut[i] = tagIn[i];
}


/**
 * compare 2 rfid tags
 */
boolean equals(byte *tag1, byte *tag2)
{
  boolean result = false;
  byte j;
  
  for (j=0; j < RFID_TAG_LENGTH ;j++) 
  {
    if(tag1[j] != tag2[j]) break;
    else if (j == RFID_TAG_LENGTH-1) result = true;
  }    
  return result;
}

/**
 * control inner LEDring
 */
void LEDsInner(int LEDsState)
{
  switch (LEDsState) 
  {
    case 0:
      // turn LEDs off
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsInnerGreen[i], LOW);
        digitalWrite(LEDsInnerRed[i], LOW);
      }
      break;
    case 1:
      // set LEDs to green    
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsInnerGreen[i], HIGH);
        digitalWrite(LEDsInnerRed[i], LOW);
      }
      break;
    case 2:
      // set LEDs to red
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsInnerGreen[i], LOW);
        digitalWrite(LEDsInnerRed[i], HIGH);
      }
      break;
    case 3:
      // set LEDs to orange
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsInnerGreen[i], HIGH);
        digitalWrite(LEDsInnerRed[i], HIGH);
      }
      break;
  }     
}

/**
 * control outer LEDring
 */
void LEDsOuter(int LEDsState)
{
  switch (LEDsState) 
  {
    case 0:
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsOuterGreen[i], LOW);
        digitalWrite(LEDsOuterRed[i], LOW);
      }
      break;
    case 1:     
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsOuterGreen[i], HIGH);
        digitalWrite(LEDsOuterRed[i], LOW);
      }
      break;
    case 2:
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsOuterGreen[i], LOW);
        digitalWrite(LEDsOuterRed[i], HIGH);
      }
      break;
    case 3:
      for(int i = 0; i < 4; i++)
      {
        digitalWrite(LEDsOuterGreen[i], HIGH);
        digitalWrite(LEDsOuterRed[i], HIGH);
      }
      break;
  }     
}
