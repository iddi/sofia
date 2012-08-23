 
 // Interaction tile
 // Bram van der Vlist
  
 // LED's PWM
 int led[] = {10, 11, 9, 6};
 // LED's digital
 int ledR[] = {12, 13, 8, 7};
 // PWM speaker
 int spk = 5;
 // PWM vibration motor
 int vibr = 3;
 // Reedswitches
 int reed[] = {14, 15, 16, 17};
 // Array storing reedswitch states
 int reedState[] = {0, 0, 0, 0};
 
 // Accelerometer
 int x = 4, y = 5;
 int xVal, yVal;
 int maxX = 255, maxY = 255, minX = 255, minY = 255; 
 
 boolean toggle = false;
 boolean vibrOn = false;
 boolean end = false;
 
 int fadeValue = 0; 
 int reverse = 0;
 
 // defining connection or connection possibillities
 // "n" = not possible, "c" connected, "p" connection possible
 char connections[] = {'n','n','n','n'};
 char start;
 
 unsigned long time;
 unsigned long prevTime;
 
 unsigned long timeEnter;
 unsigned long prevTimeEnter;
 
 int interval = 200;
 int count = 0;
 int count1 = 0;

 void setup() {
   Serial.begin(115200); // Start serial communication at 115200 bps
   for(int i = 0; i < 4; i++){pinMode(ledR[i], OUTPUT);} // configure digital ledports as output
 }
 
 // Check reed switches and control corresponding LED
 void checkReed() {
   for (int i = 0; i < 4; i++) {reedState[i] = digitalRead(reed[i]);}
   /*
    * this should probably be changed as soon as we found a solution for updating the LED's
    * for now I only commented the things written over the serial line
    */
   for (int i = 0; i < 4; i++) {
//     if (reedState[i] == HIGH) {
//       Serial.write(i);          // write the number of the reedswitch that is closed to the serial line
//       // wait for a reply from the SIB with instructions for the LED's
//       serialInstr(); 
//   } else {Serial.write(-1);}     
     if (reedState[i] == HIGH && connections[i] == 'p') {       // if connection is possible pulse the corresponding LED
       digitalWrite(ledR[i], LOW);
       ledPulse(i);
       toggle = false;
     } 
     else if (reedState[i] == HIGH && connections[i] == 'c') {  // if there is a connection turn LED to green
       digitalWrite(ledR[i], LOW);
       analogWrite(led[i], 255);
       toggle = true;
     }
     else if (reedState[i] == HIGH && connections[i] == 'n') {  // if there's no connection possible and no existing turn LED red
       analogWrite(led[i], 0);
       digitalWrite(ledR[i], HIGH);
       toggle = false;
     } 
     else {                                                 // if nothing happens turn LED's off
       analogWrite(led[i], 0);
       digitalWrite(ledR[i], LOW);
       toggle = false; 
     }
   } 
   //Serial.write('S');   // write a end marker to serial line 
 }
 
 // pulse the LED
 void ledPulse(int i) {
   if (fadeValue == 255) {reverse = 1;}
   if (fadeValue == 0) {reverse = 0;}
   
   if (reverse == 0) {
     analogWrite(led[i], fadeValue);
     fadeValue += 5;
   }   
   
   if (reverse == 1) {
     analogWrite(led[i], fadeValue);
     fadeValue -= 5;
   }
 }
 
 // reset min and max values
 void reset() {
   maxX = 255; 
   minX = 255;
   maxY = 255;
   minY = 255;
   end = true;    // end the interaction event loop on card exit
 }
 
 // check if the max values meet the thresholds
 // if so, turn on/off light and reset minima en maxima ;-)
 void checkAccel() {
   xVal = analogRead(x);
   yVal = analogRead(y);
   
   if (xVal >= maxX) {maxX = xVal;} 
   if (yVal >= maxY) {maxY = yVal;}
   if (xVal <= minX) {minX = xVal;}
   if (yVal <= minY) {minY = yVal;}
   
   int maxThresh = 896;
   int minThresh = 128; 
   
   if ( (maxX >= maxThresh) && (minX <= minThresh) && (toggle == true) ) {
     analogWrite(vibr, 127); delay(500); analogWrite(vibr, 0);
     // Disconnected
     toggle = false;
     reset();
   }  
   if ( (maxX >= maxThresh) && (minX <= minThresh) ) {
     analogWrite(spk, 127); delay(100); analogWrite(spk, 0);
     // Connected
     toggle = true;
     reset(); 
   }    
 }
 
 // read connection events from the serial line
 void serialInstr() 
 {
   char connection = 'n';
   int pos = -1;
   
   if (Serial.available()) {             // If data is available to read,
     connection = Serial.read();         // read it and store it in connection
     pos = Serial.read();
     connections[pos] = connection;  
   }
 } 
 
 // if more then one card exits at the same time, listen to the accelerometer to detect an interaction event
 // and send the result of the interaction event over the serial line.
 // if only one card exits send the position of the exiting card.
 void cardEvent() 
 {
   boolean interactionEvent = false;  // do we have an interaction event?
   
   int exitPos = -1;
   int prevExit;
   int reedEvent[4];
   
   for (int i = 0; i < 4; i++) {reedEvent[i] = digitalRead(reed[i]);} //store new reedvalues in an array
   for (int i = 0; i < 4; i++) {
     if (reedEvent[i] != reedState[i]) {                       // compare arrays     
       if (reedEvent[i] == LOW) {                              // if card exits
         count ++;                                             // add 1 to counter
         time = millis();                                      // store current time
         if ((count > 1) && ((time - prevTime) < interval)) {  // if more then one cards left and time difference between                                                   
           // listen to interaction event                      // two subsequent card exits is smaler then the predefined interval
           for (int i = 0; i < 4; i++) {digitalWrite(ledR[i], HIGH);}
           while(!end) { // wait for an interaction to finish           
             checkAccel();
             delay(10);
           }
           end = false;
           interactionEvent = true;
           count = 0;            // reset counter           
           exitPos = -1;
         } else if ((time - prevTime) < interval) {
           time = prevTime;      // equalize time and prevTime
         } else {
           exitPos = i;
         }
         prevTime = time;      // prevTime should store the "current" time
       }
       
       if (reedEvent[i] == HIGH) { // if a card enters the field, write the position plus 10  
         timeEnter = millis();
         count1 ++;
         if ((count1 > 1) && ((timeEnter - prevTimeEnter) < interval)) {
           count1 = 0;
           //timeEnter = prevTimeEnter;      // prevTime should store the "current" time
         } else {
           prevTimeEnter = timeEnter;      // prevTime should store the "current" time
           Serial.write(i+10);
           Serial.write('E');  // followed by serial event marker
         } 
       }
       
     }
   }
   
   if (exitPos >= 0) {
     Serial.write(exitPos);
     Serial.write('E'); // "E" is the serial event marker
   }
   
   // if we have an interaction event, send the result over the serial line
   if (interactionEvent) {
     // not sure about this
     //Serial.write(prevExit+20);
     //Serial.write('E');
     if (toggle == true) {
       Serial.write('C');  // connected!
       Serial.write('E');  // "E" is the serial event marker
     } else {  
       Serial.write('D');  // disconnected!
       Serial.write('E');  // "E" is the serial event marker
     }
   }
 }
 
 
 // main loop: read LED instructions from the serial line, check if cards exit,
 // check the reedswitches for any changes and update
 void loop() {
   
   serialInstr();
   cardEvent();
   checkReed();
   
   delay(10);
 }
