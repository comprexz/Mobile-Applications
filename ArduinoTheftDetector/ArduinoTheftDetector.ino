/*********************************************************************
  * Laura Arjona. UW EE P 523. SPRING 2020
  * Example of simple interaction beteween Adafruit Circuit Playground
  * and Android App. Communication with BLE - uart
*********************************************************************/
#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"
#include <Adafruit_CircuitPlayground.h>

#include "BluefruitConfig.h"

#if SOFTWARE_SERIAL_AVAILABLE
  #include <SoftwareSerial.h>
#endif

/*=========================================================================
    APPLICATION SETTINGS
    -----------------------------------------------------------------------*/
    #define FACTORYRESET_ENABLE         0
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
/*=========================================================================*/

// Create the bluefruit object, either software serial...uncomment these lines

Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);


// A small helper to show errors on the serial monitor
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}


uint32_t counter;

void setup(void)
{
  CircuitPlayground.begin();
  

  Serial.begin(115200);

  /* Initialise the module */
  Serial.print(F("Initialising the Bluefruit LE module: "));

  if ( !ble.begin(VERBOSE_MODE) )
  {
    error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
  }
  Serial.println( F("OK!") );

  if ( FACTORYRESET_ENABLE )
  {
    /* Perform a factory reset to make sure everything is in a known state */
    Serial.println(F("Performing a factory reset: "));
    if ( ! ble.factoryReset() ){
      error(F("Couldn't factory reset"));
    }
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);

  Serial.println("Requesting Bluefruit info:");
  /* Print Bluefruit information */
  ble.info();

  Serial.println(F("Please use Adafruit Bluefruit LE app to connect in UART mode"));
  Serial.println(F("Then Enter characters to send to Bluefruit"));
  Serial.println();

  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while (! ble.isConnected()) {
      delay(500);
  }
  Serial.println("CONECTED:");

  // Green pixels
  green();
  Serial.println(F("******************************"));

  // LED Activity command is only supported from 0.6.6
  if ( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
  {
    // Change Mode LED Activity
    Serial.println(F("Change LED activity to " MODE_LED_BEHAVIOUR));
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
  }

  // Set module to DATA mode
  Serial.println( F("Switching to DATA mode!") );
  ble.setMode(BLUEFRUIT_MODE_DATA);

  Serial.println(F("******************************"));
 
  CircuitPlayground.setAccelRange(LIS3DH_RANGE_2_G);
 
  delay(1000);
  counter = 0;
  cli();
  TCCR1A = 0;// set entire TCCR1A register to 0
  TCCR1B = 0;// same for TCCR1B
  TCNT1  = 0;//initialize counter value to 0
  // set compare match register for 1hz increments
  OCR1A = 15624;// = (16*10^6) / (1*1024) - 1 (must be <65536)
  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS10 and CS12 bits for 1024 prescaler
  TCCR1B |= (1 << CS12) | (1 << CS10);  
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);
}

ISR(TIMER1_COMPA_vect)
{
  counter++;
}

void green(void)
{
  for ( int i = 0; i < 10; i++ )
  {
    CircuitPlayground.setPixelColor(i, 0, 255, 0);
  }
}

void yellow(void)
{
  for ( int i = 0; i < 10; i++ )
  {
    CircuitPlayground.setPixelColor(i, 255, 255, 0);
  }
}

void red(void)
{
  for ( int i = 0; i < 10; i++ )
  {
    CircuitPlayground.setPixelColor(i, 255, 0, 0);
  }
}

bool doorOpened() 
{
  float x = CircuitPlayground.motionX();
  float y = CircuitPlayground.motionY();
  float z = CircuitPlayground.motionZ();

  return z*z > 40;
}

bool isCanceled = false;
String cancelString = "cancel\n";
bool cancel(void)
{
  String received = "";
  while ( ble.available() )
  {
    int c = ble.read();
    Serial.print((char)c);
    received += (char)c;
        delay(50);
  }

  if ( received == cancelString )
  {
    isCanceled = true;
    Serial.println("Canceled!");
    return true;
  }
  return false;
}

bool buttonCancel (void)
{
  if ( CircuitPlayground.rightButton() )
  {
    return CircuitPlayground.leftButton();
  }
  return false;
}

void loop(void)
{
  if ( doorOpened() )
  {
    counter = 0;
    char buf[3];
    String doorOpened = "DO\n";
    doorOpened.toCharArray( buf, 3);
    ble.print( buf );
    delay(200);
    sei();
    while( !cancel() && counter < 15 )
    {
      yellow();
      delay(50);
      CircuitPlayground.clearPixels();
      delay(50);

      if ( buttonCancel() )
      {
        String buttonCancel = "BC\n";
        buttonCancel.toCharArray( buf, 3 );
        ble.print( buf );
        delay(200);
        isCanceled = true;
        break;
      }
    }

    if ( !isCanceled )
    {
      char buf[3];
      String doorOpened = "TD\n";
      doorOpened.toCharArray( buf, 3 );
      ble.print( buf );
      delay(200);

      while( 1 )
      {
        CircuitPlayground.playTone(1000, 500, false);
        red();
        delay(50);
        CircuitPlayground.clearPixels();
        delay(50);
      }
    } else
    {
      cli();
      counter = 0;
      isCanceled = false;
      green();
      Serial.println("Reset the timer");
    }
  }
  
}

 
