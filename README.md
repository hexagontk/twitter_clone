
# Twitter Clone

This is a Mini Twitter Clone application developed using HexagonKt, based on
[this](http://sparkjava.com/tutorials/twitter-clone) Spark tutorial. It makes use of Pebble to
render pages, and MongoDB for database functions.

## Requirements

* Java + Kotlin
* Gradle
* MongoDB

## How to run

Navigate to the project's root folder and run the following commands in order

1. `./gradlew build`
2. `./gradlew installDist`
3. `./gradlew run`

Open `http://localhost:2010/` in your browser.

## General Functionality

* Sign up and register
* Public and personal timelines
* Follow/unfollow users
* User pages

## Architecture

The main class, which starts the server, is `Application.kt`.

The `models` package contains the models to be stored in the database.

The `routes` package contains the routing logic files.

`Utils.kt` contains some general-purpose helper methods used in other parts of the application.

Inside `resources`, the `templates` folder contains the Pebble templates used to render webpages.
`base.html` contains some common components such as the navbar, and other templates extend from this
file.

The `service.yaml` file defines several local settings such as the server port number, MongoDB URL,
etc. This can be modified, if required, based on local settings.

## Endpoints

Note: for all `POST` endpoints, data is expected in the form of form parameters
(x-www-form-url-encoded).

### `ANY /ping`
A simple ping endpoint to check if the server is up and running.

### `GET /`
The homepage. If no user is logged in, this redirects to `/public`, else a feed of messages of users
followed by the currently logged in user is shown.

### `GET /public`
Shows the list of messages from all users.

### `GET /register`
Renders the registration page.

### `POST /register`
Registers the user.

Expected data:
* username
* email
* password

If registration is successful, redirects to `/login`.

### `GET /login`
Renders the login page.

### `POST /login`
Logs the user in. Logged in users are tracked using Session variables.

Expected data:
* email
* password

If login is successful, redirects to the home page (`/`).

### `GET /logout`
Logs the user out and redirects to the home page (`/`).

### `GET /user/follow/:username`
Adds the currently logged in user as a follower of the user specified in the path.

### `GET /user/unfollow/:username`
Removes the currently logged in user from the list of followers of the user specified in the path.

### `GET /user/:username`
Renders the user page, containing the feed of messages sent by the user.

### `POST /message`
Creates a message and associates it with the currently logged in user.

Expected data:
* message

If successful, redirects to the public timeline (`/public`).