package com.example.question_service.controller;


import com.example.question_service.model.Question;
import com.example.question_service.model.QuestionWrapper;
import com.example.question_service.model.Response;
import com.example.question_service.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    QuestionService questionService;

    @Autowired
    Environment environment;


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

            @GetMapping("/generate")
            public ResponseEntity<List<Integer>> getQuestionsForQuiz(@RequestParam String categoryName,
                                                                     @RequestParam Integer numQuestions){

                        return questionService.getQuestionsForQuiz(categoryName, numQuestions);

            }

            @PostMapping("/getQuestions")
            public ResponseEntity<List<QuestionWrapper>> getQuestionsFromId(@RequestBody List<Integer> questionIds){

                System.out.println(environment.getProperty("local.server.port"));


                    return questionService.getQuestionsFromId(questionIds);

            }

            @PostMapping("/getScore")
            public ResponseEntity<Integer> getScore(@RequestBody List<Response> responses){

                return questionService.getScore(responses);

            }


}
