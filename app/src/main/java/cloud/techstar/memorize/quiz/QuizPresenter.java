package cloud.techstar.memorize.quiz;

import android.annotation.SuppressLint;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cloud.techstar.memorize.database.Question;
import cloud.techstar.memorize.database.Words;
import cloud.techstar.memorize.database.WordsDataSource;
import cloud.techstar.memorize.database.WordsRepository;
import io.reactivex.functions.Consumer;

public class QuizPresenter implements QuizContract.Presenter {

    private final WordsRepository wordsRepository;

    private final QuizContract.View quizView;

    public Integer currentIndexQuestion = 0;

    public QuizPresenter(WordsRepository wordsRepository, QuizContract.View quizView) {
        this.wordsRepository = wordsRepository;
        this.quizView = quizView;
        quizView.setPresenter(this);
    }

    @Override
    public void nextQuestion() {
        currentIndexQuestion += 1;
        quizView.updateQuestion(getCurrentQuestion());
    }

    @Override
    public Question getCurrentQuestion() {
        return getQuizWords().get(currentIndexQuestion);
    }

    @Override
    public Boolean isRightAnswer(int indexAnswer) {
        return indexAnswer == getCurrentQuestion().getRightAnswerIndex();
    }

    @SuppressLint("CheckResult")
    @Override
    public void init() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                quizView.updateQuestion(getCurrentQuestion());
            }
        }, 400);

        quizView.onAnswer()
            .subscribe(new Consumer<Integer>() {
                @Override
                public void accept(@io.reactivex.annotations.NonNull Integer indexAnswer) throws Exception {
                    quizView.disableClicks();
                    Boolean isRight = isRightAnswer(indexAnswer);
                    if (isRight) {
                        quizView.showSuccess(indexAnswer);

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                nextQuestion();
                            }
                        }, 1000);

                    } else {
                        quizView.showWrongAnswer(indexAnswer, getCurrentQuestion().getRightAnswerIndex());
                        // 2 instead 1 seconds so the user can analyse the right answer
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                nextQuestion();
                            }
                        }, 2000);
                    }
                }
            });
    }


    public List<Question> getQuizWords(){
        final List<Question> questions = new ArrayList<Question>();
        wordsRepository.getWords(new WordsDataSource.LoadWordsCallback() {
            @Override
            public void onWordsLoaded(List<Words> words) {
                Collections.shuffle(words);
                for(int i  = 0; i < words.size(); i++) {

                    Words currentWords = words.get(i);
                    List<String> possiblesAnswers = new ArrayList<>();

                    // Answers
                    for(int j  = 0; j < 3; j++) {
                        int randomIndex = new Random().nextInt(words.size());
                        // We look for 3 wrong and different answers
                        while(possiblesAnswers.contains(words.get(randomIndex).getMeaning())
                                ||  words.get(randomIndex).getMeaning().equals(currentWords.getMeaning())) {
                            randomIndex = new Random().nextInt(words.size());
                        }

                        possiblesAnswers.add(words.get(randomIndex).getMeaning());

                    }

                    int rightIndexAnswer = new Random().nextInt(4);
                    String rightAnswer = currentWords.getMeaning();

                    possiblesAnswers.add(rightIndexAnswer, rightAnswer);

                    Question question = new Question(currentWords.getCharacter(), possiblesAnswers, rightIndexAnswer);
                    questions.add(question);
                }
            }

            @Override
            public void onDataNotAvailable() {

            }
        });
        return questions;
    }

}