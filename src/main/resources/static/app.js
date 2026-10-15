
const API_URL = "/api/tasks";

const taskForm = document.querySelector("#task-form");
const taskList = document.querySelector("#task-list");
const datedTaskList = document.querySelector("#dated-task-list");
const undatedTaskList = document.querySelector("#undated-task-list");
const taskFilter = document.querySelector("#task-filter");

const formMessage = document.querySelector("#form-message");
const listMessage = document.querySelector("#list-message");
const submitButton = document.querySelector("#submit-button");

const totalCount = document.querySelector("#total-count");
const todoCount = document.querySelector("#todo-count");
const completedCount = document.querySelector("#completed-count");

const gmailScanButton = document.querySelector("#gmail-scan-button");
const gmailScanMessage = document.querySelector("#gmail-scan-message");

const senderManagerToggle =
    document.querySelector("#sender-manager-toggle");
const senderManager = document.querySelector("#sender-manager");
const senderForm = document.querySelector("#sender-form");
const senderEmailInput = document.querySelector("#sender-email");
const senderTaskTitleInput = document.querySelector("#sender-task-title");
const senderSubmitButton = document.querySelector("#sender-submit-button");
const senderFormMessage = document.querySelector("#sender-form-message");
const senderList = document.querySelector("#sender-list");


let tasks = [];

