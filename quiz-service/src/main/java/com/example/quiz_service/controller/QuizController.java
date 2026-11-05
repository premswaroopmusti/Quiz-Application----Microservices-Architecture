package com.example.quiz_service.controller;

import com.example.quiz_service.model.QuestionWrapper;
import com.example.quiz_service.model.QuizDto;
import com.example.quiz_service.model.Response;
import com.example.quiz_service.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quiz")
public class QuizController {

                @Autowired
                QuizService quizService;

                @PostMapping("/create")
                public ResponseEntity<?> createQuiz(@RequestBody QuizDto quizDto){

                        return quizService.createQuiz(quizDto.getCategory(), quizDto.getNumQuestions(), quizDto.getTitle());

                }

                @GetMapping("/get/{id}")
                public ResponseEntity<List<QuestionWrapper>> getQuizQuestions(@PathVariable Integer id ){

                    return quizService.getQuizQuestions(id);

                }

                @PostMapping("/submit/{id}")
                public ResponseEntity<Integer> submitQuiz(@PathVariable Integer id, @RequestBody List<Response> response){

                        return quizService.calculateResult(id, response);

                }

}