buy-01
├── .env
├── .env.example
├── .git
├── .gitignore
├── Backend
│   ├── discovery
│   │   ├── .gitattributes
│   │   ├── .gitignore
│   │   ├── .mvn
│   │   │   └── wrapper
│   │   │       └── maven-wrapper.properties
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── buy01
│   │       │   │       └── discovery
│   │       │   │           └── DiscoveryApplication.java
│   │       │   └── resources
│   │       │       └── application.properties
│   │       └── test
│   │           └── java
│   │               └── buy01
│   │                   └── discovery
│   │                       └── DiscoveryApplicationTests.java
│   ├── gateway
│   │   ├── .gitattributes
│   │   ├── .gitignore
│   │   ├── .mvn
│   │   │   └── wrapper
│   │   │       └── maven-wrapper.properties
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── buy01
│   │       │   │       └── gateway
│   │       │   │           ├── Config
│   │       │   │           │   └── Jwt.java
│   │       │   │           ├── GatewayApplication.java
│   │       │   │           └── Security
│   │       │   │               ├── JwtAuthFilter.java
│   │       │   │               ├── JwtFilter.java
│   │       │   │               └── SecurityConfig.java
│   │       │   └── resources
│   │       │       ├── application.properties
│   │       │       └── keystore.p12
│   │       └── test
│   │           └── java
│   │               └── buy01
│   │                   └── gateway
│   │                       └── GatewayApplicationTests.java
│   ├── jenkins
│   │   ├── backend-slave
│   │   │   └── Dockerfile
│   │   ├── frontend-slave
│   │   │   └── Dockerfile
│   │   └── master
│   │       ├── Dockerfile
│   │       ├── jenkins.yaml
│   │       └── plugins.txt
│   ├── media
│   │   ├── .gitattributes
│   │   ├── .gitignore
│   │   ├── .mvn
│   │   │   └── wrapper
│   │   │       └── maven-wrapper.properties
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── buy01
│   │       │   │       └── media
│   │       │   │           ├── Helpers
│   │       │   │           │   └── Mappers.java
│   │       │   │           ├── MediaApplication.java
│   │       │   │           ├── config
│   │       │   │           │   ├── Exceptions
│   │       │   │           │   │   ├── GlobalExceptionHandler
│   │       │   │           │   │   │   ├── BadRequestHandler.java
│   │       │   │           │   │   │   ├── InternalServerError.java
│   │       │   │           │   │   │   ├── MyForbiddenHandelr.java
│   │       │   │           │   │   │   └── NotFoundHandler.java
│   │       │   │           │   │   └── MyExeptions
│   │       │   │           │   │       ├── InternalError.java
│   │       │   │           │   │       ├── MyBadRequest.java
│   │       │   │           │   │       ├── MyForbiden.java
│   │       │   │           │   │       └── MyNotFound.java
│   │       │   │           │   ├── R2Config
│   │       │   │           │   │   └── R2Config.java
│   │       │   │           │   ├── Security
│   │       │   │           │   │   ├── HeaderAuthFilter.java
│   │       │   │           │   │   └── SecurityConfig.java
│   │       │   │           │   ├── Tika
│   │       │   │           │   │   └── TikaConfig.java
│   │       │   │           │   └── kafka
│   │       │   │           │       ├── KafkaConsumerConfig.java
│   │       │   │           │       └── KafkaProducerConfig.java
│   │       │   │           ├── dto
│   │       │   │           │   ├── kafka
│   │       │   │           │   │   ├── AcceptedUpload.java
│   │       │   │           │   │   ├── AvatarChanged.java
│   │       │   │           │   │   ├── AvatarDeleted.java
│   │       │   │           │   │   ├── DeleteEvent.java
│   │       │   │           │   │   ├── ProductCreated.java
│   │       │   │           │   │   ├── ProductDeleted.java
│   │       │   │           │   │   ├── ProductImageUploadedEvent.java
│   │       │   │           │   │   └── UserDeleted.java
│   │       │   │           │   └── media
│   │       │   │           │       ├── MediaResponse.java
│   │       │   │           │       ├── UpdateMedia.java
│   │       │   │           │       └── UploadRequest.java
│   │       │   │           ├── handler
│   │       │   │           │   └── MediaController.java
│   │       │   │           ├── model
│   │       │   │           │   ├── CheckEntity.java
│   │       │   │           │   └── MediaEntity.java
│   │       │   │           ├── repository
│   │       │   │           │   ├── CheckRepository.java
│   │       │   │           │   └── MediaRepository.java
│   │       │   │           └── service
│   │       │   │               ├── cloudflare
│   │       │   │               │   └── R2StorageService.java
│   │       │   │               ├── kafka
│   │       │   │               │   ├── ConsumeProductEvents.java
│   │       │   │               │   └── ConsumeUserEvents.java
│   │       │   │               └── media
│   │       │   │                   └── MediaService.java
│   │       │   └── resources
│   │       │       └── application.properties
│   │       └── test
│   │           ├── java
│   │           │   └── buy01
│   │           │       └── media
│   │           │           ├── Helpers
│   │           │           │   └── MappersTest.java
│   │           │           ├── MediaApplicationTests.java
│   │           │           └── service
│   │           │               ├── cloudflare
│   │           │               │   └── R2StorageServiceTest.java
│   │           │               └── media
│   │           │                   └── MediaServiceTest.java
│   │           └── resources
│   │               └── application.properties
│   ├── product
│   │   ├── .dockerignore
│   │   ├── .mvn
│   │   │   └── wrapper
│   │   │       └── maven-wrapper.properties
│   │   ├── Dockerfile
│   │   ├── HELP.md
│   │   ├── docker-compose.ymlx
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── pom.xml
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java
│   │   │   │   │   └── Product
│   │   │   │   │       └── Service
│   │   │   │   │           ├── ProductApplication.java
│   │   │   │   │           ├── config
│   │   │   │   │           │   ├── HeaderAuthFilter.java
│   │   │   │   │           │   ├── KafkaConsumerConfig.java
│   │   │   │   │           │   ├── KafkaProducerConfig.java
│   │   │   │   │           │   └── SecurityConfig.java
│   │   │   │   │           ├── controller
│   │   │   │   │           │   └── ProductController.java
│   │   │   │   │           ├── dto
│   │   │   │   │           │   ├── ProductRequest.java
│   │   │   │   │           │   ├── ProductResponse.java
│   │   │   │   │           │   └── kafka
│   │   │   │   │           │       ├── ProductCreated.java
│   │   │   │   │           │       ├── ProductDeleted.java
│   │   │   │   │           │       ├── ProductImageDeletedEvent.java
│   │   │   │   │           │       └── ProductImageUploadedEvent.java
│   │   │   │   │           ├── exception
│   │   │   │   │           │   ├── .gitkeep
│   │   │   │   │           │   ├── ForbiddenException.java
│   │   │   │   │           │   ├── GlobalExceptionHandler.java
│   │   │   │   │           │   └── ProductNotFoundException.java
│   │   │   │   │           ├── model
│   │   │   │   │           │   └── Product.java
│   │   │   │   │           ├── repository
│   │   │   │   │           │   └── ProductRepository.java
│   │   │   │   │           ├── security
│   │   │   │   │           │   └── .gitkeep
│   │   │   │   │           └── service
│   │   │   │   │               ├── ProductService.java
│   │   │   │   │               └── kafka
│   │   │   │   │                   └── ProductMediaEventConsumer.java
│   │   │   │   └── resources
│   │   │   │       └── application.properties
│   │   │   └── test
│   │   │       └── java
│   │   │           └── Product
│   │   │               └── Service
│   │   │                   ├── ProductServiceTest.java
│   │   │                   ├── controller
│   │   │                   │   ├── ProductControllerTest.java
│   │   │                   │   └── ProductControllerWebMvcTest.java
│   │   │                   └── service
│   │   │                       ├── ProductApplicationTests.java
│   │   │                       └── kafka
│   │   │                           └── ProductMediaEventConsumerTest.java
│   │   └── target
│   └── user
│       ├── .gitattributes
│       ├── .gitignore
│       ├── .mvn
│       │   └── wrapper
│       │       └── maven-wrapper.properties
│       ├── Dockerfile
│       ├── mvnw
│       ├── mvnw.cmd
│       ├── pom.xml
│       └── src
│           ├── main
│           │   ├── java
│           │   │   └── buy01
│           │   │       └── user
│           │   │           ├── UserApplication.java
│           │   │           ├── config
│           │   │           │   ├── Exceptions
│           │   │           │   │   ├── GlobalExceptionHandler
│           │   │           │   │   │   ├── badRequestHandler.java
│           │   │           │   │   │   ├── conflictHandler.java
│           │   │           │   │   │   ├── forbiddenHandler.java
│           │   │           │   │   │   ├── notFoundHandler.java
│           │   │           │   │   │   └── unauthrizedHandler.java
│           │   │           │   │   └── MyExeptions
│           │   │           │   │       ├── Conflict.java
│           │   │           │   │       ├── Myforbiden.java
│           │   │           │   │       ├── badRequest.java
│           │   │           │   │       ├── notFound.java
│           │   │           │   │       └── unauthorized.java
│           │   │           │   ├── Helpers
│           │   │           │   │   ├── KafkaConsumerConfig.java
│           │   │           │   │   ├── KafkaProducerConfig.java
│           │   │           │   │   ├── Mapper.java
│           │   │           │   │   └── UserDetailsServiceImpl.java
│           │   │           │   ├── Jwt
│           │   │           │   │   └── Jwt.java
│           │   │           │   └── Security
│           │   │           │       ├── FilterChainConfig.java
│           │   │           │       ├── HeaderAuthFilter.java
│           │   │           │       └── JwtFilter.java
│           │   │           ├── dto
│           │   │           │   ├── Auth
│           │   │           │   │   ├── authRequest.java
│           │   │           │   │   ├── authResponse.java
│           │   │           │   │   ├── loginRequest.java
│           │   │           │   │   └── registerRequest.java
│           │   │           │   ├── User
│           │   │           │   │   ├── UpdateMe.java
│           │   │           │   │   └── Userdto.java
│           │   │           │   └── kafka
│           │   │           │       ├── AcceptedUpload.java
│           │   │           │       ├── AvatarChanged.java
│           │   │           │       ├── AvatarDeleted.java
│           │   │           │       ├── DeleteEvent.java
│           │   │           │       ├── MediaUploadEvent.java
│           │   │           │       └── UserDeleted.java
│           │   │           ├── handler
│           │   │           │   ├── Auth
│           │   │           │   │   └── Authentication.java
│           │   │           │   └── usersHandler
│           │   │           │       └── usersController.java
│           │   │           ├── model
│           │   │           │   ├── Roles.java
│           │   │           │   └── userEntity.java
│           │   │           ├── repository
│           │   │           │   └── userRepository.java
│           │   │           └── service
│           │   │               ├── Auth
│           │   │               │   ├── loginService.java
│           │   │               │   └── registerService.java
│           │   │               ├── kafka
│           │   │               │   ├── CheckEventConsumer.java
│           │   │               │   └── MediaEventProducer.java
│           │   │               └── usersService
│           │   │                   └── usersService.java
│           │   └── resources
│           │       └── application.properties
│           └── test
│               └── java
│                   └── buy01
│                       └── user
│                           ├── config
│                           │   ├── Helpers
│                           │   │   └── MapperTest.java
│                           │   └── Jwt
│                           │       └── JwtTest.java
│                           └── service
│                               ├── Auth
│                               │   ├── LoginServiceTest.java
│                               │   └── RegisterServiceTest.java
│                               └── usersService
│                                   └── UsersServiceTest.java
├── Jenkinsfile
├── README.md
├── docker-compose.jenkins.yml
├── docker-compose.yml
├── marketplace-ui
│   ├── .dockerignore
│   ├── .editorconfig
│   ├── .gitignore
│   ├── .prettierrc
│   ├── Dockerfile
│   ├── README.md
│   ├── angular.json
│   ├── nginx.conf
│   ├── package-lock.json
│   ├── package.json
│   ├── public
│   │   ├── assets
│   │   │   └── images
│   │   │       ├── background.png
│   │   │       ├── logo.png
│   │   │       └── logo2.png
│   │   └── favicon.ico
│   ├── src
│   │   ├── app
│   │   │   ├── app.config.ts
│   │   │   ├── app.html
│   │   │   ├── app.routes.ts
│   │   │   ├── app.scss
│   │   │   ├── app.spec.ts
│   │   │   ├── app.ts
│   │   │   ├── core
│   │   │   │   ├── guards
│   │   │   │   │   ├── auth-guard.spec.ts
│   │   │   │   │   ├── auth-guard.ts
│   │   │   │   │   ├── no-auth-guard.spec.ts
│   │   │   │   │   ├── no-auth-guard.ts
│   │   │   │   │   ├── role-guard.spec.ts
│   │   │   │   │   └── role-guard.ts
│   │   │   │   ├── interceptors
│   │   │   │   │   ├── auth.interceptor.spec.ts
│   │   │   │   │   └── auth.interceptor.ts
│   │   │   │   ├── models
│   │   │   │   │   ├── auth-request.ts
│   │   │   │   │   ├── auth-response.ts
│   │   │   │   │   ├── media.ts
│   │   │   │   │   ├── product.ts
│   │   │   │   │   └── user.ts
│   │   │   │   └── services
│   │   │   │       ├── auth.spec.ts
│   │   │   │       ├── auth.ts
│   │   │   │       ├── confirm.ts
│   │   │   │       ├── media.ts
│   │   │   │       ├── product.spec.ts
│   │   │   │       ├── product.ts
│   │   │   │       ├── profile.ts
│   │   │   │       ├── toast.service.ts
│   │   │   │       └── user.ts
│   │   │   ├── features
│   │   │   │   ├── auth
│   │   │   │   │   └── pages
│   │   │   │   │       ├── login
│   │   │   │   │       │   ├── login.html
│   │   │   │   │       │   ├── login.scss
│   │   │   │   │       │   ├── login.spec.ts
│   │   │   │   │       │   └── login.ts
│   │   │   │   │       └── register
│   │   │   │   │           ├── register.html
│   │   │   │   │           ├── register.scss
│   │   │   │   │           ├── register.spec.ts
│   │   │   │   │           └── register.ts
│   │   │   │   ├── errors
│   │   │   │   │   └── pages
│   │   │   │   │       ├── not-found
│   │   │   │   │       │   ├── not-found.html
│   │   │   │   │       │   ├── not-found.scss
│   │   │   │   │       │   └── not-found.ts
│   │   │   │   │       ├── server-error
│   │   │   │   │       │   ├── server-error.html
│   │   │   │   │       │   ├── server-error.scss
│   │   │   │   │       │   └── server-error.ts
│   │   │   │   │       └── unauthorized
│   │   │   │   │           ├── unauthorized.html
│   │   │   │   │           ├── unauthorized.scss
│   │   │   │   │           └── unauthorized.ts
│   │   │   │   ├── media
│   │   │   │   │   └── pages
│   │   │   │   │       └── media-library
│   │   │   │   │           ├── media-library.html
│   │   │   │   │           ├── media-library.scss
│   │   │   │   │           ├── media-library.spec.ts
│   │   │   │   │           └── media-library.ts
│   │   │   │   ├── products
│   │   │   │   │   └── pages
│   │   │   │   │       ├── product-details
│   │   │   │   │       │   ├── product-details.html
│   │   │   │   │       │   ├── product-details.scss
│   │   │   │   │       │   ├── product-details.spec.ts
│   │   │   │   │       │   └── product-details.ts
│   │   │   │   │       └── product-list
│   │   │   │   │           ├── product-list.html
│   │   │   │   │           ├── product-list.scss
│   │   │   │   │           ├── product-list.spec.ts
│   │   │   │   │           └── product-list.ts
│   │   │   │   ├── profile
│   │   │   │   │   └── pages
│   │   │   │   │       └── profile
│   │   │   │   │           ├── profile.html
│   │   │   │   │           ├── profile.scss
│   │   │   │   │           ├── profile.spec.ts
│   │   │   │   │           └── profile.ts
│   │   │   │   └── seller
│   │   │   │       └── pages
│   │   │   │           ├── dashboard
│   │   │   │           │   ├── dashboard.html
│   │   │   │           │   ├── dashboard.scss
│   │   │   │           │   ├── dashboard.spec.ts
│   │   │   │           │   └── dashboard.ts
│   │   │   │           └── media-management
│   │   │   │               ├── media-management.html
│   │   │   │               ├── media-management.scss
│   │   │   │               └── media-management.ts
│   │   │   ├── layout
│   │   │   │   └── navbar
│   │   │   │       ├── navbar.html
│   │   │   │       ├── navbar.scss
│   │   │   │       ├── navbar.spec.ts
│   │   │   │       └── navbar.ts
│   │   │   └── shared
│   │   │       └── components
│   │   │           └── confirm
│   │   │               ├── confirm.html
│   │   │               ├── confirm.scss
│   │   │               └── confirm.ts
│   │   ├── environments
│   │   │   ├── environment.prod.ts
│   │   │   └── environment.ts
│   │   ├── index.html
│   │   ├── main.ts
│   │   ├── styles.scss
│   │   └── test-setup.ts
│   ├── ssl
│   │   ├── server.crt
│   │   └── server.key
│   ├── tsconfig.app.json
│   ├── tsconfig.json
│   └── tsconfig.spec.json
├── package-lock.json
└── scripts
    ├── check.sh
    ├── clear.sh
    ├── create_Self-Signed-Certificate.sh
    ├── detect-changed-services.sh
    ├── get_id.sh
    └── start.sh