async function apiRequest(url, options = {}) {
    const response = await fetch(url, options);

    if (!response.ok) {
        let message = `Request failed (${response.status})`;

        try {
            const body = await response.json();
            if (body.detail) {
                message = body.detail;
            } else if (body.message) {
                message = body.message;
            }
        } catch {
            // Keep the default message if the response isn't JSON.
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function showMessage(element, message, type = "") {
    element.textContent = message;
    element.className = `message ${type}`.trim();
}

function formatDate(dateString) {
    if (!dateString) {
        return "No deadline";
    }

    const [year, month, day] = dateString.split("-").map(Number);
    const date = new Date(year, month - 1, day);

    return new Intl.DateTimeFormat(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric"
    }).format(date);
}

function createElement(tag, className, text) {
    const element = document.createElement(tag);

    if (className) {
        element.className = className;
    }

    if (text !== undefined) {
        element.textContent = text;
    }

    return element;
}

function updateStats() {
    totalCount.textContent = tasks.length;

    todoCount.textContent =
        tasks.filter(task => !task.completed).length;

    completedCount.textContent =
        tasks.filter(task => task.completed).length;
}

function renderTasks() {
    datedTaskList.replaceChildren();
    undatedTaskList.replaceChildren();

    const filter = taskFilter.value;

    // First, apply the selected filter.
    const visibleTasks = tasks.filter(task => {
        if (filter === "todo") {
            return !task.completed;
        }

        if (filter === "completed") {
            return task.completed;
        }

        return true;
    });

    // Separate tasks with and without due dates.
    const datedTasks = visibleTasks
        .filter(task => task.dueDate)
        .sort((a, b) => a.dueDate.localeCompare(b.dueDate));

    const undatedTasks = visibleTasks
        .filter(task => !task.dueDate);

    // Show an appropriate empty state if there are no visible tasks.
    if (visibleTasks.length === 0) {
        const emptyState = createElement("div", "empty-state");

        emptyState.append(
            createElement(
                "div",
                "empty-icon",
                filter === "completed" ? "✓" : "✦"
            ),
            createElement(
                "h3",
                "",
                filter === "completed"
                    ? "No completed tasks yet"
                    : filter === "todo"
                        ? "You're all caught up!"
                        : "Your task list is empty"
            ),
            createElement(
                "p",
                "",
                filter === "completed"
                    ? "Finished tasks will appear here."
                    : filter === "todo"
                        ? "Enjoy the breathing room, or add a new task."
                        : "Add your first task using the form."
            )
        );

        datedTaskList.append(emptyState);
        return;
    }

    // Display tasks with due dates, earliest first.
    for (const task of datedTasks) {
        datedTaskList.append(createTaskCard(task));
    }

    // Display tasks without due dates separately.
    for (const task of undatedTasks) {
        undatedTaskList.append(createTaskCard(task));
    }
}

function createTaskCard(task) {
    const card = createElement(
        "article",
        `task-card${task.completed ? " is-completed" : ""}`
    );

    const checkbox = createElement("input", "task-check");
    checkbox.type = "checkbox";
    checkbox.checked = task.completed;
    checkbox.setAttribute(
        "aria-label",
        `Mark ${task.title} as ${task.completed ? "incomplete" : "complete"}`
    );

    checkbox.addEventListener("change", () => {
        toggleTask(task);
    });

    const content = createElement("div", "task-content");
    const title = createElement("h3", "task-title", task.title);
    content.append(title);

    if (task.description) {
        content.append(
            createElement("p", "task-description", task.description)
        );
    }

    const meta = createElement("div", "task-meta");


    if (task.dueDate) {
        const overdue =
            !task.completed &&
            task.dueDate < getTodayString();

        meta.append(
            createElement(
                "span",
                `task-date${overdue ? " overdue" : ""}`,
                `${overdue ? "Overdue · " : "Due · "}${formatDate(task.dueDate)}`
            )
        );
    } else {
        const addDateButton = createElement(
            "button",
            "icon-button",
            "Add due date"
        );

        addDateButton.type = "button";

        addDateButton.addEventListener("click", () => {
            showDueDateEditor(task, content, addDateButton);
        });

        meta.append(addDateButton);
    }


    if (task.completed) {
        meta.append(createElement("span", "status", "Completed"));
    }

    content.append(meta);

    // Load and display any documents attached to this task.
    const documentsContainer = createElement("div", "task-documents");
    content.append(documentsContainer);

    loadTaskDocuments(task.id, documentsContainer);

    const actions = createElement("div", "task-actions");

    

    const deleteButton = createElement(
        "button",
        "icon-button delete",
        "Delete"
    );

    deleteButton.type = "button";
    deleteButton.setAttribute("aria-label", `Delete ${task.title}`);

    deleteButton.addEventListener("click", () => {
        deleteTask(task);
    });

    actions.append(deleteButton);
    card.append(checkbox, content, actions);

    return card;
}

async function loadTaskDocuments(taskId, container) {
    try {
        const documents = await apiRequest(
            `/api/tasks/${taskId}/documents`
        );

        if (documents.length === 0) {
            return;
        }

        const label = createElement(
            "p",
            "task-documents-label",
            documents.length === 1 ? "Attachment" : "Attachments"
        );

        container.append(label);

        for (const item of documents) {
            const link = createElement(
                "a",
                "task-document-link",
                `📄 ${item.filename}`
            );

            link.href = item.url;
            link.target = "_blank";
            link.rel = "noopener noreferrer";

            container.append(link);
        }
    } catch (error) {
        console.error(
            `Couldn't load documents for task ${taskId}:`,
            error
        );
    }
}

function showDueDateEditor(task, content, addDateButton) {
    // Prevent opening multiple date pickers for the same task card.
    if (content.querySelector(".due-date-editor")) {
        return;
    }

    const editor = createElement("div", "due-date-editor");

    const dateInput = createElement("input", "due-date-input");
    dateInput.type = "date";
    dateInput.setAttribute(
        "aria-label",
        `Due date for ${task.title}`
    );
    dateInput.required = true;

    const saveButton = createElement(
        "button",
        "icon-button",
        "Save date"
    );
    saveButton.type = "button";

    const cancelButton = createElement(
        "button",
        "icon-button",
        "Cancel"
    );
    cancelButton.type = "button";

    const message = createElement("p", "message");

    saveButton.addEventListener("click", async () => {
        if (!dateInput.value) {
            showMessage(message, "Please select a date.", "error");
            return;
        }

        saveButton.disabled = true;
        showMessage(message, "Saving date...");

        try {
            await apiRequest(`/api/tasks/${task.id}/due-date`, {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    dueDate: dateInput.value
                })
            });

            // Reload from the database. The task will move into
            // the dated column and be sorted by its new due date.
            await loadTasks();
        } catch (error) {
            showMessage(
                message,
                `Couldn't save due date. ${error.message}`,
                "error"
            );
            saveButton.disabled = false;
        }
    });

    cancelButton.addEventListener("click", () => {
        editor.remove();
        addDateButton.disabled = false;
    });

    editor.append(dateInput, saveButton, cancelButton, message);
    content.append(editor);

    addDateButton.disabled = true;
    dateInput.focus();
}

