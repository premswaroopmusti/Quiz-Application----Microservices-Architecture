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


Table of Contents
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



Project Evolution
1. Monolithic Version -- QuizApp
The application was first developed as a single Spring Boot application
containing:
- Question Controller and Question Service
- Quiz Controller and Quiz Service
- Question and Quiz repositories (DAOs)
- Question, Quiz, QuestionWrapper, and Response models
In this version, quiz creation, question retrieval, and score
calculation were handled within the same application.
2. Microservices Version
The monolithic application was then used as the reference for a new
microservices implementation. Responsibilities were separated into
independent services:
  Component                           Responsibility
  Question Service                    Owns question data and provides
question-related APIs

  Quiz Service                        Creates quizzes, stores question
IDs, retrieves quiz questions, and
submits answers

  Eureka Service Registry             Maintains registered service
instances and supports service
discovery
API Gateway                         Provides a common entry point and
routes client requests to backend
services
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
Service discovery: Quiz Service, Question Service instances, and API
Gateway register with Eureka. Quiz Service uses the logical service name
QUESTION-SERVICE through OpenFeign instead of hardcoding a Question
Service host and port.

Gateway routing: Client requests to /quiz-service/** are routed to
Quiz Service using the lb://quiz-service URI. The StripPrefix=1
filter removes the /quiz-service prefix before forwarding.

Inter-service communication: Quiz Service invokes Question Service
APIs through its Feign client.
Services
1. Question Service
<details>
<summary>
<strong>{=html}Responsibilities and implementation`</strong>`{=html}
</summary>
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

  GET                   /question/generate?categoryName={category}&numQuestions={count}   Generate random
question IDs

  POST                  /question/getQuestions                                            Retrieve question
details by IDs
POST                  /question/getScore                                                Calculate the score for
submitted responses
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
</details>
2. Quiz Service
<details>
<summary>
<strong>{=html}Responsibilities and implementation`</strong>`{=html}
</summary>
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

  GET                   /quiz/get/{id}        Retrieve questions for
a quiz
POST                  /quiz/submit/{id}     Submit answers and
calculate the score
Quiz model

The Quiz entity contains:
- id
- title
- questionIds -- a list of question IDs stored using JPA
  @ElementCollection
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
</details>
3. Eureka Service Registry
<details>
<summary>
<strong>{=html}Responsibilities and implementation`</strong>`{=html}
</summary>
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
4. API Gateway
<details>
<summary>
<strong>{=html}Responsibilities and implementation`</strong>`{=html}
</summary>
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
</details>
Technology Stack
  Area                                Technologies
  Language                            Java 17

  Framework                           Spring Boot 4.1.2

  Microservices                       Spring Cloud

  API development                     Spring Web MVC, REST APIs

  API Gateway                         Spring Cloud Gateway Server MVC

  Service discovery                   Spring Cloud Netflix Eureka Server
and Client

  Inter-service communication         Spring Cloud OpenFeign

  Load balancing                      Spring Cloud LoadBalancer (through
service discovery integration)

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
  Service                 Database                Data responsibility
  Question Service        questiondb            Question records
Quiz Service            quizdb                Quiz records and their
question ID collections
Question database
Question Service uses the questiondb database to persist question
information.
Quiz database
Quiz Service uses the quizdb database. Its Quiz entity stores the
quiz title and the collection of associated question IDs.

The questionIds field is mapped with:
@ElementCollection
private List<Integer> questionIds;
This allows Quiz Service to retain question references without storing
the full question objects in its own Quiz entity.

Note: The screenshots show generated PostgreSQL tables including
quiz and quiz_question_ids. Hibernate may create or update schema
objects based on the entity mappings.
API Gateway Routes
The configured public route is:
  Client-facing path      Destination service     Forwarded path
  /quiz-service/**      quiz-service          Path after removing
/quiz-service
Examples:

  Client request                  Forwarded endpoint
  /quiz-service/quiz/create     /quiz/create
/quiz-service/quiz/get/1      /quiz/get/1
/quiz-service/quiz/submit/1   /quiz/submit/1

Base URL:
http://localhost:8765
API Documentation
All examples below use the API Gateway as the entry point.
1. Create a Quiz
Endpoint
POST http://localhost:8765/quiz-service/quiz/create
Content-Type: application/json
Request body
{
  "category": "Java",
  "numQuestions": 5,
  "title": "Springers Quiz 1"
}
Successful response
Success
Quiz Service requests random question IDs from Question Service using
the category and requested question count. It then stores the quiz title
and returned IDs in quizdb.
2. Get Quiz Questions
Endpoint
GET http://localhost:8765/quiz-service/quiz/get/1
Example response
[
  {
    "id": 9,
    "questionTitle": "Which Java operator is used to concatenate two strings?",
    "option1": "+",
    "option2": "-",
    "option3": "*",
    "option4": "/"
  },
  {
    "id": 8,
    "questionTitle": "What is the purpose of the \"break\" statement in Java?",
    "option1": "To terminate a loop or switch statement and transfer control to the next statement.",
    "option2": "To skip the rest of the code in a loop and move to the next iteration.",
    "option3": "To define a label for a loop or switch statement.",
    "option4": "To check a condition and execute a block of code repeatedly."
  }
]
The response contains question IDs, question titles, and options. The
correct answer is not included in the QuestionWrapper response.

The full response contains the questions associated with the requested
quiz; the example above is shortened for readability.
3. Submit Quiz Answers
Endpoint
POST http://localhost:8765/quiz-service/quiz/submit/1
Content-Type: application/json
Request body
[
  {
    "id": 10,
    "response": "ArrayList"
  },
  {
    "id": 4,
    "response": "throw"
  },
  {
    "id": 7,
    "response": "do-while loop"
  },
  {
    "id": 2,
    "response": "5"
  },
  {
    "id": 6,
    "response": "final int x = 5;"
  }
]
Example response
5
Quiz Service forwards the submitted responses to Question Service.
Question Service retrieves the corresponding questions, compares each
submitted response with the stored correct answer, and returns the
score.
4. Question Service APIs
These endpoints are available on Question Service itself and are also
used internally by Quiz Service through OpenFeign.
  Method                  Direct local endpoint                                                        Description
  GET                   http://localhost:8080/question/allQuestions                                Get all questions

  GET                   http://localhost:8080/question/category/Java                               Get questions by
category

  POST                  http://localhost:8080/question/add                                         Add a question

  GET                   http://localhost:8080/question/generate?categoryName=Java&numQuestions=5   Generate random
question IDs

  POST                  http://localhost:8080/question/getQuestions                                Get question details
for a list of IDs
POST                  http://localhost:8080/question/getScore                                    Calculate score from
submitted responses
Question Service also runs on port 8081 as a second instance during
the load-balancing test. Use the relevant instance port when testing it
directly.

Example add-question request
POST http://localhost:8080/question/add
Content-Type: application/json
{
  "questionTitle": "Which collection provides a dynamic array in Java?",
  "option1": "HashMap",
  "option2": "ArrayList",
  "option3": "LinkedList",
  "option4": "HashSet",
  "rightAnswer": "ArrayList",
  "difficultylevel": "Easy",
  "category": "Java"
}
Application Flow
The main quiz workflow is:
1. Create quiz: The client sends a category, number of questions,
   and quiz title to the API Gateway.
2. Route request: The Gateway forwards the request to Quiz Service.
3. Generate question IDs: Quiz Service calls Question Service
   through OpenFeign.
4. Select random questions: Question Service queries PostgreSQL and
   returns random question IDs for the requested category.
5. Persist quiz: Quiz Service stores the quiz title and question
   IDs in its own PostgreSQL database.
6. Retrieve quiz: When the client requests a quiz by ID, Quiz
   Service reads its stored question IDs.
7. Fetch question details: Quiz Service calls Question Service to
   retrieve the corresponding question titles and options.
8. Submit answers: The client submits question IDs and selected
   answers.
9. Calculate score: Quiz Service forwards the responses to Question
   Service, which compares them with the correct answers and returns
   the score.
Load Balancing
Two instances of Question Service were started to demonstrate load
balancing:

  Instance                             Port
  Question Service -- Instance 1     8080
Question Service -- Instance 2     8081

Both instances use the same application name:
spring.application.name=question-service
They register with Eureka under the same logical service ID. Quiz
Service uses the Feign client service name rather than a fixed instance
URL:
@FeignClient("QUESTION-SERVICE")
public interface QuizInterface {
}
The project uses Spring Cloud's service discovery and load-balancing
integration to select an available Question Service instance.
Verifying the selected instance
The Question Service controller injects Spring's Environment and
prints the local server port:
@Autowired
Environment environment;

System.out.println(environment.getProperty("local.server.port"));
When requests were sent repeatedly, the console showed requests being
handled by both ports 8080 and 8081.

The Eureka dashboard also showed two registered QUESTION-SERVICE
instances.

This demonstrates service registration, service discovery, and
instance-level request distribution.
Run Locally
Prerequisites
Install and configure:
- JDK 17
- Maven
- PostgreSQL
- An IDE such as IntelliJ IDEA (optional)
- Postman (for API testing)
1. Create PostgreSQL databases
Create the two databases used by the supplied service configurations:
CREATE DATABASE questiondb;
CREATE DATABASE quizdb;
Update the username and password in each service's
application.properties to match your local PostgreSQL setup.
2. Start Eureka Service Registry
Run the Service Registry Spring Boot application.

Default address:
http://localhost:8761
Open the Eureka dashboard in a browser and verify that the registry is
running.
3. Start Question Service -- Instance 1
Run Question Service using its configured port or specify port 8080:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
4. Start Question Service -- Instance 2
Start another instance of the same Question Service application on port
8081:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
Both instances should register with Eureka under QUESTION-SERVICE.

If running from IntelliJ IDEA, create a second run configuration with
the VM/application argument that overrides the server port to 8081.
5. Start Quiz Service
Run Quiz Service on its configured port:
8090
It should register with Eureka and use its Feign client to discover
Question Service.
6. Start API Gateway
Run API Gateway on its configured port:
8765
The Gateway should register with Eureka and load its configured Quiz
Service route.
7. Verify registered services
Open:
http://localhost:8761
Verify that the dashboard lists:
- API-GATEWAY
- QUIZ-SERVICE
- QUESTION-SERVICE with two instances
8. Test through Postman
Use the Gateway base URL:
http://localhost:8765
For example:
POST http://localhost:8765/quiz-service/quiz/create
GET  http://localhost:8765/quiz-service/quiz/get/1
POST http://localhost:8765/quiz-service/quiz/submit/1
Configuration
The following are the important configuration values from the supplied
project files.
  Component               Property                                  Value
  Service Registry        server.port                             8761

  Question Service        spring.application.name                 question-service

  Question Service        PostgreSQL database                       questiondb

  Quiz Service            server.port                             8090

  Quiz Service            spring.application.name                 quiz-service

  Quiz Service            PostgreSQL database                       quizdb

  API Gateway             server.port                             8765
Eureka URL              eureka.client.service-url.defaultZone   http://localhost:8761/eureka/
Question Service's two instances use ports 8080 and 8081 during the
load-balancing test.

Local configuration reminder: The supplied properties contain local
database credentials. Replace them with your own values. Do not commit
real passwords or secrets to GitHub. For a public repository, use
environment variables or an untracked local configuration file.
Concepts Demonstrated
This project provided practical implementation experience with the
following concepts:
Spring Boot and REST APIs
- Building REST controllers with @RestController.
- Mapping endpoints with @GetMapping and @PostMapping.
- Accepting request parameters, path variables, and JSON request
  bodies.
- Returning responses using ResponseEntity.
- Separating controller, service, repository/DAO, and model
  responsibilities.
Microservices Architecture
- Splitting a monolithic application into independently running
  services.
- Assigning separate responsibilities to Question Service and Quiz
  Service.
- Maintaining service-specific database configurations.
- Communicating between services through HTTP APIs.
Service Discovery with Eureka
- Enabling Eureka Server with @EnableEurekaServer.
- Registering services as Eureka clients.
- Discovering services by logical application name.
- Viewing registered instances on the Eureka dashboard.
Inter-Service Communication with OpenFeign
- Enabling Feign clients using @EnableFeignClients.
- Defining service interfaces using @FeignClient.
- Mapping remote endpoints with @GetMapping and @PostMapping.
- Calling Question Service without hardcoding its host and port in
  Quiz Service.
Client-Side Load Balancing
- Registering multiple instances of the same service.
- Running Question Service on ports 8080 and 8081.
- Using service discovery and Spring Cloud LoadBalancer integration.
- Observing the selected instance using
  Environment.getProperty("local.server.port").
API Gateway
- Using Spring Cloud Gateway Server MVC.
- Configuring routes with Path predicates.
- Using lb:// service URIs.
- Applying StripPrefix to transform the incoming request path.
- Exposing a common entry point for client requests.
Persistence with Spring Data JPA
- Mapping entities using @Entity, @Id, and @GeneratedValue.
- Creating repositories using JpaRepository.
- Using derived query methods such as findByCategory.
- Using a native SQL query to select random questions by category.
- Mapping a collection of question IDs using @ElementCollection.
API Testing and Database Inspection
- Testing REST endpoints with Postman.
- Inspecting registered services using the Eureka dashboard.
- Inspecting PostgreSQL databases and generated tables using pgAdmin
  4.
- Verifying quiz records and their associated question IDs.
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
- Add Spring Security with JWT-based authentication and authorization.
- Add request validation and centralized exception handling.
- Add automated unit and integration tests for service and controller
  layers.
- Add Docker Compose to start PostgreSQL and all services together.
- Add centralized configuration management.
- Add distributed tracing and centralized logging.
- Add health checks, metrics, and monitoring.
- Add circuit breakers, timeouts, and retry policies for inter-service
  calls.
- Add API documentation using OpenAPI/Swagger.
- Add CI/CD automation.
Author
Prem Swaroop

Java Developer
License
Add a license file to the repository if you intend to distribute the
project under a specific open-source license.


