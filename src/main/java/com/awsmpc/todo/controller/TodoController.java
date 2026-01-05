package com.awsmpc.todo.controller;

import com.awsmpc.todo.model.Todo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Controller
@RequestMapping("/")
public class TodoController {
    private List<Todo> todos = new ArrayList<>();
    private AtomicLong idCounter = new AtomicLong(1);
    
    public TodoController() {
        // 初始化一些示例数据
        todos.add(new Todo(idCounter.getAndIncrement(), "Learn Spring Boot"));
        todos.add(new Todo(idCounter.getAndIncrement(), "Build a Todo App"));
        todos.add(new Todo(idCounter.getAndIncrement(), "Deploy with Jenkins"));
    }
    
    @GetMapping
    public String index(Model model) {
        model.addAttribute("todos", todos);
        model.addAttribute("newTodo", new Todo());
        model.addAttribute("total", todos.size());
        model.addAttribute("completed", todos.stream().filter(Todo::isCompleted).count());
        return "index";
    }
    
    @PostMapping("/add")
    public String addTodo(@ModelAttribute Todo newTodo) {
        if (newTodo.getTitle() != null && !newTodo.getTitle().trim().isEmpty()) {
            newTodo.setId(idCounter.getAndIncrement());
            todos.add(newTodo);
        }
        return "redirect:/";
    }
    
    @PostMapping("/complete/{id}")
    public String completeTodo(@PathVariable Long id) {
        todos.stream()
            .filter(todo -> todo.getId().equals(id))
            .findFirst()
            .ifPresent(todo -> todo.setCompleted(true));
        return "redirect:/";
    }
    
    @PostMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Long id) {
        todos.removeIf(todo -> todo.getId().equals(id));
        return "redirect:/";
    }
    
    @GetMapping("/clear")
    public String clearCompleted() {
        todos.removeIf(Todo::isCompleted);
        return "redirect:/";
    }
    
    // REST API 端点（可选）
    @GetMapping("/api/todos")
    @ResponseBody
    public List<Todo> getAllTodos() {
        return todos;
    }
    
    @GetMapping("/api/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}