function getTodayString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

async function loadTasks() {
    datedTaskList.replaceChildren(
        createElement("p", "loading", "Loading your tasks...")
    );
    undatedTaskList.replaceChildren();

    try {
        tasks = await apiRequest(API_URL);
        updateStats();
        renderTasks();
        showMessage(listMessage, "");
    } catch (error) {
        datedTaskList.replaceChildren();
        undatedTaskList.replaceChildren();

        showMessage(
            listMessage,
            `Couldn't load your tasks. ${error.message}`,
            "error"
        );
    }
}

taskForm.addEventListener("submit", async event => {
    event.preventDefault();

    const formData = new FormData(taskForm);

    const newTask = {
        title: formData.get("title").trim(),
        description: formData.get("description").trim(),
        dueDate: formData.get("dueDate") || null
    };

    submitButton.disabled = true;
    showMessage(formMessage, "Adding task...");

    try {
        await apiRequest(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(newTask)
        });

        taskForm.reset();
        taskFilter.value = "all";

        showMessage(formMessage, "Task added successfully!", "success");
        await loadTasks();
    } catch (error) {
        showMessage(
            formMessage,
            `Couldn't add task. ${error.message}`,
            "error"
        );
    } finally {
        submitButton.disabled = false;
    }
});

async function toggleTask(task) {
    const action = task.completed ? "uncomplete" : "complete";

    try {
        const response = await fetch(
            `/api/tasks/${task.id}/${action}`,
            {
                method: "PATCH"
            }
        );

        if (!response.ok) {
            throw new Error("Could not update task status.");
        }

        await loadTasks();
    } catch (error) {
        console.error("Error updating task:", error);
        alert("Could not update the task. Please try again.");
    }
}

async function deleteTask(task) {
    const confirmed = window.confirm(
        `Are you sure you want to delete "${task.title}"?`
    );

    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`${API_URL}/${task.id}`, {
            method: "DELETE"
        });

        showMessage(listMessage, "Task deleted.", "success");
        await loadTasks();
    } catch (error) {
        showMessage(
            listMessage,
            `Couldn't delete task. ${error.message}`,
            "error"
        );
    }
}


async function loadBillSenders() {
    senderList.replaceChildren(
        createElement("p", "loading", "Loading senders...")
    );

    try {
        const senders = await apiRequest("/api/bill-senders");

        senderList.replaceChildren();

        if (senders.length === 0) {
            senderList.append(
                createElement(
                    "p",
                    "empty-senders",
                    "No senders configured yet."
                )
            );
            return;
        }

        for (const sender of senders) {
            const item = createElement("div", "sender-item");
            const details = createElement("div", "sender-details");

            details.append(
                createElement("strong", "", sender.taskTitle),
                createElement("p", "", sender.senderEmail)
            );

            // Rename button
            const renameButton = createElement(
                "button",
                "icon-button",
                "Rename"
            );

            renameButton.type = "button";

            renameButton.addEventListener("click", () => {
                showSenderRenameEditor(sender, item);
            });

            // Remove button
            const deleteButton = createElement(
                "button",
                "icon-button delete",
                "Remove"
            );

            deleteButton.type = "button";

            deleteButton.addEventListener("click", async () => {
                const confirmed = window.confirm(
                    `Remove "${sender.senderEmail}" from bill scanning?`
                );

                if (!confirmed) {
                    return;
                }

                deleteButton.disabled = true;

                try {
                    await apiRequest(
                        `/api/bill-senders/${sender.id}`,
                        { method: "DELETE" }
                    );

                    showMessage(
                        senderFormMessage,
                        "Sender removed.",
                        "success"
                    );

                    await loadBillSenders();
                } catch (error) {
                    showMessage(
                        senderFormMessage,
                        `Couldn't remove sender. ${error.message}`,
                        "error"
                    );

                    deleteButton.disabled = false;
                }
            });

            item.append(details, renameButton, deleteButton);
            senderList.append(item);
        }
    } catch (error) {
        senderList.replaceChildren();

        showMessage(
            senderFormMessage,
            `Couldn't load senders. ${error.message}`,
            "error"
        );
    }
}


