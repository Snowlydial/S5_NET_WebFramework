# S5_NET_WebFramework

A custom Java web framework built from scratch to understand how Spring MVC works under the hood. Instead of using the framework as a black box, the goal was to implement the core mechanics — annotation scanning, request routing, parameter resolution — piece by piece.

The result is a lightweight, Servlet-based dispatcher that routes HTTP requests to user-defined controller classes without depending on Spring itself.

## Features

### What it does

- Front Controller pattern via `FrontServlet` — a single entry point for all incoming requests
- Annotation-based routing with `@AnotController`, `@AnotGetMapping`, `@AnotPostMapping`, and `@AnotRequestMapping`
- Path parameter support with dynamic pattern matching (e.g. `/users/{id}`)
- Classpath scanning on startup via `ControllerScanner` — no manual registration
- Reflection-based method dispatch: controllers are instantiated and invoked at runtime
- Parameter resolution from request data, path variables, and session objects (`@AnotParam`, `@AnotSession`)
- JSON response serialization via `@AnotJSON` and `JsonResponse`
- File upload handling via `FileUpload`
- Authorization checking with `@AnotRole` before method invocation
- Request filtering via `ResourceFilter` and `CustomRequestWrapper`

### Why this project matters

- It forces a concrete understanding of what a framework actually does at the HTTP layer
- Annotation scanning and reflective dispatch are things that look like magic until you implement them yourself
- The pipeline (receive → match → resolve → invoke → respond) maps directly to how Spring MVC and similar frameworks are structured internally

## Tech Stack

- Language: Java
- Servlet API: Jakarta Servlet 6.0
- Build: Maven
- Runtime: Apache Tomcat

## Project Structure

```
src/main/java/com/snowly/framework/
├── FrontServlet.java               # Central dispatcher servlet
├── Annotations/
│   ├── AnotController.java
│   ├── AnotGetMapping.java
│   ├── AnotPostMapping.java
│   ├── AnotRequestMapping.java
│   ├── AnotParam.java
│   ├── AnotRole.java
│   ├── AnotSession.java
│   ├── AnotURL.java
│   ├── AnotJSON.java
│   └── Authorized.java
├── Util/
│   ├── ControllerScanner.java
│   ├── Mapping.java
│   ├── ModelView.java
│   ├── JsonResponse.java
│   ├── FileUpload.java
│   └── SessionMap.java
└── Filters/
    └── ResourceFilter.java
```

## Getting Started

### Prerequisites

- Java 11+
- Apache Maven
- Apache Tomcat

### Build and deploy

```bash
# 1) Clone the repository
git clone https://github.com/Snowlydial/S5_NET_WebFramework.git
cd S5_NET_WebFramework

# 2) Build the project
mvn clean package

# 3) Deploy the output to Tomcat
```

### Defining a controller

```java
@AnotController
public class UserController {

    @AnotGetMapping("/users/{id}")
    public ModelView getUser(@AnotParam("id") String id) {
        ModelView mv = new ModelView("user");
        mv.addObject("userId", id);
        return mv;
    }

    @AnotGetMapping("/api/users")
    @AnotJSON
    public List<User> listUsers() {
        return userService.getAll();
    }
}
```

`FrontServlet` scans the classpath on startup, registers all annotated controller methods, and dispatches incoming requests to the matching handler.

## Academic context

Built during Semester 5 at IT University as a networks and web programming project.
