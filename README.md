# Kafka From Zero to Hero
## Description
This repository would contains all the Kafka playground commands and also the complete capstone e-commerce project which we will be doing while learning Kafka.

## Pre-requisites:
- Java must be installed on your machine.
- 
## Kafka Docker Setup
- There is a confluent Kafka image available but thats huge in size. So, for learning, i decided to create our own custom Kafka image.
- You would find the DockerFile in setup/image folder. I have created the generic image, no hardcoding of file paths from my machine, so it should run fine in your machine as well.
- But still if you want to do/tweak any changes according to your needs, you can just change the file path directory locations and you are good with it.
- In setup/compose folder, i have created the docker-compose.yaml file where i used the same image which i published in my docker hub repository.
- You can just use the same file, but if you want to do some changes, you can publish in your own docker-hub repository and use it.

## Kafka Docker Container Start/Stop
- We are using docker-compose throughout the series, i would suggest to use the same so that it would be easier later when we will spin up multiple kafka brokers to form a cluster.
- But still if you want to run the simple docker container from the image only, you can just run below command:
-  ```docker run suhail50/kafka ```
- If you are using the docker-compose, then simply below command would be required to run
-  ```docker-compose up ```
