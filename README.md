# Quiz-Application -- Microservices-Architecture

A Spring Boot--based Quiz Application that was initially developed as a
monolithic application and later redesigned into a microservices
architecture.

The microservices version separates question management from quiz
management. The services communicate through Spring Cloud OpenFeign,
register with a central Netflix Eureka Service Registry, and are
accessed through a Spring Cloud API Gateway.

The project demonstrates service decomposition, service discovery,
client-side load balancing, synchronous inter-service communication,
REST API design, and independent database configuration.


## Table of Contents
- Project Evolution
- Architecture
- Services
- Technology Stack
- Database Design
- API Gateway Routes
- API Documentation
  - Create a Quiz
  - Get Quiz Questions
  - Submit Quiz Answers
  - Question Service APIs
- Application Flow
- Load Balancing
- Run Locally
- Configuration
- Concepts Demonstrated
- Current Scope and Notes
- Future Enhancements
- Author



## Project Evolution
### 1. Monolithic Version -- QuizApp
The application was first developed as a single Spring Boot application
containing:
- Question Controller and Question Service
- Quiz Controller and Quiz Service
- Question and Quiz repositories (DAOs)
- Question, Quiz, QuestionWrapper, and Response models
In this version, quiz creation, question retrieval, and score
calculation were handled within the same application.

### 2. Microservices Version
The monolithic application was then used as the reference for a new
microservices implementation. Responsibilities were separated into
independent services:
  Component                           Responsibility
  Question Service                    Owns question data and provides question-related APIs

  Quiz Service                        Creates quizzes, stores question IDs, retrieves quiz questions, and submits answers

  Eureka Service Registry             Maintains registered service instances and supports service discovery

API Gateway                         Provides a common entry point and routes client requests to backend services

The original monolithic application was kept separately; the
microservices implementation is a separate version.

