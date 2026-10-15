# LifeAdmin

LifeAdmin is a personal administration dashboard built with Java and Spring Boot. It helps organize tasks, keep track of deadlines, and manage bills by importing PDF invoices from Gmail.

The application runs locally on your computer, with tasks and related data stored in a local database.

## Features

- **Task management** — Create, complete, reopen, and delete tasks.
- **Due dates** — Assign deadlines to tasks, with overdue tasks highlighted automatically.
- **Task organization** — View tasks with deadlines separately from tasks without deadlines. Tasks with deadlines are sorted by date.
- **Gmail integration** — Scan unread emails from configured senders for PDF invoices.
- **Automatic invoice import** — Create tasks from matching emails and save their PDF attachments locally.
- **Configurable bill senders** — Add, view, and remove email senders and configure the task title used for their imported invoices.
- **Persistent storage** — Keep tasks, sender settings, and invoice metadata between application restarts.
- **Local PDF storage** — Associate imported invoice documents with their corresponding tasks.

## Technologies

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- H2 Database
- Maven
- HTML, CSS, and JavaScript
- Gmail API with Google OAuth 2.0

## Requirements

Before running LifeAdmin, make sure you have:

- Java 21 installed.
- Git, if cloning the repository.
- An internet connection for the initial Maven dependency download.
- A Google account and Google Cloud OAuth configuration if you want to use Gmail integration.

Maven does not need to be installed separately because the project includes the Maven Wrapper.

## Getting started

### 1. Get the project

Clone the repository:

```bash
git clone https://github.com/katarinafaks/LifeAdmin.git
cd LifeAdmin
```

Alternatively, download the repository as a ZIP file and extract it.

### 2. Start the application on Windows

Double-click `Start LifeAdmin.bat` if the startup script is included in your copy of the project.

The script starts the Spring Boot application, waits for it to become available, and opens the application in your default browser.

Alternatively, open a terminal in the project directory and run:

```powershell
.\mvnw.cmd spring-boot:run
```

Once the application has started, open:

[http://localhost:8080/](http://localhost:8080/)

Keep the terminal running while using the application. Stop the server with `Ctrl+C` in its terminal.

### 3. Build and test

To compile the project:

```powershell
.\mvnw.cmd compile
```

To run the tests:

```powershell
.\mvnw.cmd test
```

## Gmail integration setup

Gmail integration is optional. The rest of the application can be used without configuring it.

To enable Gmail invoice importing, configure your own Google OAuth credentials.

### 1. Configure Google Cloud

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the Gmail API.
3. Configure the OAuth consent screen.
4. Create an OAuth client ID with the **Desktop app** application type.
5. If the application is in Testing mode, add your Google account as a test user.

### 2. Add the credentials

Download the OAuth client JSON file and place it at:

```text
secrets/credentials.json
```

Do not commit this file or share it publicly.

### 3. Authorize Gmail access

Start LifeAdmin and use the Gmail scanning feature. On the first authorization, follow the Google sign-in and consent flow in your browser.

The application uses Gmail's read-only scope to find matching unread emails and retrieve their PDF attachments. OAuth tokens are stored locally in the `tokens/` directory.

Google OAuth apps in Testing mode can have refresh tokens that expire after seven days when using Gmail scopes. Reauthorization may therefore be required periodically.

### 4. Configure bill senders

Use **Manage senders** in the dashboard to add email addresses from which you receive bills and specify the task title to use for imported invoices.

When you scan Gmail, LifeAdmin searches unread messages from enabled senders that have PDF attachments. Emails already processed are tracked to avoid repeatedly importing the same message.

## Data storage

LifeAdmin stores its data locally:

| Location | Purpose |
|---|---|
| `data/` | H2 database containing tasks, sender settings, and related records |
| `documents/` | Downloaded PDF invoices |
| `secrets/` | Google OAuth client credentials |
| `tokens/` | Locally stored Google OAuth tokens |

These directories contain local or potentially sensitive data and should not be committed to Git.

Deleting an imported task also removes its associated document records and attempts to delete the corresponding downloaded PDF files. The associated email can be eligible for importing again if it remains unread.

## Security and privacy

LifeAdmin is designed for personal, local use.

- Application data and downloaded documents are stored on the local computer.
- Gmail access requires explicit Google OAuth authorization.
- Gmail integration uses read-only access rather than sending or modifying emails.
- Credentials, tokens, database files, and downloaded documents should remain outside version control.

The application is not currently designed as a multi-user service. Do not expose it to the public internet or use it to store other people's private data without implementing appropriate authentication and security controls.

## Future improvements

Potential future improvements include:

- Editing and removing existing due dates.
- More advanced task filtering and sorting.
- Improved invoice categorization and metadata extraction.
- Easier installation and distribution as a standalone desktop application.

## License

No license has been specified yet. Until a license is added, all rights remain with the copyright holder, and others do not automatically receive permission to reuse or redistribute the code.