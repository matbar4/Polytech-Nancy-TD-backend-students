package com.example.todoapp.presentation;

import com.example.todoapp.model.JsonUtils;
import com.example.todoapp.model.Task;
import com.example.todoapp.model.dto.ErrorDto;
import com.example.todoapp.model.dto.TaskCreateDto;
import com.example.todoapp.model.dto.TaskResponseDto;
import com.example.todoapp.model.dto.TaskUpdateDto;
import com.example.todoapp.persistence.TaskDao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

/**
 * Main class of the application. Managing routing and HTTP layer.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private static final TaskDao dao = new TaskDao();

    public static void main(String[] args) throws Exception {
        log.info("In-memory repository initialised");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", Application::handleTasks);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:8080");
    }

    private static void handleTasks(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();


            //region Manage POST /tasks
            if ("POST".equals(method) && "/tasks".equals(path)) {
                TaskCreateDto dto = JsonUtils.deserialize(new String(exchange.getRequestBody().readAllBytes(), UTF_8), TaskCreateDto.class);
                ErrorDto error = validateCreate(dto);
                if (nonNull(error)) {
                    sendResponse(exchange, 400, JsonUtils.serialize(error));
                    return;
                }

                Task created = dao.save(dto);
                TaskResponseDto response = TaskResponseDto.from(created);

                exchange.getResponseHeaders().add("Location", "/tasks/" + response.id());
                sendResponse(exchange, 201, JsonUtils.serialize(response));
                return;
            }
            //endregion

            //region Manage GET /tasks
            if ("GET".equals(method) && "/tasks".equals(path)) {
                String query = exchange.getRequestURI().getQuery();
                boolean todoOnly = query != null && query.contains("todo-only=true");

                List<TaskResponseDto> tasks = dao.findAll(todoOnly).stream().map(TaskResponseDto::from).toList();

                if (tasks.isEmpty()) {
                    sendResponse(exchange, 204, null);
                } else {
                    sendResponse(exchange, 200, JsonUtils.serialize(tasks));
                }
                return;
            }
            //endregion

            //region Manage GET /tasks/{id}
            Matcher m = ID_PATH.matcher(path);
            if ("GET".equals(method) && m.matches()) {
                int id = Integer.parseInt(m.group(1));
                Optional<Task> task = dao.findById(id);

                if (task.isPresent()) {
                    sendResponse(exchange, 200, JsonUtils.serialize(TaskResponseDto.from(task.get())));
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }
            //endregion

            //region Manage DELETE /tasks/{id}
            if ("DELETE".equals(method) && m.matches()) {
                int id = Integer.parseInt(m.group(1));
                boolean deleted = dao.deleteById(id);

                if (deleted) {
                    sendResponse(exchange, 204, null);
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }
            //endregion

            //region Manage PUT /tasks/{id}
            if ("PUT".equals(method) && m.matches()) {
                int id = Integer.parseInt(m.group(1));
                TaskUpdateDto dto = JsonUtils.deserialize(new String(exchange.getRequestBody().readAllBytes(), UTF_8), TaskUpdateDto.class);

                ErrorDto error = validateUpdate(dto);
                if (nonNull(error)) {
                    sendResponse(exchange, 400, JsonUtils.serialize(error));
                    return;
                }

                boolean updated = dao.update(id, dto);

                if (updated) {
                    sendResponse(exchange, 204, null);
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }
            //endregion

            // Otherwise → 404
            sendResponse(exchange, 404, null);
        } catch (Exception e) {
            log.error("Unexpected error while handling {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
            try {
                sendResponse(exchange, 500, JsonUtils.serialize(new ErrorDto("server", "An unexpected error occurred")));
            } catch (Exception ignored){}
        }
    }



        /**
         * Validates a {@link TaskCreateDto}.
         * @param dto DTO to validate.
         * @return an {@link ErrorDto} if invalid, null otherwise.
         */
    private static ErrorDto validateCreate(TaskCreateDto dto) {
        if (dto.title() == null || dto.title().isBlank()) {
            return new ErrorDto("title", "Title is required");
        }
        if (dto.title().length() > 50) {
            return new ErrorDto("title", "Title must not exceed 50 characters");
        }
        if (dto.description() != null && dto.description().length() > 255) {
            return new ErrorDto("description", "Description must not exceed 255 characters");
        }
        return null;
    }

    /**
     * Validates a {@link TaskUpdateDto}.
     * @param dto DTO to validate.
     * @return an {@link ErrorDto} if invalid, null otherwise.
     */
    private static ErrorDto validateUpdate(TaskUpdateDto dto) {
        if (dto.title() == null || dto.title().isBlank()) {
            return new ErrorDto("title", "Title is required");
        }
        if (dto.title().length() > 50) {
            return new ErrorDto("title", "Title must not exceed 50 characters");
        }
        if (dto.description() != null && dto.description().length() > 255) {
            return new ErrorDto("description", "Description must not exceed 255 characters");
        }
        return null;
    }





    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if(nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }
    }
}
