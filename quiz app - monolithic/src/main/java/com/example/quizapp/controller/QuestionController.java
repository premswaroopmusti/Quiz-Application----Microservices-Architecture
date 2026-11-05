package com.example.quizapp.controller;


import com.example.quizapp.model.Question;
import com.example.quizapp.service.QuestionService;
import com.example.quizapp.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    QuestionService questionService;

    @Autowired
    QuizService quizService;

            @GetMapping("/allQuestions")
            public ResponseEntity<?> getAllQuestions(){

                try {
                    return new ResponseEntity<>(questionService.getAllQuestions(), HttpStatus.OK);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
            }

            @GetMapping("/category/{category}")
            public ResponseEntity<?> getQuestionsByCategory(@PathVariable String category){

                try {
                    return new ResponseEntity<>(questionService.getQuestionsByCategory(category), HttpStatus.OK);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
            }

            @PostMapping("/add")
            public ResponseEntity<?>  addQuestion(@RequestBody Question question){

                try{
                    return new ResponseEntity<>(questionService.addQuestion(question), HttpStatus.CREATED);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
            }


}
