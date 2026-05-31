package com.example.todoapp.persistence;

import com.example.todoapp.model.Task;
import com.example.todoapp.model.dto.TaskCreateDto;
import com.example.todoapp.model.dto.TaskUpdateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.*;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private static final Logger log = LoggerFactory.getLogger(TaskDao.class);
    private static final String dbUrl = "jdbc:sqlite:DatabaseTask.db";

    public TaskDao() {
        initDatabase();
    }



    /**
     * Creates the tasks table if it doesn't exist yet
     */

    private void initDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    done BOOLEAN NOT NULL DEFAULT 0
                )
                """;
        try (Connection conn = getConnection()){
            Statement stmt = conn.createStatement();

            stmt.execute(sql);
            log.info("Table 'tasks' created");

        } catch (SQLException e) {
            log.error("Impossible to initiate table'",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Connect to the database
     * @return {@link Connection} to the database
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Maps the current row of a {@link ResultSet} to a {@link Task}.
     * @param rs ResultSet positioned on a row.
     * @return {@link Task} built from the row.
     * @throws SQLException if a column cannot be read.
     */

    private Task mapRow(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("done") == 1
        );
    }


    /**
     * Persist {@link Task} model.
     * @param dto task to save.
     * @return task model.
     */
    public Task save(TaskCreateDto dto) {
        String sql = "INSERT INTO tasks (title, description, done) VALUES (?, ?, 0)";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dto.title());
            stmt.setString(2, dto.description());
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            int generatedId = generatedKeys.getInt(1);

            log.info("Task saved with id: " + generatedId);
            return new Task(generatedId, dto.title(), dto.description(), false);

        } catch (SQLException e) {
            log.error("Failed to save task", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete {@link Task} model by id.
     * @param id identifier of the {@link Task}.
     * @return True if task existed
     */
    public boolean deleteById(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();

            log.info("Task deleted with id: " + id);
            return rows > 0;

        } catch (SQLException e){
            log.error("Failed to delete task :" + id, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve {@link Task} model by id.
     * @param id identifier of the {@link Task}.
     * @return {@link Task} model wrapped by Optional.
     */
    public Optional<Task> findById(int id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()){
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();

        } catch (SQLException e){
            log.error("Failed to find task with id: " + id, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve all tasks
     * @param todoOnly if true, only tasks to do
     * @return list of tasks
     */
    public List<Task> findAll(boolean todoOnly) {
        String sql = todoOnly ? "SELECT * FROM tasks WHERE done = 0" : "SELECT * FROM tasks";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)){
            ResultSet rs = stmt.executeQuery();
            List<Task> tasks = new ArrayList<>();

            while (rs.next()){
                tasks.add(mapRow(rs));
            } return tasks;

        } catch (SQLException e){
            log.error("Failed to find tasks : ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update {@link Task} model by id
     * @param id of existing Task
     * @return modified Task model or
     */
    public boolean update(int id, TaskUpdateDto dto) {
        String sql = "UPDATE tasks SET title = ?, description = ?, done = ? WHERE id = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, dto.title());
            stmt.setString(2, dto.description());
            stmt.setBoolean(3, dto.done());
            stmt.setInt(4, id);
            int rows = stmt.executeUpdate();

            log.info("Task updated with id: " + id);
            return rows > 0;

        } catch (SQLException e){
            log.error("Failed to update task with id: " + id, e);
            throw new RuntimeException(e);
        }
    }
}
