# Drive Safe
Drive Safe is an app that makes driving safe interactive and fun.

# Using Sensors
The app uses the phone's accelerometer in order to calculate the vehicle's acceleration. The program accounts for the force of gravity and phone orientation. The program detects which way the driver is driving, and finds acceleration in m/s^2.

# Using Latitude and Longitude Positioning
By recieving latitude and longitude data, the program is able to find the speed at which the driver is moving at in m/s. The program is also able to find the speed limit relative to what road the driver is on. In turn, the program is able to accurately understand when the user is driving above the speed limit.

# Calculating the Driver's Score
As a team, we decided that Speed should account for 75% of the Driver's score, and Acceleration should account 25% of their score. We decided that following the speed limit should be a much higher priority than smoothly breaking the vehicle. The score is created based on the percentage of offenses that the driver has faced relative to the amount of time that they have been driving. This creates a fun interactive environment where, good driving is rewarded, and low scores can slowly be improved upon. We have designed the scoring system to be healthy and encouraging. Our hope is that drivers find enjoyment from self-improvement and safe driving.
 
   # Calculating the user's score based off speed
   As we decided, following the speed limit is a very high priority. Following the DMV handbook guidlines, the driver begins to lose        points when they go 5mph above the speed limit. The consquences have a linear increase the faster the driver is moving, encouraging      them to slow down in order to obtain a better score.
   
   # Calculating the user's score based off acceleration
   Although acceleration is less important than speed, it is still extremely important for the Driver to smoothly break and accelerate.
   Using the phone's accelerometer, the program grades the driver's accerleration states. Based off of DMV recommendations, our team has 
   developed test harnesses that remove points for unsafe breaking and acceleration.
  
 # Using FireStore storage
 The user's score data is uploaded to the firStore system, and keeps the statistical data of each and every user's score.
