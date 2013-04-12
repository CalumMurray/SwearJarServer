#!/bin/bash
#===================================================================
# Installs software required and configures server
#===================================================================
cd ~

#Install stuff
sudo apt-get update
sudo apt-get install tomcat7 git maven sox libsox-fmt-ffmpeg openjdk-7-jdk 

#TODO Configure tomcat server conf @ /var/lib/tomcat7/conf/server.xml

#Clone server repo
git clone git://github.com/CalumMurray/SwearJarServer.git
cd SwearJarServer

#Configue deploy
sudo ln -s ~/SwearJarServer/deploy /sbin/deploy
chmod +x deploy

#Link to sox splitter
sudo ln -s ~/SwearJarServer/sox_splitter.sh /sbin/sox_splitter

#Deploy server
./deploy