function showSenderRenameEditor(sender, item) {
    // Don't open multiple editors for the same sender.
    if (item.querySelector(".sender-rename-editor")) {
        return;
    }

    const editor = createElement("div", "sender-rename-editor");

    const input = createElement("input", "sender-rename-input");
    input.type = "text";
    input.value = sender.taskTitle;
    input.maxLength = 100;
    input.required = true;
    input.setAttribute("aria-label", "New task title");

    const saveButton = createElement(
        "button",
        "icon-button",
        "Save"
    );
    saveButton.type = "button";

    const cancelButton = createElement(
        "button",
        "icon-button",
        "Cancel"
    );
    cancelButton.type = "button";

    const message = createElement("p", "message");

    saveButton.addEventListener("click", async () => {
        const taskTitle = input.value.trim();

        if (!taskTitle) {
            showMessage(message, "Please enter a name.", "error");
            input.focus();
            return;
        }

        saveButton.disabled = true;
        cancelButton.disabled = true;
        input.disabled = true;

        showMessage(message, "Saving name...");

        try {
            await apiRequest(`/api/bill-senders/${sender.id}`, {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ taskTitle })
            });

            showMessage(
                senderFormMessage,
                "Sender name updated.",
                "success"
            );

            await loadBillSenders();
        } catch (error) {
            showMessage(
                message,
                `Couldn't update name. ${error.message}`,
                "error"
            );

            saveButton.disabled = false;
            cancelButton.disabled = false;
            input.disabled = false;
            input.focus();
        }
    });

    cancelButton.addEventListener("click", () => {
        editor.remove();
    });

    // Allow Enter to save and Escape to cancel.
    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            saveButton.click();
        }

        if (event.key === "Escape") {
            editor.remove();
        }
    });

    editor.append(input, saveButton, cancelButton, message);
    item.append(editor);

    input.focus();
    input.select();
}




senderManagerToggle.addEventListener("click", async () => {
    const opening = senderManager.hidden;
    senderManager.hidden = !opening;

    if (opening) {
        await loadBillSenders();
    }
});

senderForm.addEventListener("submit", async event => {
    event.preventDefault();

    const senderEmail = senderEmailInput.value.trim();
    const taskTitle = senderTaskTitleInput.value.trim();

    senderSubmitButton.disabled = true;
    showMessage(senderFormMessage, "Adding sender...");

    try {
        await apiRequest("/api/bill-senders", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                senderEmail,
                taskTitle
            })
        });

        senderForm.reset();

        showMessage(
            senderFormMessage,
            "Sender added successfully!",
            "success"
        );

        await loadBillSenders();
    } catch (error) {
        showMessage(
            senderFormMessage,
            `Couldn't add sender. ${error.message}`,
            "error"
        );
    } finally {
        senderSubmitButton.disabled = false;
    }
});

taskFilter.addEventListener("change", renderTasks);

gmailScanButton.addEventListener("click", async () => {
    gmailScanButton.disabled = true;
    gmailScanButton.textContent = "Scanning Gmail...";

    showMessage(gmailScanMessage, "Checking for unread bills...");

    try {
        const result = await apiRequest("/api/gmail/scan", {
            method: "POST"
        });

        showMessage(
            gmailScanMessage,
            result.message ||
                `Gmail scan finished. Imported ${result.importedTasks} task(s).`,
            "success"
        );

        await loadTasks();
    } catch (error) {
        showMessage(
            gmailScanMessage,
            `Gmail scan failed. ${error.message}`,
            "error"
        );
    } finally {
        gmailScanButton.disabled = false;
        gmailScanButton.textContent = "Scan Gmail for bills";
    }
});

loadTasks();