Architecture



                         Client / Postman
                                |
                                | HTTP
                                v
                    +------------------------+
                    |       API Gateway      |
                    |       Port: 8765       |
                    +------------------------+
                                |
                     /quiz-service/**
                                |
                                v
                    +------------------------+
                    |      Quiz Service      |
                    |       Port: 8090       |
                    +------------------------+
                                |
                                | Spring Cloud OpenFeign
                                | @FeignClient("QUESTION-SERVICE")
                                v
                 +----------------------------------+
                 |          Question Service       |
                 |                                  |
                 |       Instance 1: 8080           |
                 |       Instance 2: 8081           |
                 +----------------------------------+
                                |
                                v
                       +------------------+
                       |   questiondb     |
                       |   PostgreSQL     |
                       +------------------+


          +-----------------------------------------------+
          |              Eureka Service Registry          |
          |                   Port: 8761                  |
          |                                               |
          |  Registered services:                         |
          |  - API-GATEWAY                                 |
          |  - QUIZ-SERVICE                                |
          |  - QUESTION-SERVICE (two instances)            |
          +-----------------------------------------------+

                    Quiz Service
                         |
                         v
                  +------------------+
                  |     quizdb       |
                  |    PostgreSQL    |
                  +------------------+

### Service discovery: Quiz Service, Question Service instances, and API
Gateway register with Eureka. Quiz Service uses the logical service name
QUESTION-SERVICE through OpenFeign instead of hardcoding a Question
Service host and port.

### Gateway routing: Client requests to /quiz-service/** are routed to
Quiz Service using the lb://quiz-service URI. The StripPrefix=1
filter removes the /quiz-service prefix before forwarding.

### Inter-service communication: Quiz Service invokes Question Service
APIs through its Feign client.

### Services
1. Question Service

Question Service is responsible for managing question data and
performing question-related operations.

Responsibilities
- Retrieve all questions.
- Retrieve questions by category.
- Add questions.
- Select a requested number of random question IDs for a category.
- Retrieve question details for a supplied list of IDs.
- Calculate the score by comparing submitted answers with the correct
  answers.
  
Important endpoints
  Method                  Endpoint                                                            Purpose
  GET                   /question/allQuestions                                            Retrieve all questions

  GET                   /question/category/{category}                                     Retrieve questions by
category

  POST                  /question/add                                                     Add a question

  GET                   /question/generate?categoryName={category}&numQuestions={count}   Generate random question IDs

  POST                  /question/getQuestions                                            Retrieve question details by IDs
POST                  /question/getScore                                                Calculate the score for submitted responses

Question model

The Question entity contains:
- id
- questionTitle
- option1
- option2
- option3
- option4
- rightAnswer
- difficultylevel
- category
  
Question response model

QuestionWrapper is used to return question details and the four
options without exposing the rightAnswer field.

Persistence
- Database: PostgreSQL
- Database name in the supplied configuration: questiondb
- Repository: Spring Data JPA QuestionDao
  
Service registration
spring.application.name=question-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
The service was started as two instances on ports 8080 and 8081
to test load balancing.


### 2. Quiz Service

Quiz Service manages quiz creation and quiz-related operations. It does
not directly own the question records; it communicates with Question
Service to obtain question IDs, question details, and scores.

Responsibilities
- Create a quiz using a category, number of questions, and title.
- Store the generated question IDs with the quiz.
- Retrieve quiz questions using a quiz ID.
- Submit answers and request score calculation from Question Service.
  
Important endpoints
  Method                  Endpoint                Purpose
  POST                  /quiz/create          Create a quiz

  GET                   /quiz/get/{id}        Retrieve questions for a quiz
POST                  /quiz/submit/{id}     Submit answers and calculate the score

Quiz model

The Quiz entity contains:
- id
- title
- questionIds -- a list of question IDs stored using JPA @ElementCollection
  
QuizDto is used as the request model for quiz creation.

Persistence
- Database: PostgreSQL
- Database name in the supplied configuration: quizdb
- Repository: Spring Data JPA QuizDao
  
Service registration
spring.application.name=quiz-service
server.port=8090
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

OpenFeign integration

Quiz Service declares a Feign client:
@FeignClient("QUESTION-SERVICE")
public interface QuizInterface {
    // Question Service API mappings
}

The Feign client calls the Question Service to generate question IDs,
retrieve question details, and calculate scores.

### 3. Eureka Service Registry

The Service Registry provides a central place for service registration
and discovery.

Implementation
- Uses Spring Cloud Netflix Eureka Server.
- Enables the server using @EnableEurekaServer.
- Runs on port 8761.
  
Configuration
spring.application.name=service-registry
server.port=8761

eureka.instance.hostname=localhost
eureka.client.fetch-registry=false
eureka.client.register-with-eureka=false

Dashboard
Open the Eureka dashboard at:
http://localhost:8761

The dashboard can be used to view registered application instances and
their status.

During testing, the dashboard showed:
- API-GATEWAY -- one instance
- QUIZ-SERVICE -- one instance
- QUESTION-SERVICE -- two instances
</details>

### 4. API Gateway

The API Gateway provides a single entry point for client requests and
forwards requests to the appropriate backend service.

Implementation
- Spring Cloud Gateway Server MVC (Servlet-based Gateway)
- Eureka Client for service discovery
- Runs on port 8765
  
Configured Quiz Service route
spring.cloud.gateway.server.webmvc.routes[0].id=quiz-service
spring.cloud.gateway.server.webmvc.routes[0].uri=lb://quiz-service
spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/quiz-service/**
spring.cloud.gateway.server.webmvc.routes[0].filters[0]=StripPrefix=1

How the route works

A client sends:
http://localhost:8765/quiz-service/quiz/get/1

The Gateway:
1. Matches the /quiz-service/** path.
2. Uses lb://quiz-service to resolve a Quiz Service instance through
   service discovery and Spring Cloud LoadBalancer.
3. Removes the first path segment (/quiz-service) using
   StripPrefix=1.
4. Forwards /quiz/get/1 to Quiz Service.
   
The Gateway is configured with an explicit Quiz Service route. The
supplied configuration does not define a separate public Gateway route
for Question Service; Quiz Service reaches Question Service through
OpenFeign.

### Technology Stack
  Area                                Technologies
  Language                            Java 17

  Framework                           Spring Boot 4.1.2

  Microservices                       Spring Cloud

  API development                     Spring Web MVC, REST APIs

  API Gateway                         Spring Cloud Gateway Server MVC

  Service discovery                   Spring Cloud Netflix Eureka Server and Client

  Inter-service communication         Spring Cloud OpenFeign

  Load balancing                      Spring Cloud LoadBalancer (through service discovery integration)

  Persistence                         Spring Data JPA, Hibernate

  Databases                           PostgreSQL

  Boilerplate reduction               Project Lombok

  Build and dependency management     Maven

  API testing                         Postman

  Database administration             pgAdmin 4


IDE                                 IntelliJ IDEA
Spring Cloud release train configured in the Maven projects: 2025.1.4.

Database Design
The microservices use separate PostgreSQL databases in the supplied
configuration.


<img width="757" height="747" alt="image" src="https://github.com/user-attachments/assets/40695f60-970e-4c19-afbc-0d0712a0fac1" />
<img width="755" height="802" alt="image" src="https://github.com/user-attachments/assets/2600263d-6f87-4796-8561-bb9b253b908a" />
<img width="747" height="906" alt="image" src="https://github.com/user-attachments/assets/0d58a910-68b5-4f01-8efb-2546ac379b1a" />
<img width="531" height="232" alt="image" src="https://github.com/user-attachments/assets/d2718cfa-7ff4-44e7-8431-b92bcd0404fd" />



<img width="926" height="927" alt="image" src="https://github.com/user-attachments/assets/8384f56d-be66-42fe-aac9-23aad3934ac3" />
<img width="822" height="897" alt="image" src="https://github.com/user-attachments/assets/f5daba42-dda9-423b-af31-492b0efd4799" />
<img width="832" height="870" alt="image" src="https://github.com/user-attachments/assets/91d0296e-3aa6-43ad-be24-a8c54bbad483" />
<img width="822" height="302" alt="image" src="https://github.com/user-attachments/assets/5447a76b-28fb-4ac9-9697-34e245f7787f" />




<img width="567" height="552" alt="image" src="https://github.com/user-attachments/assets/f2b07c52-67a9-4b8e-a1d9-ecc5399a0655" />


<img width="727" height="717" alt="image" src="https://github.com/user-attachments/assets/c0bbe546-d7c0-47a3-9ae1-75b49ecab243" />
<img width="731" height="487" alt="image" src="https://github.com/user-attachments/assets/023eb1f4-2758-426c-a6ae-20c458f13537" />




<img width="600" height="780" alt="image" src="https://github.com/user-attachments/assets/7b7b448a-d420-4fa2-9e32-fb05758d723c" />
<img width="572" height="902" alt="image" src="https://github.com/user-attachments/assets/b119b64b-9410-44b6-b4b0-962e6ea3f548" />


  
Current Scope and Notes
This README describes the implementation represented by the supplied
source files and configuration.
- The API Gateway currently has an explicit route for Quiz Service.
- Quiz Service communicates with Question Service using OpenFeign.
- Question Service is configured to run as two instances for the
  load-balancing demonstration.
- The supplied implementation uses PostgreSQL for both Question
  Service and Quiz Service.
- The code shown does not include Spring Security, JWT authentication,
  or role-based authorization.
- The code shown does not include Docker, Kubernetes, a message
  broker, or a centralized configuration server.
- The score calculation is delegated to Question Service.
These capabilities are not claimed as implemented features.


Future Enhancements
Potential improvements for future versions include:

- Add centralized configuration management.
- Add distributed tracing and centralized logging.
- Add health checks, metrics, and monitoring.
- Add circuit breakers, timeouts, and retry policies for inter-service
  calls.


  
Author
Prem Swaroop

Java Developer
License
Add a license file to the repository if you intend to distribute the
project under a specific open-source license.


