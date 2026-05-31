package com.example.todoapp.model.dto;

import com.example.todoapp.model.Task;

public record TaskResponseDto(Integer id, String title, String description, boolean done) {

    public static TaskResponseDto from(Task task) {
        return new TaskResponseDto(task.id(), task.title(), task.description(), task.done());
    }
}
