  
  char val; // Data received from the serial port
  //int led;
  int red, green, blue, dim, duration;
  int redLed = 11, greenLed = 10, blueLed = 9;
  int newColor[3];
  int oldColor[3] = {255,255,255};
  
  void setup() {
     Serial.begin(115200); // Start serial communication at 115200 bps      
  }
  
  void loop() {
   if (Serial.available()) {             // If data is available to read,
     val = Serial.read();                // read it and store it in val
  
     if (val == 'S') {                   //If start char is recieved,
       while (!Serial.available()) {}    //Wait until next value.
       red = Serial.read();              //Once available, assign.
  
       while (!Serial.available()) {}
       green = Serial.read();
  
       while (!Serial.available()) {}
       blue = Serial.read();
  
       newColor[0] = red;
       newColor[1] = green;
       newColor[2] = blue;
       
       fadeToColor(oldColor, newColor);
       for (int i=0; i<3; i++) {oldColor[i] = newColor[i];}
     }
     
     if (val == 'V') {                   //If start char is recieved,
       while (!Serial.available()) {}    //Wait until next value.
       dim = Serial.read();              //Once available, assign.
  
  
       newColor[0] = dim;
       newColor[1] = dim;
       newColor[2] = dim;
       
       fadeToColor(oldColor, newColor);
       for (int i=0; i<3; i++) {oldColor[i] = newColor[i];}
     }
     
     if (val == 'D') {                   //If start char is recieved,
       while (!Serial.available()) {}    //Wait until next value.
       duration = Serial.read();              //Once available, assign.
  
       while (!Serial.available()) {}    //Wait until next value.
       dim = Serial.read();  
  
       previewEvent(duration, dim);  
     }
    }
  }
  
  void fadeToColor(int startColor[3], int endColor[3]) {
    int changeRed = endColor[0] - startColor[0];                            
    int changeGreen = endColor[1] - startColor[1];                          
    int changeBlue = endColor[2] - startColor[2];
    int steps = max(abs(changeRed),max(abs(changeGreen), abs(changeBlue)));

    for(int i = 0 ; i < steps; i++) {
      int newRed = startColor[0] + (i * changeRed / steps);
      int newGreen = startColor[1] + (i * changeGreen / steps);
      int newBlue = startColor[2] + (i * changeBlue / steps);
      int targetColor[] = {newRed, newGreen, newBlue};
      setRGB(targetColor);
      delay(1);  
    }
    setRGB(endColor);                 //The LED should be at the endColor but set to endColor to avoid rounding errors
  }
  
  void previewEvent(int duration, int value){
    float s = (float)duration / (float)value;
    int sleep = s * 1000;
    int targetColor[3];
		
    for(int i=0; i<value ; i++ ){
      targetColor[0] = i;
      targetColor[1] = i;
      targetColor[2] = i;
      setRGB(targetColor);
      delay(sleep);
    }
    targetColor[0] = 0;
    targetColor[1] = 0;
    targetColor[2] = 0;
    setRGB(targetColor);
  }
   
  void setRGB(int color[3]) {
    analogWrite(redLed, color[0]);
    analogWrite(greenLed, color[1]);
    analogWrite(blueLed, color[2]);  
  } 

